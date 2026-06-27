package com.koustav.kaptur.services;

import com.koustav.kaptur.dto.EventRequest;
import com.koustav.kaptur.dto.EventResponse;
import com.koustav.kaptur.model.Event;
import com.koustav.kaptur.model.EventMembers;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.Role;
import com.koustav.kaptur.model.enums.Status;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Creates a new event and assigns the creator as the OWNER.
     *
     * @param request     The data for the new event.
     * @param currentUser The user who is creating the event.
     * @return The created event data as a DTO.
     */
    @Transactional
    public EventResponse createEvent(EventRequest request, User currentUser) {
        log.info(
            "Creating event '{}' by user: {}",
            request.getEventTitle(),
            currentUser.getKptId()
        );
        // 1. Create the Event entity from the request
        Event event = Event.builder()
            .eventTitle(request.getEventTitle())
            .description(request.getDescription())
            .eventDate(request.getEventDate())
            .eventLocation(request.getEventLocation())
            .createdBy(currentUser)
            .isActive(true)
            .isDeleted(false)
            .build();

        // 2. Save the event to get an ID
        event = eventRepository.save(event);
        log.debug("Event saved with internal ID: {}", event.getId());

        // 3. Generate a unique public ID (evntid)
        // We use a combination of a prefix and the database ID, or a random string.
        // String evntid = "EVT-" + UUID.randomUUID().toString().substring(0,
        // 8).toUpperCase();
        String evntid = String.format("EVNT%07d", event.getId());
        event.setEvntid(evntid);
        event = eventRepository.save(event);

        // 4. Automatically add the creator as a member with the 'OWNER' role
        EventMembers membership = EventMembers.builder()
            .event(event)
            .user(currentUser)
            .joinedAt(LocalDateTime.now())
            .role(Role.OWNER)
            .status(Status.ACCEPTED)
            .build();

        eventMembersRepository.save(membership);
        log.info("Event created successfully with evntid: {}", evntid);

        return mapToResponse(event);
    }

    /**
     * Retrieves an event by its ID.
     */
    public EventResponse getEventById(Long id) {
        log.debug("Fetching event by ID: {}", id);
        Event event = eventRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException("Event not found with id: " + id)
            );

        if (event.getIsDeleted()) {
            log.warn("Attempted to access deleted event: {}", id);
            throw new RuntimeException("Event has been deleted");
        }

        return mapToResponse(event);
    }

    /**
     * Retrieves all events created by the current user.
     */
    public List<EventResponse> getUserCreatedEvents(Long userId) {
        log.debug("Fetching events for user ID: {}", userId);
        List<EventResponse> events = eventRepository
            .findByCreatedById(userId)
            .stream()
            .filter(event -> !event.getIsDeleted())
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        log.debug("Found {} events for user ID: {}", events.size(), userId);
        return events;
    }

    /**
     * Updates an existing event. Only the owner should be allowed to update (this
     * check should be in service or controller).
     */
    @Transactional
    public EventResponse updateEvent(
        Long id,
        EventRequest request,
        User currentUser
    ) {
        log.info(
            "Updating event ID: {} by user: {}",
            id,
            currentUser.getKptId()
        );
        Event event = eventRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Security check: Is this user the owner?
        if (!event.getCreatedBy().getId().equals(currentUser.getId())) {
            log.warn(
                "Unauthorized update attempt on event: {} by user: {}",
                id,
                currentUser.getKptId()
            );
            throw new RuntimeException(
                "You are not authorized to update this event"
            );
        }

        event.setEventTitle(request.getEventTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setEventLocation(request.getEventLocation());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: {}", updatedEvent.getEvntid());
        return mapToResponse(updatedEvent);
    }

    /**
     * Soft deletes an event by setting isDeleted to true.
     */
    @Transactional
    public void deleteEvent(Long id, User currentUser) {
        log.info(
            "Deleting event ID: {} by user: {}",
            id,
            currentUser.getKptId()
        );
        Event event = eventRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getCreatedBy().getId().equals(currentUser.getId())) {
            log.warn(
                "Unauthorized delete attempt on event: {} by user: {}",
                id,
                currentUser.getKptId()
            );
            throw new RuntimeException(
                "You are not authorized to delete this event"
            );
        }

        event.setIsDeleted(true);
        eventRepository.save(event);
        log.info("Event deleted successfully: {}", id);
    }

    /**
     * Helper method to convert an Event entity to an EventResponse DTO.
     */
    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
            .id(event.getId())
            .evntid(event.getEvntid())
            .eventTitle(event.getEventTitle())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .eventLocation(event.getEventLocation())
            .creatorId(event.getCreatedBy().getId())
            .creatorName(event.getCreatedBy().getName())
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }
}
