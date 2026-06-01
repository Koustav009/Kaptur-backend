package com.koustav.kaptur.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.koustav.kaptur.services.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final com.koustav.kaptur.repository.UserRepository userRepository;

    /**
     * POST /api/events Creates a new event for the currently logged-in user.
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@jakarta.validation.Valid @RequestBody EventRequest request) {
        User currentUser = getCurrentUser();
        EventResponse response = eventService.createEvent(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/events/{id} Retrieves details of a specific event.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/events Retrieves all events created by the currently logged-in user.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getUserEvents() {
        User currentUser = getCurrentUser();
        List<EventResponse> response = eventService.getUserCreatedEvents(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/events/{id} Updates an existing event.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
            @jakarta.validation.Valid @RequestBody EventRequest request) {
        User currentUser = getCurrentUser();
        EventResponse response = eventService.updateEvent(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/events/{id} Deletes (soft delete) an event.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        eventService.deleteEvent(id, currentUser);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Event deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get the current authenticated user from the Security
     * Context.
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String kptId = authentication.getName();
        return userRepository.findByKptId(kptId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));
    }
}
