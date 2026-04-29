package com.koustav.kaptur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.EventPhotos;

/**
 * Repository for EventPhotos entity.
 * Handles database operations for photos associated with events.
 */
@Repository
public interface EventPhotosRepository extends JpaRepository<EventPhotos, Long> {
    
    // Find all photos for a specific event
    List<EventPhotos> findByEventId(Long eventId);
    
    // Find all photos uploaded by a specific user
    List<EventPhotos> findByUploadedById(Long userId);
}
