package com.koustav.kaptur.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koustav.kaptur.dto.EventRequest;
import com.koustav.kaptur.dto.EventResponse;
import com.koustav.kaptur.model.Event;
import com.koustav.kaptur.model.EventMembers;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventRepository;

/**
 * Unit tests for EventService.
 * Tests event CRUD operations and authorization checks.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMembersRepository eventMembersRepository;

    @InjectMocks
    private EventService eventService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Create a reusable test user
        testUser = User.builder()
                .id(1L)
                .kptId("KPT0000001")
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        // Create a reusable test event
        testEvent = Event.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Wedding")
                .description("A beautiful wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .createdBy(testUser)
                .isActive(true)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // createEvent() Tests
    // ==========================================

    @Test
    @DisplayName("createEvent - valid request returns EventResponse with evntid")
    void createEvent_success() {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Wedding")
                .description("A beautiful wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .build();

        // Simulate double-save: first save sets ID, second save updates evntid
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) {
                e.setId(1L); // Simulate @GeneratedValue
            }
            return e;
        });

        when(eventMembersRepository.save(any(EventMembers.class))).thenReturn(null);

        // Act
        EventResponse response = eventService.createEvent(request, testUser);

        // Assert
        assertNotNull(response);
        assertEquals("EVNT0000001", response.getEvntid());
        assertEquals("Wedding", response.getEventTitle());
        assertEquals(LocalDate.of(2026, 8, 15), response.getEventDate());
        assertEquals("Kolkata", response.getEventLocation());
        assertEquals(1L, response.getCreatorId());
        assertEquals("John Doe", response.getCreatorName());

        // Verify: event saved twice, membership saved once
        verify(eventRepository, times(2)).save(any(Event.class));
        verify(eventMembersRepository).save(any(EventMembers.class));
    }

    // ==========================================
    // getEventById() Tests
    // ==========================================

    @Test
    @DisplayName("getEventById - existing event returns EventResponse")
    void getEventById_success() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // Act
        EventResponse response = eventService.getEventById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("EVNT0000001", response.getEvntid());
        assertEquals("Wedding", response.getEventTitle());

        verify(eventRepository).findById(1L);
    }

    @Test
    @DisplayName("getEventById - non-existent event throws RuntimeException")
    void getEventById_notFound_throws() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.getEventById(999L));

        assertTrue(exception.getMessage().contains("not found"));
        verify(eventRepository).findById(999L);
    }

    @Test
    @DisplayName("getEventById - soft-deleted event throws RuntimeException")
    void getEventById_deleted_throws() {
        // Arrange
        Event deletedEvent = Event.builder()
                .id(2L)
                .evntid("EVNT0000002")
                .eventTitle("Deleted Event")
                .createdBy(testUser)
                .isDeleted(true)
                .build();

        when(eventRepository.findById(2L)).thenReturn(Optional.of(deletedEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.getEventById(2L));

        assertTrue(exception.getMessage().contains("deleted"));
    }

    // ==========================================
    // getUserCreatedEvents() Tests
    // ==========================================

    @Test
    @DisplayName("getUserCreatedEvents - returns list of non-deleted events")
    void getUserCreatedEvents_success() {
        // Arrange
        Event deletedEvent = Event.builder()
                .id(2L)
                .evntid("EVNT0000002")
                .eventTitle("Deleted Party")
                .createdBy(testUser)
                .isDeleted(true)
                .build();

        when(eventRepository.findByCreatedById(1L))
                .thenReturn(List.of(testEvent, deletedEvent));

        // Act
        List<EventResponse> responses = eventService.getUserCreatedEvents(1L);

        // Assert: only the non-deleted event should be returned
        assertEquals(1, responses.size());
        assertEquals("EVNT0000001", responses.get(0).getEvntid());

        verify(eventRepository).findByCreatedById(1L);
    }

    @Test
    @DisplayName("getUserCreatedEvents - no events returns empty list")
    void getUserCreatedEvents_empty() {
        // Arrange
        when(eventRepository.findByCreatedById(1L)).thenReturn(List.of());

        // Act
        List<EventResponse> responses = eventService.getUserCreatedEvents(1L);

        // Assert
        assertTrue(responses.isEmpty());
    }

    // ==========================================
    // updateEvent() Tests
    // ==========================================

    @Test
    @DisplayName("updateEvent - owner updates event successfully")
    void updateEvent_success() {
        // Arrange
        EventRequest updateRequest = EventRequest.builder()
                .eventTitle("Updated Wedding")
                .description("Updated description")
                .eventDate(LocalDate.of(2026, 9, 20))
                .eventLocation("Mumbai")
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        EventResponse response = eventService.updateEvent(1L, updateRequest, testUser);

        // Assert: the service updates the event entity in-place
        assertNotNull(response);
        assertEquals("Updated Wedding", testEvent.getEventTitle());
        assertEquals("Updated description", testEvent.getDescription());

        verify(eventRepository).findById(1L);
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("updateEvent - non-owner throws RuntimeException")
    void updateEvent_notOwner_throws() {
        // Arrange
        User otherUser = User.builder()
                .id(2L)
                .kptId("KPT0000002")
                .email("other@example.com")
                .name("Other User")
                .build();

        EventRequest updateRequest = EventRequest.builder()
                .eventTitle("Hacked Title")
                .eventDate(LocalDate.of(2026, 1, 1))
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.updateEvent(1L, updateRequest, otherUser));

        assertTrue(exception.getMessage().contains("not authorized"));

        verify(eventRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    // ==========================================
    // deleteEvent() Tests
    // ==========================================

    @Test
    @DisplayName("deleteEvent - owner soft-deletes event successfully")
    void deleteEvent_success() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        eventService.deleteEvent(1L, testUser);

        // Assert: isDeleted should be set to true
        assertTrue(testEvent.getIsDeleted());
        verify(eventRepository).findById(1L);
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - non-owner throws RuntimeException")
    void deleteEvent_notOwner_throws() {
        // Arrange
        User otherUser = User.builder()
                .id(2L)
                .kptId("KPT0000002")
                .email("other@example.com")
                .name("Other User")
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.deleteEvent(1L, otherUser));

        assertTrue(exception.getMessage().contains("not authorized"));

        // Event should NOT have been saved
        assertFalse(testEvent.getIsDeleted());
        verify(eventRepository, never()).save(any());
    }
}
