package com.koustav.kaptur.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koustav.kaptur.dto.PhotoResponse;
import com.koustav.kaptur.dto.PhotoUploadRequest;
import com.koustav.kaptur.dto.PhotoUploadResponse;
import com.koustav.kaptur.model.TusdHookRequest;
import com.koustav.kaptur.model.TusdHookResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.repository.UserRepository;
import com.koustav.kaptur.security.CustomUserDetailsService;
import com.koustav.kaptur.security.JwtUtils;
import com.koustav.kaptur.services.FileServices;

/**
 * Controller tests for FileController using MockMvc.
 * 
 * Tests photo upload initiation, TUSd webhook handling,
 * photo listing, and photo deletion endpoints.
 */
@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FileServices fileServices;

    @MockitoBean
    private UserRepository userRepository;

    // Mock security dependencies so AuthTokenFilter can be instantiated
    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private UUID testKptId;
    private UUID testEvntId;
    private UUID testPhotoId;

    @BeforeEach
    void setUp() {
        testKptId = UUID.fromString("0192f3a4-5678-9abc-def0-123456789abc");
        testEvntId = UUID.fromString("0192f3b5-6789-abcd-ef01-234567890bcd");
        testPhotoId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        testUser = User.builder()
                .kptId(testKptId)
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findByKptId(testKptId)).thenReturn(Optional.of(testUser));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        testKptId.toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // POST /events/{eventId}/photos/init Tests
    // ==========================================

    @Test
    @DisplayName("POST /events/{eventId}/photos/init - valid request returns 201 with photoId")
    void initUpload_success() throws Exception {
        // Arrange
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        PhotoUploadResponse response = PhotoUploadResponse.builder()
                .photoId(testPhotoId)
                .tusdUploadUrl("http://localhost:1080/files/")
                .build();

        when(fileServices.initUpload(eq(testEvntId.toString()),
                any(PhotoUploadRequest.class), any(User.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/events/" + testEvntId + "/photos/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoId").value(testPhotoId.toString()))
                .andExpect(jsonPath("$.tusdUploadUrl").value("http://localhost:1080/files/"));
    }

    @Test
    @DisplayName("POST /events/{eventId}/photos/init - missing filename returns 400")
    void initUpload_missingFilename_returns400() throws Exception {
        // Arrange: filename is blank
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        // Act & Assert
        mockMvc.perform(post("/events/" + testEvntId + "/photos/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events/{eventId}/photos/init - missing fileSizeInKb returns 400")
    void initUpload_missingFileSize_returns400() throws Exception {
        // Arrange: fileSizeInKb is null
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(null)
                .build();

        // Act & Assert
        mockMvc.perform(post("/events/" + testEvntId + "/photos/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events/{eventId}/photos/init - event not found returns 400")
    void initUpload_eventNotFound_returns400() throws Exception {
        // Arrange
        UUID unknownEvntId = UUID.fromString("0192f3ff-ffff-ffff-ffff-ffffffffffff");
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        when(fileServices.initUpload(eq(unknownEvntId.toString()),
                any(PhotoUploadRequest.class), any(User.class)))
                .thenThrow(new RuntimeException("Event not found with evntid: " + unknownEvntId));

        // Act & Assert
        mockMvc.perform(post("/events/" + unknownEvntId + "/photos/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Event not found with evntid: " + unknownEvntId));
    }

    // ==========================================
    // POST /tusd/hooks Tests
    // ==========================================

    @Test
    @DisplayName("POST /tusd/hooks - pre-create hook returns 200")
    void handleTusdHook_preCreate_success() throws Exception {
        // Arrange
        TusdHookResponse hookResponse = new TusdHookResponse();
        TusdHookResponse.ChangeFileInfo changeFileInfo = new TusdHookResponse.ChangeFileInfo();
        changeFileInfo.setID(testPhotoId.toString());
        hookResponse.setChangeFileInfo(changeFileInfo);

        when(fileServices.handleTusdHook(any(TusdHookRequest.class)))
                .thenReturn(hookResponse);

        String hookJson = """
                {
                    "Type": "pre-create",
                    "Event": {
                        "Upload": {
                            "ID": "some-tusd-id",
                            "Size": 2097152,
                            "MetaData": {
                                "photoId": "%s",
                                "filename": "photo.jpg"
                            }
                        }
                    }
                }
                """.formatted(testPhotoId.toString());

        // Act & Assert
        mockMvc.perform(post("/tusd/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ChangeFileInfo.ID")
                        .value(testPhotoId.toString()));
    }

    @Test
    @DisplayName("POST /tusd/hooks - post-finish hook returns 200")
    void handleTusdHook_postFinish_success() throws Exception {
        // Arrange
        TusdHookResponse hookResponse = new TusdHookResponse();
        hookResponse.setRejectUpload(false);

        when(fileServices.handleTusdHook(any(TusdHookRequest.class)))
                .thenReturn(hookResponse);

        String hookJson = """
                {
                    "Type": "post-finish",
                    "Event": {
                        "Upload": {
                            "ID": "%s",
                            "Size": 2097152,
                            "Offset": 2097152,
                            "Storage": {
                                "Key": "s3-bucket-key",
                                "Bucket": "my-bucket"
                            }
                        }
                    }
                }
                """.formatted(testPhotoId.toString());

        // Act & Assert
        mockMvc.perform(post("/tusd/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RejectUpload").value(false));
    }

    @Test
    @DisplayName("POST /tusd/hooks - post-terminate hook returns 200")
    void handleTusdHook_postTerminate_success() throws Exception {
        // Arrange
        TusdHookResponse hookResponse = new TusdHookResponse();
        hookResponse.setRejectUpload(false);

        when(fileServices.handleTusdHook(any(TusdHookRequest.class)))
                .thenReturn(hookResponse);

        String hookJson = """
                {
                    "Type": "post-terminate",
                    "Event": {
                        "Upload": {
                            "ID": "%s"
                        }
                    }
                }
                """.formatted(testPhotoId.toString());

        // Act & Assert
        mockMvc.perform(post("/tusd/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hookJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /tusd/hooks - unknown hook type returns 200 (default behavior)")
    void handleTusdHook_unknownType() throws Exception {
        // Arrange
        TusdHookResponse hookResponse = new TusdHookResponse();
        hookResponse.setRejectUpload(false);

        when(fileServices.handleTusdHook(any(TusdHookRequest.class)))
                .thenReturn(hookResponse);

        String hookJson = """
                {
                    "Type": "pre-terminate",
                    "Event": {
                        "Upload": {
                            "ID": "some-id"
                        }
                    }
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/tusd/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hookJson))
                .andExpect(status().isOk());
    }

    // ==========================================
    // GET /events/{eventId}/photos Tests
    // ==========================================

    @Test
    @DisplayName("GET /events/{eventId}/photos - returns list of photos with download URLs")
    void getEventPhotos_success() throws Exception {
        // Arrange
        PhotoResponse photo = PhotoResponse.builder()
                .photoId(testPhotoId)
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .photoPath("s3-key")
                .photoStatus("COMPLETED")
                .uploadedByKptId(testKptId)
                .uploadedByName("John Doe")
                .createdAt(LocalDateTime.now())
                .uploadCompletedAt(LocalDateTime.now())
                .downloadUrl("http://localhost:1080/files/" + testPhotoId)
                .build();

        when(fileServices.getEventPhotos(eq(testEvntId.toString()), any(User.class)))
                .thenReturn(List.of(photo));

        // Act & Assert
        mockMvc.perform(get("/events/" + testEvntId + "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].photoId").value(testPhotoId.toString()))
                .andExpect(jsonPath("$[0].filename").value("photo.jpg"))
                .andExpect(jsonPath("$[0].photoStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].uploadedByKptId").value(testKptId.toString()))
                .andExpect(jsonPath("$[0].downloadUrl")
                        .value("http://localhost:1080/files/" + testPhotoId));
    }

    @Test
    @DisplayName("GET /events/{eventId}/photos - non-member gets 400")
    void getEventPhotos_notMember_returns400() throws Exception {
        // Arrange
        when(fileServices.getEventPhotos(eq(testEvntId.toString()), any(User.class)))
                .thenThrow(new RuntimeException(
                        "You must be a member of this event to manage photos"));

        // Act & Assert
        mockMvc.perform(get("/events/" + testEvntId + "/photos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "You must be a member of this event to manage photos"));
    }

    // ==========================================
    // DELETE /events/{eventId}/photos/{photoId} Tests
    // ==========================================

    @Test
    @DisplayName("DELETE /events/{eventId}/photos/{photoId} - authorized user deletes photo")
    void deletePhoto_success() throws Exception {
        // Arrange
        doNothing().when(fileServices).deletePhoto(
                eq(testEvntId.toString()),
                eq(testPhotoId.toString()),
                any(User.class));

        // Act & Assert
        mockMvc.perform(delete("/events/" + testEvntId + "/photos/" + testPhotoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Photo deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/photos/{photoId} - unauthorized user returns 400")
    void deletePhoto_unauthorized_returns400() throws Exception {
        // Arrange
        doThrow(new RuntimeException("You are not authorized to delete this photo"))
                .when(fileServices).deletePhoto(
                        eq(testEvntId.toString()),
                        eq(testPhotoId.toString()),
                        any(User.class));

        // Act & Assert
        mockMvc.perform(delete("/events/" + testEvntId + "/photos/" + testPhotoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "You are not authorized to delete this photo"));
    }
}
