package com.koustav.kaptur.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.koustav.kaptur.dto.PhotoResponse;
import com.koustav.kaptur.dto.PhotoUploadRequest;
import com.koustav.kaptur.dto.PhotoUploadResponse;
import com.koustav.kaptur.model.TusdHookRequest;
import com.koustav.kaptur.model.TusdHookResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.services.FileServices;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for photo upload lifecycle management.
 * 
 * Responsibilities:
 * - Initiate photo uploads (POST /events/{eventId}/photos/init)
 * - Handle TUSd webhooks (POST /tusd/hooks)
 * - List event photos (GET /events/{eventId}/photos)
 * - Delete photos (DELETE /events/{eventId}/photos/{photoId})
 * 
 * This controller does NOT handle file bytes — the TUSd Go server handles that.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileServices fileServices;
    private final UserRepository userRepository;

    /**
     * Initiates a photo upload for a specific event.
     * 
     * POST /events/{eventId}/photos/init
     * 
     * The client sends file metadata (filename, fileType, fileSizeInKb).
     * The server creates a PENDING photo record with a UUID photoId
     * and returns the TUSd upload URL where the client should upload.
     * 
     * @param evntid  The event's public ID (e.g. "EVNT0000001")
     * @param request The file metadata
     * @return 201 CREATED with photoId and TUSd upload URL
     */
    @PostMapping("/events/{eventId}/photos/init")
    public ResponseEntity<PhotoUploadResponse> initUpload(
            @PathVariable("eventId") String evntid,
            @Valid @RequestBody PhotoUploadRequest request) {
        User currentUser = getCurrentUser();
        PhotoUploadResponse response = fileServices.initUpload(evntid, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Handles webhook calls from the TUSd Go server.
     * 
     * POST /tusd/hooks (public — no JWT required, TUSd calls this directly)
     * 
     * TUSd sends hook requests at key lifecycle points:
     * - pre-create:     We validate the photoId exists and is PENDING,
     *                   then override the upload ID to match our UUID.
     * - post-finish:    We mark the photo as COMPLETED and store the S3 key.
     * - post-terminate: We mark the photo as FAILED.
     * 
     * @param hookRequest The JSON payload from TUSd
     * @return TusdHookResponse instructing TUSd how to proceed
     */
    @PostMapping("/tusd/hooks")
    public ResponseEntity<TusdHookResponse> handleTusdHook(@RequestBody TusdHookRequest hookRequest) {
        log.info("TUSd hook received: type={}, uploadId={}",
                hookRequest.getType(),
                hookRequest.getEvent() != null && hookRequest.getEvent().getUpload() != null
                        ? hookRequest.getEvent().getUpload().getID()
                        : "N/A");

        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all non-deleted photos for an event.
     * 
     * GET /events/{eventId}/photos
     * 
     * Each photo includes a downloadUrl pointing to the TUSd server:
     * http://localhost:1080/files/{photoId}
     * The TUSd server streams the file directly from S3.
     * 
     * @param evntid The event's public ID
     * @return List of PhotoResponse DTOs
     */
    @GetMapping("/events/{eventId}/photos")
    public ResponseEntity<List<PhotoResponse>> getEventPhotos(
            @PathVariable("eventId") String evntid) {
        User currentUser = getCurrentUser();
        List<PhotoResponse> photos = fileServices.getEventPhotos(evntid, currentUser);
        return ResponseEntity.ok(photos);
    }

    /**
     * Soft-deletes a photo from an event.
     * 
     * DELETE /events/{eventId}/photos/{photoId}
     * 
     * Permission: Only the photo uploader or the event creator can delete.
     * The photo is soft-deleted (isDeleted=true), not permanently removed.
     * 
     * @param evntid  The event's public ID
     * @param photoId The photo's UUID-based public ID
     * @return 200 OK with success message
     */
    @DeleteMapping("/events/{eventId}/photos/{photoId}")
    public ResponseEntity<?> deletePhoto(
            @PathVariable("eventId") String evntid,
            @PathVariable("photoId") String photoId) {
        User currentUser = getCurrentUser();
        fileServices.deletePhoto(evntid, photoId, currentUser);

        return ResponseEntity.ok(Map.of("message", "Photo deleted successfully"));
    }

    /**
     * Helper method to get the authenticated user from Spring Security's context.
     * The JWT stores kptId as the principal name.
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        String kptId = authentication.getName();
        return userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));
    }
}