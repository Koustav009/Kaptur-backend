package com.koustav.kaptur.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.EventPhotos;

/**
 * Repository for EventPhotos entity. Handles database operations for photos
 * associated with events.
 */
@Repository
public interface EventPhotosRepository extends JpaRepository<EventPhotos, UUID> {

    // Find all photos for a specific event
    List<EventPhotos> findByEventEvntId(UUID eventId);

    // Find all photos uploaded by a specific user
    List<EventPhotos> findByUploadedByKptId(UUID userId);

    // Look up a photo by its unique UUID-based photoId (used by TUSd hooks)
    Optional<EventPhotos> findByPhotoId(UUID photoId);

    // Find all non-soft-deleted photos for a specific event
    List<EventPhotos> findByEventEvntIdAndIsDeletedFalse(UUID eventId);
}