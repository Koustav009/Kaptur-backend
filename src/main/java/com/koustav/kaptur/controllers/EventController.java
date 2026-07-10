package com.koustav.kaptur.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koustav.kaptur.dto.EventRequest;
import com.koustav.kaptur.dto.EventResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.services.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    /**
     * POST /api/events
     * Creates a new event for the currently logged-in user.
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating event '{}' by user: {}", request.getEventTitle(),
                currentUser.getKptId());
        EventResponse response = eventService.createEvent(request, currentUser);
        log.info("Event created successfully with ID: {}", response.getEvntId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/events/{id}
     * Retrieves details of a specific event by its UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        log.debug("Fetching event by ID: {}", id);
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/events
     * Retrieves all events created by the currently logged-in user.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getUserEvents() {
        User currentUser = getCurrentUser();
        log.debug("Fetching events for user: {}", currentUser.getKptId());
        List<EventResponse> response = eventService.getUserJoinedEvents(currentUser.getKptId());
        log.debug("Found {} events for user: {}", response.size(), currentUser.getKptId());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/events/{id}
     * Updates an existing event.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable UUID id,
            @Valid @RequestBody EventRequest request) {
        User currentUser = getCurrentUser();
        log.info("Updating event ID: {} by user: {}", id, currentUser.getKptId());
        EventResponse response = eventService.updateEvent(id, request, currentUser);
        log.info("Event updated successfully: {}", response.getEvntId());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/events/{id}
     * Soft-deletes an event by its UUID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable UUID id) {
        User currentUser = getCurrentUser();
        log.info("Deleting event ID: {} by user: {}", id, currentUser.getKptId());
        eventService.deleteEvent(id, currentUser);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Event deleted successfully");
        log.info("Event deleted successfully: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get the current authenticated user from the Security
     * Context.
     * 
     * The JWT stores kptId as the subject claim. The AuthTokenFilter extracts
     * it and sets it as the principal name in the SecurityContext.
     * We then look up the full User entity from the database.
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String kptIdStr = authentication.getName();
        UUID kptId = UUID.fromString(kptIdStr);
        log.debug("Getting current user with kptId: {}", kptId);
        return userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));
    }
}
