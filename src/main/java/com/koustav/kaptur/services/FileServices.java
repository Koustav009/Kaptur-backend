package com.koustav.kaptur.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.kaptur.dto.PhotoResponse;
import com.koustav.kaptur.dto.PhotoUploadRequest;
import com.koustav.kaptur.dto.PhotoUploadResponse;
import com.koustav.kaptur.model.Event;
import com.koustav.kaptur.model.EventMembersDtl;
import com.koustav.kaptur.model.EventPhotos;
import com.koustav.kaptur.model.TusdHookRequest;
import com.koustav.kaptur.model.TusdHookResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.PhotoStatus;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventPhotosRepository;
import com.koustav.kaptur.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core business logic for photo upload lifecycle management.
 * 
 * Responsibilities: 1. Initiate uploads — create a PENDING photo record and
 * return a TUSd URL. 2. Handle TUSd hooks — validate pre-create, finalize
 * post-finish, mark failures. 3. List/delete photos — serve event photo
 * metadata, soft-delete with ownership checks.
 * 
 * Architecture note: This service does NOT handle file bytes. The TUSd Go
 * server manages all file I/O and S3 storage. This service only manages
 * metadata and the upload lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServices {

    private final EventPhotosRepository eventPhotosRepository;
    private final EventRepository eventRepository;
    private final EventMembersRepository eventMembersRepository;

    @Value("${tusd.base-url}")
    private String tusdBaseUrl;

    /**
     * Initiates a photo upload for an event.
     * 
     * Flow: 1. Parse the event's public evntId from string to UUID. 2. Look up the
     * event. 3. Verify the current user is a member of that event. 4. Generate a
     * UUID photoId (also used as the TUSd upload ID). 5. Save a PENDING EventPhotos
     * record. 6. Return the photoId and TUSd base URL for the client to start
     * uploading.
     * 
     * @param evntid      The event's public UUID (as string from the URL path)
     * @param request     Metadata about the file to be uploaded
     * @param currentUser The authenticated user initiating the upload
     * @return PhotoUploadResponse containing the photoId and TUSd upload URL
     */
    @Transactional
    public PhotoUploadResponse initUpload(String evntid, PhotoUploadRequest request,
            User currentUser) {
        // 1. Parse the event's public evntId from string to UUID
        UUID eventUuid = UUID.fromString(evntid);

        // 2. Look up the event
        Event event = eventRepository.findByEvntId(eventUuid)
                .orElseThrow(() -> new RuntimeException("Event not found with evntid: " + evntid));

        // 3. Verify the current user is a member of this event
        // Existence in EventMembersDtl = accepted member
        validateEventMembership(event.getEvntId(), currentUser.getKptId());

        // 4. Generate a UUID for the photoId — this is also the TUSd upload ID
        UUID photoId = UUID.randomUUID();

        // 5. Create and save a PENDING photo record (single DB write)
        EventPhotos photo = EventPhotos.builder().photoId(photoId).event(event)
                .uploadedBy(currentUser).filename(request.getFilename())
                .fileType(request.getFileType()).fileSizeInKb(request.getFileSizeInKb())
                .photoStatus(PhotoStatus.PENDING).isDeleted(false).build();

        eventPhotosRepository.save(photo);

        log.info("Photo upload initiated: photoId={}, event={}, user={}", photoId, evntid,
                currentUser.getKptId());

        // 6. Return the photoId and TUSd base URL
        return PhotoUploadResponse.builder().photoId(photoId).tusdUploadUrl(tusdBaseUrl + "/")
                .build();
    }

    /**
     * Processes incoming hooks from the TUSd Go server. TUSd calls this method
     * during key upload lifecycle events.
     * 
     * Hook types handled: - pre-create: Validates the photoId exists in our DB
     * (PENDING). Overrides the upload ID to match our UUID photoId. - post-finish:
     * Marks the photo as COMPLETED and stores the S3 key. - post-terminate: Marks
     * the photo as FAILED.
     * 
     * @param hookRequest The parsed JSON payload from TUSd
     * @return TusdHookResponse indicating whether to accept/reject/modify the
     *         upload
     */
    @Transactional
    public TusdHookResponse handleTusdHook(TusdHookRequest hookRequest) {
        String hookType = hookRequest.getType();
        log.info("Received TUSd hook: type={}", hookType);

        switch (hookType) {
        case "pre-create":
            return handlePreCreate(hookRequest);
        case "post-finish":
            return handlePostFinish(hookRequest);
        case "post-terminate":
            return handlePostTerminate(hookRequest);
        default:
            log.warn("Unhandled TUSd hook type: {}", hookType);
            TusdHookResponse defaultResponse = new TusdHookResponse();
            defaultResponse.setRejectUpload(false);
            return defaultResponse;
        }
    }

    /**
     * Handles the pre-create hook — TUSd calls this BEFORE creating the upload.
     * 
     * We validate that: 1. The photoId exists in our metadata (passed as
     * Upload-Metadata by the client). 2. The photo record is in PENDING status.
     * 
     * If valid, we tell TUSd to use our photoId as the upload ID (via
     * ChangeFileInfo). This ensures the upload URL path is predictable:
     * /files/{photoId}
     * 
     * We also transition the status from PENDING → UPLOADING.
     */
    private TusdHookResponse handlePreCreate(TusdHookRequest hookRequest) {
        TusdHookResponse response = new TusdHookResponse();

        // Extract photoId from the upload metadata sent by the client
        Map<String, String> metadata = hookRequest.getEvent().getUpload().getMetaData();
        String photoId = metadata != null ? metadata.get("photoId") : null;

        if (photoId == null) {
            log.warn("pre-create hook rejected: missing photoId in metadata");
            response.setRejectUpload(true);
            TusdHookResponse.HttpResponse httpResp = new TusdHookResponse.HttpResponse();
            httpResp.setStatusCode(400);
            httpResp.setBody("Missing photoId metadata");
            response.setHttpResponse(httpResp);
            return response;
        }

        // Convert string photoId from TUSd metadata to UUID for DB lookup
        UUID photoUuid = UUID.fromString(photoId);

        // Look up the photo record by photoId
        EventPhotos photo = eventPhotosRepository.findByPhotoId(photoUuid).orElse(null);

        if (photo == null) {
            log.warn("pre-create hook rejected: photoId not found in DB: {}", photoId);
            response.setRejectUpload(true);
            TusdHookResponse.HttpResponse httpResp = new TusdHookResponse.HttpResponse();
            httpResp.setStatusCode(404);
            httpResp.setBody("Photo not found with ID: " + photoId);
            response.setHttpResponse(httpResp);
            return response;
        }

        if (photo.getPhotoStatus() != PhotoStatus.PENDING) {
            log.warn("pre-create hook rejected: photoId {} has status {} (expected PENDING)",
                    photoId, photo.getPhotoStatus());
            response.setRejectUpload(true);
            TusdHookResponse.HttpResponse httpResp = new TusdHookResponse.HttpResponse();
            httpResp.setStatusCode(409);
            httpResp.setBody("Photo upload already in progress or completed");
            response.setHttpResponse(httpResp);
            return response;
        }

        // Validation passed — override the TUSd upload ID to match our photoId
        TusdHookResponse.ChangeFileInfo changeFileInfo = new TusdHookResponse.ChangeFileInfo();
        changeFileInfo.setID(photoId); // TUSd will use this UUID as the upload ID
        response.setChangeFileInfo(changeFileInfo);

        // Transition status from PENDING → UPLOADING
        photo.setPhotoStatus(PhotoStatus.UPLOADING);
        eventPhotosRepository.save(photo);

        log.info("pre-create hook accepted: photoId={}, status transition PENDING→UPLOADING",
                photoId);
        return response;
    }

    /**
     * Handles the post-finish hook — TUSd calls this AFTER the upload completes.
     * 
     * We extract the S3 storage key from the hook and mark the photo as COMPLETED.
     * The upload ID in the hook IS our photoId (because we overrode it in
     * pre-create).
     */
    private TusdHookResponse handlePostFinish(TusdHookRequest hookRequest) {
        String photoId = hookRequest.getEvent().getUpload().getID();
        log.info("post-finish hook: photoId={}", photoId);

        UUID photoUuid = UUID.fromString(photoId);
        EventPhotos photo = eventPhotosRepository.findByPhotoId(photoUuid).orElse(null);
        if (photo == null) {
            log.error("post-finish hook: photoId not found in DB: {}", photoId);
            TusdHookResponse response = new TusdHookResponse();
            response.setRejectUpload(false); // Upload already completed on TUSd side, don't reject
            return response;
        }

        // Extract the S3 object key from the storage info
        Map<String, String> storage = hookRequest.getEvent().getUpload().getStorage();
        if (storage != null && storage.containsKey("Key")) {
            photo.setPhotoPath(storage.get("Key"));
        }

        // Mark as COMPLETED and record the completion timestamp
        photo.setPhotoStatus(PhotoStatus.COMPLETED);
        photo.setUploadCompletedAt(LocalDateTime.now());
        eventPhotosRepository.save(photo);

        log.info("post-finish hook: photoId={} marked COMPLETED, S3 key={}", photoId,
                photo.getPhotoPath());

        TusdHookResponse response = new TusdHookResponse();
        response.setRejectUpload(false);
        return response;
    }

    /**
     * Handles the post-terminate hook — TUSd calls this when an upload is
     * terminated/aborted.
     * 
     * We mark the photo as FAILED so the client can retry.
     */
    private TusdHookResponse handlePostTerminate(TusdHookRequest hookRequest) {
        String photoId = hookRequest.getEvent().getUpload().getID();
        log.info("post-terminate hook: photoId={}", photoId);

        UUID photoUuid = UUID.fromString(photoId);
        EventPhotos photo = eventPhotosRepository.findByPhotoId(photoUuid).orElse(null);
        if (photo != null) {
            photo.setPhotoStatus(PhotoStatus.FAILED);
            eventPhotosRepository.save(photo);
            log.info("post-terminate hook: photoId={} marked FAILED", photoId);
        }

        TusdHookResponse response = new TusdHookResponse();
        response.setRejectUpload(false);
        return response;
    }

    /**
     * Retrieves all non-deleted photos for an event.
     * 
     * The download URL for each photo points to the TUSd server, which serves the
     * file directly from S3.
     * 
     * @param evntid      The event's public UUID (as string from URL path)
     * @param currentUser The authenticated user (must be an event member)
     * @return List of PhotoResponse DTOs with metadata and download URLs
     */
    @Transactional(readOnly = true)
    public List<PhotoResponse> getEventPhotos(String evntid, User currentUser) {
        // 1. Parse event UUID and look up the event
        UUID eventUuid = UUID.fromString(evntid);
        Event event = eventRepository.findByEvntId(eventUuid)
                .orElseThrow(() -> new RuntimeException("Event not found with evntid: " + evntid));

        // 2. Verify membership (existence in EventMembersDtl = accepted member)
        validateEventMembership(event.getEvntId(), currentUser.getKptId());

        // 3. Fetch non-deleted photos and map to response DTOs
        List<EventPhotos> photos = eventPhotosRepository
                .findByEventEvntIdAndIsDeletedFalse(event.getEvntId());

        return photos.stream().map(photo -> PhotoResponse.builder().photoId(photo.getPhotoId())
                .filename(photo.getFilename()).fileType(photo.getFileType())
                .fileSizeInKb(photo.getFileSizeInKb()).photoPath(photo.getPhotoPath())
                .photoStatus(photo.getPhotoStatus().name())
                .uploadedByKptId(photo.getUploadedBy().getKptId())
                .uploadedByName(photo.getUploadedBy().getName()).createdAt(photo.getCreatedAt())
                .uploadCompletedAt(photo.getUploadCompletedAt())
                // TUSd serves the file directly: GET /files/{photoId}
                .downloadUrl(tusdBaseUrl + "/" + photo.getPhotoId()).build())
                .collect(Collectors.toList());
    }

    /**
     * Soft-deletes a photo from an event.
     * 
     * Permission rules: - The photo uploader can delete their own photo. - The
     * event creator can delete any photo in their event.
     * 
     * @param evntid      The event's public UUID (as string from URL path)
     * @param photoId     The photo's UUID-based public ID (as string from URL path)
     * @param currentUser The authenticated user
     */
    @Transactional
    public void deletePhoto(String evntid, String photoId, User currentUser) {
        // 1. Parse UUIDs from URL path strings
        UUID eventUuid = UUID.fromString(evntid);
        UUID photoUuid = UUID.fromString(photoId);

        // 2. Look up the event
        Event event = eventRepository.findByEvntId(eventUuid)
                .orElseThrow(() -> new RuntimeException("Event not found with evntid: " + evntid));

        // 3. Look up the photo
        EventPhotos photo = eventPhotosRepository.findByPhotoId(photoUuid)
                .orElseThrow(() -> new RuntimeException("Photo not found with ID: " + photoId));

        // 4. Verify the photo belongs to the specified event
        if (!photo.getEvent().getEvntId().equals(event.getEvntId())) {
            throw new RuntimeException("Photo does not belong to this event");
        }

        // 5. Permission check: must be the uploader OR the event creator
        boolean isUploader = photo.getUploadedBy().getKptId().equals(currentUser.getKptId());
        boolean isEventOwner = event.getCreatedBy().getKptId().equals(currentUser.getKptId());

        if (!isUploader && !isEventOwner) {
            throw new RuntimeException("You are not authorized to delete this photo");
        }

        // 6. Soft delete
        photo.setIsDeleted(true);
        eventPhotosRepository.save(photo);

        log.info("Photo soft-deleted: photoId={}, event={}, deletedBy={}", photoId, evntid,
                currentUser.getKptId());
    }

    /**
     * Validates that a user is a member of an event.
     * 
     * Existence of a record in EventMembersDtl for this (event, user) pair means
     * the user is an accepted member. There is no separate invitation status field.
     * 
     * @param eventUuid The event's UUID primary key (evntId)
     * @param userUuid  The user's UUID primary key (kptId)
     */
    private void validateEventMembership(UUID eventUuid, UUID userUuid) {
        List<EventMembersDtl> memberships = eventMembersRepository.findByEventEvntId(eventUuid);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getKptId().equals(userUuid));

        if (!isMember) {
            throw new RuntimeException("You must be a member of this event to manage photos");
        }
    }
}
