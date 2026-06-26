package com.koustav.kaptur.services;

import com.koustav.kaptur.dto.PhotoResponse;
import com.koustav.kaptur.dto.PhotoUploadRequest;
import com.koustav.kaptur.dto.PhotoUploadResponse;
import com.koustav.kaptur.model.*;
import com.koustav.kaptur.model.enums.*;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventPhotosRepository;
import com.koustav.kaptur.repository.EventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileServices.
 * Tests photo upload initiation, TUSd hook handling, photo listing, and deletion.
 */
@ExtendWith(MockitoExtension.class)
class FileServicesTest {

    @Mock
    private EventPhotosRepository eventPhotosRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMembersRepository eventMembersRepository;

    @InjectMocks
    private FileServices fileServices;

    private User testUser;
    private User otherUser;
    private Event testEvent;
    private EventPhotos testPhoto;
    private EventMembers acceptedMembership;

    @BeforeEach
    void setUp() {
        // Set the @Value field for TUSd base URL
        ReflectionTestUtils.setField(fileServices, "tusdBaseUrl", "http://localhost:1080/files");

        // Test user (photo uploader and event owner)
        testUser = User.builder()
                .id(1L)
                .kptId("KPT0000001")
                .email("john@example.com")
                .name("John Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        // Another user (not a member)
        otherUser = User.builder()
                .id(2L)
                .kptId("KPT0000002")
                .email("jane@example.com")
                .name("Jane Doe")
                .provider(AuthProvider.LOCAL)
                .build();

        // Test event
        testEvent = Event.builder()
                .id(1L)
                .evntid("EVNT0000001")
                .eventTitle("Wedding")
                .createdBy(testUser)
                .isActive(true)
                .isDeleted(false)
                .eventDate(LocalDate.of(2026, 8, 15))
                .build();

        // Test photo in PENDING status
        testPhoto = EventPhotos.builder()
                .id(1L)
                .photoId("550e8400-e29b-41d4-a716-446655440000")
                .event(testEvent)
                .uploadedBy(testUser)
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .photoStatus(PhotoStatus.PENDING)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Accepted membership for testUser in testEvent
        acceptedMembership = EventMembers.builder()
                .event(testEvent)
                .user(testUser)
                .role(Role.OWNER)
                .status(Status.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // initUpload() Tests
    // ==========================================

    @Test
    @DisplayName("initUpload - valid event and member creates PENDING photo record")
    void initUpload_success() {
        // Arrange
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventMembersRepository.findByEventId(1L)).thenReturn(List.of(acceptedMembership));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

        // Act
        PhotoUploadResponse response = fileServices.initUpload("EVNT0000001", request, testUser);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getPhotoId()); // UUID generated
        assertEquals("http://localhost:1080/files/", response.getTusdUploadUrl());

        verify(eventRepository).findByEvntid("EVNT0000001");
        verify(eventMembersRepository).findByEventId(1L);
        verify(eventPhotosRepository).save(any(EventPhotos.class));
    }

    @Test
    @DisplayName("initUpload - event not found throws RuntimeException")
    void initUpload_eventNotFound_throws() {
        // Arrange
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        when(eventRepository.findByEvntid("EVNT9999999")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileServices.initUpload("EVNT9999999", request, testUser));

        assertTrue(exception.getMessage().contains("not found"));
        verify(eventPhotosRepository, never()).save(any());
    }

    @Test
    @DisplayName("initUpload - user not an accepted member throws RuntimeException")
    void initUpload_notMember_throws() {
        // Arrange
        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .build();

        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        // otherUser is not in the membership list
        when(eventMembersRepository.findByEventId(1L)).thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileServices.initUpload("EVNT0000001", request, otherUser));

        assertTrue(exception.getMessage().contains("accepted member"));
        verify(eventPhotosRepository, never()).save(any());
    }

    // ==========================================
    // handleTusdHook() — pre-create Tests
    // ==========================================

