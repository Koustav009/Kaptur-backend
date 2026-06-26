package com.koustav.kaptur.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.koustav.kaptur.dto.EventRequest;
import com.koustav.kaptur.dto.EventResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.security.CustomUserDetailsService;
import com.koustav.kaptur.security.JwtUtils;
import com.koustav.kaptur.services.EventService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for EventController using MockMvc.
 * 
 * Security filters are disabled; SecurityContext is manually set
 * to simulate an authenticated user (kptId = "KPT0000001").
 */
@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private UserRepository userRepository;

    // Mock security dependencies so AuthTokenFilter can be instantiated
    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user that getCurrentUser() will resolve
        testUser = User.builder()
                .id(1L)
                .kptId("KPT0000001")
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        // Mock the repository lookup that getCurrentUser() calls
        when(userRepository.findByKptId("KPT0000001")).thenReturn(Optional.of(testUser));

        // Set up the SecurityContext so authentication.getName() returns "KPT0000001"
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("KPT0000001", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // POST /events Tests
    // ==========================================

    @Test
    @DisplayName("POST /events - valid request returns 201 with EventResponse")
    void createEvent_success() throws Exception {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Wedding")
                .description("A beautiful wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .build();

        EventResponse response = EventResponse.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Wedding")
                .description("A beautiful wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .creatorId(1L)
                .creatorName("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(eventService.createEvent(any(EventRequest.class), any(User.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evntid").value("EVNT0000001"))
                .andExpect(jsonPath("$.eventTitle").value("Wedding"))
                .andExpect(jsonPath("$.eventLocation").value("Kolkata"))
                .andExpect(jsonPath("$.creatorName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /events - missing eventTitle returns 400 (validation failure)")
    void createEvent_missingTitle_returns400() throws Exception {
        // Arrange: eventTitle is blank
        EventRequest request = EventRequest.builder()
                .eventTitle("")
                .eventDate(LocalDate.of(2026, 8, 15))
                .build();

        // Act & Assert
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - missing eventDate returns 400 (validation failure)")
    void createEvent_missingDate_returns400() throws Exception {
        // Arrange: eventDate is null
        EventRequest request = EventRequest.builder()
                .eventTitle("Wedding")
                .eventDate(null) // Required field
                .build();

        // Act & Assert
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // GET /events Tests
    // ==========================================

    @Test
    @DisplayName("GET /events - returns list of user's events")
    void getUserEvents_success() throws Exception {
        // Arrange
        EventResponse response = EventResponse.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .creatorId(1L)
                .creatorName("John Doe")
                .build();

        when(eventService.getUserCreatedEvents(1L)).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evntid").value("EVNT0000001"))
                .andExpect(jsonPath("$[0].eventTitle").value("Wedding"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /events - no events returns empty array")
    void getUserEvents_empty() throws Exception {
        // Arrange
        when(eventService.getUserCreatedEvents(1L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==========================================
    // GET /events/{id} Tests
    // ==========================================

    @Test
    @DisplayName("GET /events/{id} - existing event returns 200 with EventResponse")
    void getEventById_success() throws Exception {
        // Arrange
        EventResponse response = EventResponse.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Wedding")
                .eventDate(LocalDate.of(2026, 8, 15))
                .eventLocation("Kolkata")
                .creatorId(1L)
                .creatorName("John Doe")
                .build();

        when(eventService.getEventById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evntid").value("EVNT0000001"))
                .andExpect(jsonPath("$.eventTitle").value("Wedding"));
    }

    @Test
    @DisplayName("GET /events/{id} - non-existent event returns 400")
    void getEventById_notFound_returns400() throws Exception {
        // Arrange
        when(eventService.getEventById(999L))
                .thenThrow(new RuntimeException("Event not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/events/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Event not found with id: 999"));
    }

    // ==========================================
    // PUT /events/{id} Tests
    // ==========================================

    @Test
    @DisplayName("PUT /events/{id} - owner updates event returns 200")
    void updateEvent_success() throws Exception {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Updated Wedding")
                .description("Updated description")
                .eventDate(LocalDate.of(2026, 9, 20))
                .eventLocation("Mumbai")
                .build();

        EventResponse response = EventResponse.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Updated Wedding")
                .description("Updated description")
                .eventDate(LocalDate.of(2026, 9, 20))
                .eventLocation("Mumbai")
                .creatorId(1L)
                .creatorName("John Doe")
                .build();

        when(eventService.updateEvent(eq(1L), any(EventRequest.class), any(User.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTitle").value("Updated Wedding"))
                .andExpect(jsonPath("$.eventLocation").value("Mumbai"));
    }

    @Test
    @DisplayName("PUT /events/{id} - non-owner returns 400")
    void updateEvent_unauthorized_returns400() throws Exception {
        // Arrange
        EventRequest request = EventRequest.builder()
                .eventTitle("Hacked Title")
                .eventDate(LocalDate.of(2026, 1, 1))
                .build();

        when(eventService.updateEvent(eq(1L), any(EventRequest.class), any(User.class)))
                .thenThrow(new RuntimeException("You are not authorized to update this event"));

        // Act & Assert
        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You are not authorized to update this event"));
    }

    // ==========================================
    // DELETE /events/{id} Tests
    // ==========================================

    @Test
    @DisplayName("DELETE /events/{id} - owner deletes event returns 200")
    void deleteEvent_success() throws Exception {
        // Arrange
        doNothing().when(eventService).deleteEvent(1L, testUser);

        // Act & Assert
        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /events/{id} - non-owner returns 400")
    void deleteEvent_unauthorized_returns400() throws Exception {
        // Arrange
        doThrow(new RuntimeException("You are not authorized to delete this event"))
                .when(eventService).deleteEvent(1L, testUser);

        // Act & Assert
        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You are not authorized to delete this event"));
    }
}
