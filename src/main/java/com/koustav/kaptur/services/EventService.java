package com.koustav.kaptur.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.uuid.UuidCreator;
import com.koustav.kaptur.dto.EventRequest;
import com.koustav.kaptur.dto.EventResponse;
import com.koustav.kaptur.model.Event;
import com.koustav.kaptur.model.EventMembersDtl;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.UserRoleMst;
import com.koustav.kaptur.model.enums.Role;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventRepository;
import com.koustav.kaptur.repository.UserRoleMstRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class for managing Events. This is where the business logic for
 * creating, updating, and deleting events resides. We use @Transactional to
 * ensure that multiple database operations either all succeed or all fail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMembersRepository eventMembersRepository;
    private final UserRoleMstRepository userRoleMstRepository;

    /**
     * Creates a new event and automatically adds the creator as a member
     * with the ADMIN role.
     * 
     * A record in EventMembersDtl means the user is an accepted member —
     * there is no separate invitation/pending status.
     *
     * @param request     The data for the new event.
     * @param currentUser The user who is creating the event.
     * @return The created event data as a DTO.
     */
    @Transactional
    public EventResponse createEvent(EventRequest request, User currentUser) {
        log.info("Creating event '{}' by user: {}", request.getEventTitle(),
                currentUser.getKptId());

        // 1. Look up the ADMIN role from the role master table
        UserRoleMst adminRole = userRoleMstRepository.findByRoleCode(Role.ADMIN)
                .orElseThrow(() -> new RuntimeException(
                        "ADMIN role not found in USER_ROLE_MST. Please seed the role data."));

        // 2. Create the Event entity from the request
        // Generate a time-ordered UUIDv7 for index-friendly primary key
        UUID evntId = UuidCreator.getTimeOrderedEpoch();

        Event event = Event.builder()
                .evntId(evntId)
                .eventTitle(request.getEventTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .createdBy(currentUser)
                .isActive(true)
                .isDeleted(false)
                .build();

        // 3. Save the event once (no double-save needed since we set evntId upfront)
        event = eventRepository.save(event);
        log.debug("Event saved with evntId: {}", evntId);

        // 4. Automatically add the creator as a member with the ADMIN role
        // Existence in EventMembersDtl = accepted member (no separate status field)
        EventMembersDtl membership = EventMembersDtl.builder()
                .event(event)
                .user(currentUser)
                .joinedAt(LocalDateTime.now())
                .roleMst(adminRole)
                .build();

        eventMembersRepository.save(membership);
        log.info("Event created successfully with evntId: {}", evntId);

        return mapToResponse(event);
    }

    /**
     * Retrieves an event by its evntId (UUID).
     */
    public EventResponse getEventById(UUID id) {
        log.debug("Fetching event by ID: {}", id);
        Event event = eventRepository.findByEvntId(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        if (event.getIsDeleted()) {
            log.warn("Attempted to access deleted event: {}", id);
            throw new RuntimeException("Event has been deleted");
        }

        return mapToResponse(event);
    }

    /**
     * Retrieves all non-deleted events created by a specific user.
     */
    public List<EventResponse> getUserJoinedEvents(UUID userId) {
        log.debug("Fetching events for user ID: {}", userId);
        List<EventResponse> events = eventRepository.findByCreatedById(userId).stream()
                .filter(event -> !event.getIsDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        log.debug("Found {} events for user ID: {}", events.size(), userId);
        return events;
    }

    /**
     * Updates an existing event. Only the creator (owner) is allowed to update.
     */
    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request, User currentUser) {
        log.info("Updating event ID: {} by user: {}", id, currentUser.getKptId());
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Security check: Is this user the creator?
        if (!event.getCreatedBy().getKptId().equals(currentUser.getKptId())) {
            log.warn("Unauthorized update attempt on event: {} by user: {}", id,
                    currentUser.getKptId());
            throw new RuntimeException("You are not authorized to update this event");
        }

        event.setEventTitle(request.getEventTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setEventLocation(request.getEventLocation());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: {}", updatedEvent.getEvntId());
        return mapToResponse(updatedEvent);
    }

    /**
     * Soft deletes an event by setting isDeleted to true.
     * Only the creator may delete their event.
     */
    @Transactional
    public void deleteEvent(UUID id, User currentUser) {
        log.info("Deleting event ID: {} by user: {}", id, currentUser.getKptId());
        Event event = eventRepository.findByEvntId(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Security check: compare UUID primary keys directly
        if (!event.getCreatedBy().getKptId().equals(currentUser.getKptId())) {
            log.warn("Unauthorized delete attempt on event: {} by user: {}", id,
                    currentUser.getKptId());
            throw new RuntimeException("You are not authorized to delete this event");
        }

        event.setIsDeleted(true);
        eventRepository.save(event);
        log.info("Event deleted successfully: {}", id);
    }

    /**
     * Helper method to convert an Event entity to an EventResponse DTO.
     * Uses UUID-based identifiers throughout.
     */
    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .evntId(event.getEvntId())
                .eventTitle(event.getEventTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventLocation(event.getEventLocation())
                .creatorId(event.getCreatedBy().getKptId())
                .creatorName(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