    @Test
    @DisplayName("pre-create hook - valid photoId and PENDING status accepts upload")
    void handlePreCreate_success() {
        // Arrange
        TusdHookRequest hookRequest = buildPreCreateHookRequest("550e8400-e29b-41d4-a716-446655440000");

        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertNotNull(response);
        assertNull(response.getRejectUpload());
        assertNotNull(response.getChangeFileInfo());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getChangeFileInfo().getID());

        // Status should transition to UPLOADING
        assertEquals(PhotoStatus.UPLOADING, testPhoto.getPhotoStatus());
        verify(eventPhotosRepository).save(testPhoto);
    }

    @Test
    @DisplayName("pre-create hook - missing photoId in metadata rejects upload with 400")
    void handlePreCreate_missingPhotoId_rejects() {
        // Arrange: hook request with no photoId in metadata
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("pre-create");
        TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
        TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
        upload.setMetaData(Map.of()); // Empty metadata — no photoId
        hookEvent.setUpload(upload);
        hookRequest.setEvent(hookEvent);

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertTrue(response.getRejectUpload());
        assertNotNull(response.getHttpResponse());
        assertEquals(400, response.getHttpResponse().getStatusCode());
    }

    @Test
    @DisplayName("pre-create hook - photoId not found in DB rejects upload with 404")
    void handlePreCreate_photoNotFound_rejects() {
        // Arrange
        TusdHookRequest hookRequest = buildPreCreateHookRequest("non-existent-uuid");

        when(eventPhotosRepository.findByPhotoId("non-existent-uuid")).thenReturn(Optional.empty());

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertTrue(response.getRejectUpload());
        assertNotNull(response.getHttpResponse());
        assertEquals(404, response.getHttpResponse().getStatusCode());
    }

    @Test
    @DisplayName("pre-create hook - photo not in PENDING status rejects upload with 409")
    void handlePreCreate_wrongStatus_rejects() {
        // Arrange
        testPhoto.setPhotoStatus(PhotoStatus.UPLOADING); // Already uploading
        TusdHookRequest hookRequest = buildPreCreateHookRequest("550e8400-e29b-41d4-a716-446655440000");

        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertTrue(response.getRejectUpload());
        assertNotNull(response.getHttpResponse());
        assertEquals(409, response.getHttpResponse().getStatusCode());
    }

    // ==========================================
    // handleTusdHook() — post-finish Tests
    // ==========================================

    @Test
    @DisplayName("post-finish hook - marks photo as COMPLETED and stores S3 key")
    void handlePostFinish_success() {
        // Arrange
        TusdHookRequest hookRequest = buildPostFinishHookRequest(
                "550e8400-e29b-41d4-a716-446655440000", "s3-bucket-key-abc123");

        testPhoto.setPhotoStatus(PhotoStatus.UPLOADING); // Currently uploading
        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.getRejectUpload());
        assertEquals(PhotoStatus.COMPLETED, testPhoto.getPhotoStatus());
        assertEquals("s3-bucket-key-abc123", testPhoto.getPhotoPath());
        assertNotNull(testPhoto.getUploadCompletedAt());

        verify(eventPhotosRepository).save(testPhoto);
    }

    @Test
    @DisplayName("post-finish hook - photo not found in DB still returns non-reject response")
    void handlePostFinish_photoNotFound() {
        // Arrange
        TusdHookRequest hookRequest = buildPostFinishHookRequest("unknown-uuid", "s3-key");

        when(eventPhotosRepository.findByPhotoId("unknown-uuid")).thenReturn(Optional.empty());

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert: should NOT reject (upload already completed on TUSd side)
        assertFalse(response.getRejectUpload());
        verify(eventPhotosRepository, never()).save(any());
    }

    // ==========================================
    // handleTusdHook() — post-terminate Tests
    // ==========================================

    @Test
    @DisplayName("post-terminate hook - marks photo as FAILED")
    void handlePostTerminate_success() {
        // Arrange
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("post-terminate");
        TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
        TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
        upload.setID("550e8400-e29b-41d4-a716-446655440000");
        hookEvent.setUpload(upload);
        hookRequest.setEvent(hookEvent);

        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertFalse(response.getRejectUpload());
        assertEquals(PhotoStatus.FAILED, testPhoto.getPhotoStatus());
        verify(eventPhotosRepository).save(testPhoto);
    }

    @Test
    @DisplayName("post-terminate hook - photo not found does not throw")
    void handlePostTerminate_photoNotFound() {
        // Arrange
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("post-terminate");
        TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
        TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
        upload.setID("unknown-uuid");
        hookEvent.setUpload(upload);
        hookRequest.setEvent(hookEvent);

        when(eventPhotosRepository.findByPhotoId("unknown-uuid")).thenReturn(Optional.empty());

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert: should not reject and not throw
        assertFalse(response.getRejectUpload());
        verify(eventPhotosRepository, never()).save(any());
    }

    // ==========================================
    // handleTusdHook() — unknown hook type
    // ==========================================

    @Test
    @DisplayName("unknown hook type - returns non-reject default response")
    void handleUnknownHookType() {
        // Arrange
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("pre-terminate"); // Unknown type

        // Act
        TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

        // Assert
        assertFalse(response.getRejectUpload());
    }

    // ==========================================
    // getEventPhotos() Tests
    // ==========================================

    @Test
    @DisplayName("getEventPhotos - member lists photos with download URLs")
    void getEventPhotos_success() {
        // Arrange
        EventPhotos completedPhoto = EventPhotos.builder()
                .id(1L)
                .photoId("550e8400-e29b-41d4-a716-446655440000")
                .event(testEvent)
                .uploadedBy(testUser)
                .filename("photo.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(2048L)
                .photoPath("s3-key-abc")
                .photoStatus(PhotoStatus.COMPLETED)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .uploadCompletedAt(LocalDateTime.now())
                .build();

        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventMembersRepository.findByEventId(1L)).thenReturn(List.of(acceptedMembership));
        when(eventPhotosRepository.findByEventIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(completedPhoto));

        // Act
        List<PhotoResponse> photos = fileServices.getEventPhotos("EVNT0000001", testUser);

        // Assert
        assertEquals(1, photos.size());
        PhotoResponse photo = photos.get(0);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", photo.getPhotoId());
        assertEquals("photo.jpg", photo.getFilename());
        assertEquals("image/jpeg", photo.getFileType());
        assertEquals(2048L, photo.getFileSizeInKb());
        assertEquals("COMPLETED", photo.getPhotoStatus());
        assertEquals("KPT0000001", photo.getUploadedByKptId());
        assertEquals("John Doe", photo.getUploadedByName());
        assertEquals("http://localhost:1080/files/550e8400-e29b-41d4-a716-446655440000",
                photo.getDownloadUrl());
    }

    @Test
    @DisplayName("getEventPhotos - non-member throws RuntimeException")
    void getEventPhotos_notMember_throws() {
        // Arrange
        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventMembersRepository.findByEventId(1L)).thenReturn(List.of()); // No members

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileServices.getEventPhotos("EVNT0000001", otherUser));

        assertTrue(exception.getMessage().contains("accepted member"));
    }

    // ==========================================
    // deletePhoto() Tests
    // ==========================================

    @Test
    @DisplayName("deletePhoto - uploader can delete their own photo")
    void deletePhoto_byUploader_success() {
        // Arrange
        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

        // Act: testUser uploaded the photo AND owns the event
        fileServices.deletePhoto("EVNT0000001", "550e8400-e29b-41d4-a716-446655440000", testUser);

        // Assert
        assertTrue(testPhoto.getIsDeleted());
        verify(eventPhotosRepository).save(testPhoto);
    }

    @Test
    @DisplayName("deletePhoto - event owner can delete any photo in their event")
    void deletePhoto_byEventOwner_success() {
        // Arrange: otherUser uploaded the photo, but testUser owns the event
        EventPhotos otherUserPhoto = EventPhotos.builder()
                .id(2L)
                .photoId("660e8400-e29b-41d4-a716-446655440001")
                .event(testEvent)
                .uploadedBy(otherUser) // Uploaded by otherUser
                .filename("other.jpg")
                .fileType("image/jpeg")
                .fileSizeInKb(1024L)
                .photoStatus(PhotoStatus.COMPLETED)
                .isDeleted(false)
                .build();

        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventPhotosRepository.findByPhotoId("660e8400-e29b-41d4-a716-446655440001"))
                .thenReturn(Optional.of(otherUserPhoto));
        when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(otherUserPhoto);

        // Act: testUser is the event owner, deleting otherUser's photo
        fileServices.deletePhoto("EVNT0000001", "660e8400-e29b-41d4-a716-446655440001", testUser);

        // Assert
        assertTrue(otherUserPhoto.getIsDeleted());
    }

    @Test
    @DisplayName("deletePhoto - unauthorized user (not uploader, not owner) throws RuntimeException")
    void deletePhoto_unauthorized_throws() {
        // Arrange: otherUser is neither the uploader nor the event owner
        User randomUser = User.builder()
                .id(3L)
                .kptId("KPT0000003")
                .email("random@example.com")
                .name("Random User")
                .build();

        when(eventRepository.findByEvntid("EVNT0000001")).thenReturn(Optional.of(testEvent));
        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileServices.deletePhoto("EVNT0000001",
                        "550e8400-e29b-41d4-a716-446655440000", randomUser));

        assertTrue(exception.getMessage().contains("not authorized"));
        assertFalse(testPhoto.getIsDeleted()); // Should NOT be deleted
        verify(eventPhotosRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletePhoto - photo does not belong to event throws RuntimeException")
    void deletePhoto_photoNotInEvent_throws() {
        // Arrange
        Event otherEvent = Event.builder()
                .id(2L)
                .evntid("EVNT0000002")
                .eventTitle("Other Event")
                .createdBy(testUser)
                .build();

        when(eventRepository.findByEvntid("EVNT0000002")).thenReturn(Optional.of(otherEvent));
        when(eventPhotosRepository.findByPhotoId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(Optional.of(testPhoto)); // testPhoto belongs to EVNT0000001, not EVNT0000002

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileServices.deletePhoto("EVNT0000002",
                        "550e8400-e29b-41d4-a716-446655440000", testUser));

        assertTrue(exception.getMessage().contains("does not belong"));
    }

    // ==========================================
    // Helper Methods to Build TUSd Hook Requests
    // ==========================================

    /**
     * Builds a TUSd pre-create hook request with the given photoId in metadata.
     */
    private TusdHookRequest buildPreCreateHookRequest(String photoId) {
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("pre-create");

        TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
        TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
        upload.setID(photoId);
        upload.setSize(2097152L);
        upload.setOffset(0L);
        upload.setMetaData(Map.of(
                "photoId", photoId,
                "filename", "photo.jpg",
                "filetype", "image/jpeg"));
        hookEvent.setUpload(upload);
        hookRequest.setEvent(hookEvent);

        return hookRequest;
    }

    /**
     * Builds a TUSd post-finish hook request with the given photoId and S3 key.
     */
    private TusdHookRequest buildPostFinishHookRequest(String photoId, String s3Key) {
        TusdHookRequest hookRequest = new TusdHookRequest();
        hookRequest.setType("post-finish");

        TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
        TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
        upload.setID(photoId);
        upload.setSize(2097152L);
        upload.setOffset(2097152L);
        upload.setStorage(Map.of("Key", s3Key, "Bucket", "my-bucket"));
        hookEvent.setUpload(upload);
        hookRequest.setEvent(hookEvent);

        return hookRequest;
    }
}
