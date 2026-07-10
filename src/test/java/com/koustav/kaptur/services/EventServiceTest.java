package com.koustav.kaptur.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.koustav.kaptur.model.EventMembersDtl;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.UserRoleMst;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.model.enums.Role;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventRepository;
import com.koustav.kaptur.repository.UserRoleMstRepository;

/**
 * Unit tests for EventService. Tests event CRUD operations and authorization
 * checks with UUID-based IDs.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMembersRepository eventMembersRepository;

    @Mock
    private UserRoleMstRepository userRoleMstRepository;

    @InjectMocks
    private EventService eventService;

    private User testUser;
    private Event testEvent;
    private UUID testKptId;
    private UUID testEvntId;
    private UUID adminRoleId;
    private UserRoleMst adminRole;

    @BeforeEach
    void setUp() {
        testKptId = UUID.fromString("0192f3a4-5678-9abc-def0-123456789abc");
        testEvntId = UUID.fromString("0192f3b5-6789-abcd-ef01-234567890bcd");
        adminRoleId = UUID.randomUUID();

        adminRole = new UserRoleMst();
        adminRole.setRoleId(adminRoleId);
        adminRole.setRoleCode(Role.ADMIN);
        adminRole.setRoleName("Administrator");

        // Create a reusable test user (UUID PK only, no Long id)
        testUser = User.builder()
                .kptId(testKptId)
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        // Create a reusable test event (UUID PK only)
        testEvent = Event.builder()
                .evntId(testEvntId)
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
    @DisplayName("createEvent - valid request returns EventResponse with evntId (UUID)")
    void createEvent_success() {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Wedding")
                .description("A beautiful wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .build();

        when(userRoleMstRepository.findByRoleCode(Role.ADMIN))
                .thenReturn(Optional.of(adminRole));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMembersRepository.save(any(EventMembersDtl.class))).thenReturn(null);

        // Act
        EventResponse response = eventService.createEvent(request, testUser);

        // Assert
        assertNotNull(response);
        assertEquals(testEvntId, response.getEvntId());
        assertEquals("Wedding", response.getEventTitle());
        assertEquals(LocalDate.of(2026, 8, 15), response.getEventDate());
        assertEquals("Kolkata", response.getEventLocation());
        assertEquals(testKptId, response.getCreatorId());
        assertEquals("John Doe", response.getCreatorName());

        // Verify: event saved once, membership saved once, role lookup performed
        verify(userRoleMstRepository).findByRoleCode(Role.ADMIN);
        verify(eventRepository).save(any(Event.class));
        verify(eventMembersRepository).save(any(EventMembersDtl.class));
    }

    @Test
    @DisplayName("createEvent - ADMIN role not found in master table throws RuntimeException")
    void createEvent_roleNotFound_throws() {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .build();

        when(userRoleMstRepository.findByRoleCode(Role.ADMIN))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.createEvent(request, testUser));

        assertTrue(exception.getMessage().contains("ADMIN role not found"));

        // Verify: role lookup happened, but event was never saved
        verify(userRoleMstRepository).findByRoleCode(Role.ADMIN);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ==========================================
    // getEventById() Tests
    // ==========================================

    @Test
    @DisplayName("getEventById - existing event returns EventResponse")
    void getEventById_success() {
        // Arrange
        when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));

        // Act
        EventResponse response = eventService.getEventById(testEvntId);

        // Assert
        assertNotNull(response);
        assertEquals(testEvntId, response.getEvntId());
        assertEquals("Wedding", response.getEventTitle());

        verify(eventRepository).findByEvntId(testEvntId);
    }

    @Test
    @DisplayName("getEventById - non-existent event throws RuntimeException")
    void getEventById_notFound_throws() {
        // Arrange
        UUID unknownId = UUID.fromString("0192f3ff-ffff-ffff-ffff-ffffffffffff");
        when(eventRepository.findByEvntId(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.getEventById(unknownId));

        assertTrue(exception.getMessage().contains("not found"));
        verify(eventRepository).findByEvntId(unknownId);
    }

    @Test
    @DisplayName("getEventById - soft-deleted event throws RuntimeException")
    void getEventById_deleted_throws() {
        // Arrange
        UUID deletedEvntId = UUID.fromString("0192f3ff-0000-0000-0000-000000000002");
        Event deletedEvent = Event.builder()
                .evntId(deletedEvntId)
                .eventTitle("Deleted Event")
                .createdBy(testUser)
                .isDeleted(true)
                .build();

        when(eventRepository.findByEvntId(deletedEvntId))
                .thenReturn(Optional.of(deletedEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.getEventById(deletedEvntId));

        assertTrue(exception.getMessage().contains("deleted"));
    }

    // ==========================================
    // getUserJoinedEvents() Tests
    // ==========================================

    @Test
    @DisplayName("getUserJoinedEvents - returns list of non-deleted events")
    void getUserJoinedEvents_success() {
        // Arrange
        UUID deletedEvntId = UUID.fromString("0192f3ff-0000-0000-0000-000000000002");
        Event deletedEvent = Event.builder()
                .evntId(deletedEvntId)
                .eventTitle("Deleted Party")
                .createdBy(testUser)
                .isDeleted(true)
                .build();

        when(eventRepository.findByCreatedById(testKptId))
                .thenReturn(List.of(testEvent, deletedEvent));

        // Act
        List<EventResponse> responses = eventService.getUserJoinedEvents(testKptId);

        // Assert: only the non-deleted event should be returned
        assertEquals(1, responses.size());
        assertEquals(testEvntId, responses.get(0).getEvntId());

        verify(eventRepository).findByCreatedById(testKptId);
    }

    @Test
    @DisplayName("getUserJoinedEvents - no events returns empty list")
    void getUserJoinedEvents_empty() {
        // Arrange
        when(eventRepository.findByCreatedById(testKptId)).thenReturn(List.of());

        // Act
        List<EventResponse> responses = eventService.getUserJoinedEvents(testKptId);

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

        when(eventRepository.findById(testEvntId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        EventResponse response = eventService.updateEvent(testEvntId, updateRequest, testUser);

        // Assert: the service updates the event entity in-place
        assertNotNull(response);
        assertEquals("Updated Wedding", testEvent.getEventTitle());
        assertEquals("Updated description", testEvent.getDescription());

        verify(eventRepository).findById(testEvntId);
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("updateEvent - non-owner throws RuntimeException")
    void updateEvent_notOwner_throws() {
        // Arrange
        UUID otherKptId = UUID.fromString("0192f3ff-0000-0000-0000-000000000002");
        User otherUser = User.builder()
                .kptId(otherKptId)
                .email("other@example.com")
                .name("Other User")
                .build();

        EventRequest updateRequest = EventRequest.builder()
                .eventTitle("Hacked Title")
                .eventDate(LocalDate.of(2026, 1, 1))
                .build();

        when(eventRepository.findById(testEvntId)).thenReturn(Optional.of(testEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.updateEvent(testEvntId, updateRequest, otherUser));

        assertTrue(exception.getMessage().contains("not authorized"));

        verify(eventRepository).findById(testEvntId);
        verify(eventRepository, never()).save(any());
    }

    // ==========================================
    // deleteEvent() Tests
    // ==========================================

    @Test
    @DisplayName("deleteEvent - owner soft-deletes event successfully")
    void deleteEvent_success() {
        // Arrange
        when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        eventService.deleteEvent(testEvntId, testUser);

        // Assert: isDeleted should be set to true
        assertTrue(testEvent.getIsDeleted());
        verify(eventRepository).findByEvntId(testEvntId);
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - non-owner throws RuntimeException")
    void deleteEvent_notOwner_throws() {
        // Arrange
        UUID otherKptId = UUID.fromString("0192f3ff-0000-0000-0000-000000000002");
        User otherUser = User.builder()
                .kptId(otherKptId)
                .email("other@example.com")
                .name("Other User")
                .build();

        when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.deleteEvent(testEvntId, otherUser));

        assertTrue(exception.getMessage().contains("not authorized"));

        // Event should NOT have been saved
        assertFalse(testEvent.getIsDeleted());
        verify(eventRepository, never()).save(any());
    }
}
