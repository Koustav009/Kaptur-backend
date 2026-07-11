package com.koustav.kaptur.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.koustav.kaptur.dto.PhotoResponse;
import com.koustav.kaptur.dto.PhotoUploadRequest;
import com.koustav.kaptur.dto.PhotoUploadResponse;
import com.koustav.kaptur.model.Event;
import com.koustav.kaptur.model.EventMembersDtl;
import com.koustav.kaptur.model.EventPhotos;
import com.koustav.kaptur.model.TusdHookRequest;
import com.koustav.kaptur.model.TusdHookResponse;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.AuthProvider;
import com.koustav.kaptur.model.enums.PhotoStatus;
import com.koustav.kaptur.repository.EventMembersRepository;
import com.koustav.kaptur.repository.EventPhotosRepository;
import com.koustav.kaptur.repository.EventRepository;

/**
 * Unit tests for FileServices. Tests photo upload initiation, TUSd hook
 * handling, photo listing, and deletion — all using UUID-based IDs.
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
        private EventMembersDtl acceptedMembership;

        private UUID testKptId;
        private UUID otherKptId;
        private UUID testEvntId;
        private UUID testPhotoId;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(fileServices, "tusdBaseUrl",
                                "http://localhost:1080/files");

                testKptId = UUID.fromString("0192f3a4-5678-9abc-def0-123456789abc");
                otherKptId = UUID.fromString("0192f3ff-0000-0000-0000-000000000002");
                testEvntId = UUID.fromString("0192f3b5-6789-abcd-ef01-234567890bcd");
                testPhotoId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

                // Test user (photo uploader and event owner)
                testUser = User.builder().kptId(testKptId).email("john@example.com")
                                .name("John Doe").provider(AuthProvider.LOCAL).build();

                // Another user (not a member)
                otherUser = User.builder().kptId(otherKptId).email("jane@example.com")
                                .name("Jane Doe").provider(AuthProvider.LOCAL).build();

                // Test event
                testEvent = Event.builder().evntId(testEvntId).eventTitle("Wedding")
                                .createdBy(testUser).isActive(true).isDeleted(false)
                                .eventDate(LocalDate.of(2026, 8, 15)).build();

                // Test photo in PENDING status
                testPhoto = EventPhotos.builder().photoId(testPhotoId).event(testEvent)
                                .uploadedBy(testUser).filename("photo.jpg").fileType("image/jpeg")
                                .fileSizeInKb(2048L).photoStatus(PhotoStatus.PENDING)
                                .isDeleted(false).createdAt(LocalDateTime.now()).build();

                // Membership for testUser in testEvent
                // Existence in EventMembersDtl = accepted member (no status field)
                acceptedMembership = EventMembersDtl.builder().event(testEvent).user(testUser)
                                .joinedAt(LocalDateTime.now()).build();
        }

        // ==========================================
        // initUpload() Tests
        // ==========================================

        @Test
        @DisplayName("initUpload - valid event and member creates PENDING photo record")
        void initUpload_success() {
                // Arrange
                PhotoUploadRequest request = PhotoUploadRequest.builder().filename("photo.jpg")
                                .fileType("image/jpeg").fileSizeInKb(2048L).build();

                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventMembersRepository.findByEventEvntId(testEvntId))
                                .thenReturn(List.of(acceptedMembership));
                when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

                // Act
                PhotoUploadResponse response = fileServices.initUpload(testEvntId.toString(),
                                request, testUser);

                // Assert
                assertNotNull(response);
                assertNotNull(response.getPhotoId()); // UUID generated
                assertEquals("http://localhost:1080/files/", response.getTusdUploadUrl());

                verify(eventRepository).findByEvntId(testEvntId);
                verify(eventMembersRepository).findByEventEvntId(testEvntId);
                verify(eventPhotosRepository).save(any(EventPhotos.class));
        }

        @Test
        @DisplayName("initUpload - event not found throws RuntimeException")
        void initUpload_eventNotFound_throws() {
                // Arrange
                UUID unknownEvntId = UUID.fromString("0192f3ff-ffff-ffff-ffff-ffffffffffff");
                PhotoUploadRequest request = PhotoUploadRequest.builder().filename("photo.jpg")
                                .fileType("image/jpeg").fileSizeInKb(2048L).build();

                when(eventRepository.findByEvntId(unknownEvntId)).thenReturn(Optional.empty());

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class, () -> fileServices
                                .initUpload(unknownEvntId.toString(), request, testUser));

                assertTrue(exception.getMessage().contains("not found"));
                verify(eventPhotosRepository, never()).save(any());
        }

        @Test
        @DisplayName("initUpload - user not a member throws RuntimeException")
        void initUpload_notMember_throws() {
                // Arrange
                PhotoUploadRequest request = PhotoUploadRequest.builder().filename("photo.jpg")
                                .fileType("image/jpeg").fileSizeInKb(2048L).build();

                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                // otherUser is not in the membership list
                when(eventMembersRepository.findByEventEvntId(testEvntId)).thenReturn(List.of());

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class, () -> fileServices
                                .initUpload(testEvntId.toString(), request, otherUser));

                assertTrue(exception.getMessage().contains("member"));
                verify(eventPhotosRepository, never()).save(any());
        }

        // ==========================================
        // handleTusdHook() — pre-create Tests
        // ==========================================

        @Test
        @DisplayName("pre-create hook - valid photoId and PENDING status accepts upload")
        void handlePreCreate_success() {
                // Arrange
                TusdHookRequest hookRequest = buildPreCreateHookRequest(testPhotoId);

                when(eventPhotosRepository.findByPhotoId(testPhotoId))
                                .thenReturn(Optional.of(testPhoto));
                when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

                // Act
                TusdHookResponse response = fileServices.handleTusdHook(hookRequest);

                // Assert
                assertNotNull(response);
                assertNull(response.getRejectUpload());
                assertNotNull(response.getChangeFileInfo());
                assertEquals(testPhotoId.toString(), response.getChangeFileInfo().getID());

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
                UUID unknownPhotoId = UUID.fromString("550e8400-ffff-ffff-ffff-ffffffffffff");
                TusdHookRequest hookRequest = buildPreCreateHookRequest(unknownPhotoId);

                when(eventPhotosRepository.findByPhotoId(unknownPhotoId))
                                .thenReturn(Optional.empty());

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
                TusdHookRequest hookRequest = buildPreCreateHookRequest(testPhotoId);

                when(eventPhotosRepository.findByPhotoId(testPhotoId))
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
                TusdHookRequest hookRequest = buildPostFinishHookRequest(testPhotoId,
                                "s3-bucket-key-abc123");

                testPhoto.setPhotoStatus(PhotoStatus.UPLOADING); // Currently uploading
                when(eventPhotosRepository.findByPhotoId(testPhotoId))
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
                UUID unknownPhotoId = UUID.fromString("550e8400-ffff-ffff-ffff-ffffffffffff");
                TusdHookRequest hookRequest = buildPostFinishHookRequest(unknownPhotoId, "s3-key");

                when(eventPhotosRepository.findByPhotoId(unknownPhotoId))
                                .thenReturn(Optional.empty());

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
                upload.setID(testPhotoId.toString());
                hookEvent.setUpload(upload);
                hookRequest.setEvent(hookEvent);

                when(eventPhotosRepository.findByPhotoId(testPhotoId))
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
                upload.setID("550e8400-ffff-ffff-ffff-ffffffffffff");
                hookEvent.setUpload(upload);
                hookRequest.setEvent(hookEvent);

                UUID unknownPhotoId = UUID.fromString("550e8400-ffff-ffff-ffff-ffffffffffff");
                when(eventPhotosRepository.findByPhotoId(unknownPhotoId))
                                .thenReturn(Optional.empty());

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
        @DisplayName("getEventPhotos - member lists photos with download URLs (UUID-based)")
        void getEventPhotos_success() {
                // Arrange
                EventPhotos completedPhoto = EventPhotos.builder().photoId(testPhotoId)
                                .event(testEvent).uploadedBy(testUser).filename("photo.jpg")
                                .fileType("image/jpeg").fileSizeInKb(2048L).photoPath("s3-key-abc")
                                .photoStatus(PhotoStatus.COMPLETED).isDeleted(false)
                                .createdAt(LocalDateTime.now())
                                .uploadCompletedAt(LocalDateTime.now()).build();

                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventMembersRepository.findByEventEvntId(testEvntId))
                                .thenReturn(List.of(acceptedMembership));
                when(eventPhotosRepository.findByEventEvntIdAndIsDeletedFalse(testEvntId))
                                .thenReturn(List.of(completedPhoto));

                // Act
                List<PhotoResponse> photos = fileServices.getEventPhotos(testEvntId.toString(),
                                testUser);

                // Assert
                assertEquals(1, photos.size());
                PhotoResponse photo = photos.get(0);
                assertEquals(testPhotoId, photo.getPhotoId());
                assertEquals("photo.jpg", photo.getFilename());
                assertEquals("image/jpeg", photo.getFileType());
                assertEquals(2048L, photo.getFileSizeInKb());
                assertEquals("COMPLETED", photo.getPhotoStatus());
                assertEquals(testKptId, photo.getUploadedByKptId());
                assertEquals("John Doe", photo.getUploadedByName());
                assertEquals("http://localhost:1080/files/" + testPhotoId, photo.getDownloadUrl());
        }

        @Test
        @DisplayName("getEventPhotos - non-member throws RuntimeException")
        void getEventPhotos_notMember_throws() {
                // Arrange
                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventMembersRepository.findByEventEvntId(testEvntId)).thenReturn(List.of()); // No
                                                                                                  // members

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class, () -> fileServices
                                .getEventPhotos(testEvntId.toString(), otherUser));

                assertTrue(exception.getMessage().contains("member"));
        }

        // ==========================================
        // deletePhoto() Tests
        // ==========================================

        @Test
        @DisplayName("deletePhoto - uploader can delete their own photo")
        void deletePhoto_byUploader_success() {
                // Arrange
                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventPhotosRepository.findByPhotoId(testPhotoId))
                                .thenReturn(Optional.of(testPhoto));
                when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(testPhoto);

                // Act: testUser uploaded the photo AND owns the event
                fileServices.deletePhoto(testEvntId.toString(), testPhotoId.toString(), testUser);

                // Assert
                assertTrue(testPhoto.getIsDeleted());
                verify(eventPhotosRepository).save(testPhoto);
        }

        @Test
        @DisplayName("deletePhoto - event owner can delete any photo in their event")
        void deletePhoto_byEventOwner_success() {
                // Arrange: otherUser uploaded the photo, but testUser owns the event
                UUID otherPhotoId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
                EventPhotos otherUserPhoto = EventPhotos.builder().photoId(otherPhotoId)
                                .event(testEvent).uploadedBy(otherUser) // Uploaded by otherUser
                                .filename("other.jpg").fileType("image/jpeg").fileSizeInKb(1024L)
                                .photoStatus(PhotoStatus.COMPLETED).isDeleted(false).build();

                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventPhotosRepository.findByPhotoId(otherPhotoId))
                                .thenReturn(Optional.of(otherUserPhoto));
                when(eventPhotosRepository.save(any(EventPhotos.class))).thenReturn(otherUserPhoto);

                // Act: testUser is the event owner, deleting otherUser's photo
                fileServices.deletePhoto(testEvntId.toString(), otherPhotoId.toString(), testUser);

                // Assert
                assertTrue(otherUserPhoto.getIsDeleted());
        }

        @Test
        @DisplayName("deletePhoto - unauthorized user (not uploader, not owner) throws RuntimeException")
        void deletePhoto_unauthorized_throws() {
                // Arrange: randomUser is neither the uploader nor the event owner
                UUID randomKptId = UUID.fromString("0192f3ff-0000-0000-0000-000000000003");
                User randomUser = User.builder().kptId(randomKptId).email("random@example.com")
                                .name("Random User").build();

                when(eventRepository.findByEvntId(testEvntId)).thenReturn(Optional.of(testEvent));
                when(eventPhotosRepository.findByPhotoId(testPhotoId))
                                .thenReturn(Optional.of(testPhoto));

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class,
                                () -> fileServices.deletePhoto(testEvntId.toString(),
                                                testPhotoId.toString(), randomUser));

                assertTrue(exception.getMessage().contains("not authorized"));
                assertFalse(testPhoto.getIsDeleted()); // Should NOT be deleted
                verify(eventPhotosRepository, never()).save(any());
        }

        @Test
        @DisplayName("deletePhoto - photo does not belong to event throws RuntimeException")
        void deletePhoto_photoNotInEvent_throws() {
                // Arrange
                UUID otherEvntId = UUID.fromString("0192f3ff-0000-0000-0000-000000000004");
                Event otherEvent = Event.builder().evntId(otherEvntId).eventTitle("Other Event")
                                .createdBy(testUser).build();

                when(eventRepository.findByEvntId(otherEvntId)).thenReturn(Optional.of(otherEvent));
                when(eventPhotosRepository.findByPhotoId(testPhotoId))
                                .thenReturn(Optional.of(testPhoto));
                // testPhoto belongs to testEvent (evntId = testEvntId),
                // not otherEvent (evntId = otherEvntId)

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class,
                                () -> fileServices.deletePhoto(otherEvntId.toString(),
                                                testPhotoId.toString(), testUser));

                assertTrue(exception.getMessage().contains("does not belong"));
        }

        // ==========================================
        // Helper Methods to Build TUSd Hook Requests
        // ==========================================

        /**
         * Builds a TUSd pre-create hook request with the given photoId UUID in
         * metadata.
         */
        private TusdHookRequest buildPreCreateHookRequest(UUID photoId) {
                String photoIdStr = photoId.toString();
                TusdHookRequest hookRequest = new TusdHookRequest();
                hookRequest.setType("pre-create");

                TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
                TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
                upload.setID(photoIdStr);
                upload.setSize(2097152L);
                upload.setOffset(0L);
                upload.setMetaData(Map.of("photoId", photoIdStr, "filename", "photo.jpg",
                                "filetype", "image/jpeg"));
                hookEvent.setUpload(upload);
                hookRequest.setEvent(hookEvent);

                return hookRequest;
        }

        /**
         * Builds a TUSd post-finish hook request with the given photoId and S3 key.
         */
        private TusdHookRequest buildPostFinishHookRequest(UUID photoId, String s3Key) {
                String photoIdStr = photoId.toString();
                TusdHookRequest hookRequest = new TusdHookRequest();
                hookRequest.setType("post-finish");

                TusdHookRequest.Event hookEvent = new TusdHookRequest.Event();
                TusdHookRequest.Upload upload = new TusdHookRequest.Upload();
                upload.setID(photoIdStr);
                upload.setSize(2097152L);
                upload.setOffset(2097152L);
                upload.setStorage(Map.of("Key", s3Key, "Bucket", "my-bucket"));
                hookEvent.setUpload(upload);
                hookRequest.setEvent(hookEvent);

                return hookRequest;
        }
}
