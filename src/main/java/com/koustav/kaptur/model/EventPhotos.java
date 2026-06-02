package com.koustav.kaptur.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.koustav.kaptur.model.enums.PhotoStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity representing a photo uploaded to an event.
 * Tracks metadata, upload lifecycle status (via TUSd hooks), and S3 storage path.
 * Uses UUID-based photoId as the public identifier (also the TUSd upload ID).
 * 
 * We use @Getter/@Setter instead of @Data to avoid circular reference issues
 * with the lazily-loaded event and uploadedBy relationships.
 */
@Entity
@Table(name = "event_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"event", "uploadedBy"})
@EqualsAndHashCode(exclude = {"event", "uploadedBy"})
public class EventPhotos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique UUID-based public identifier for this photo (also used as the TUSd upload ID)
    @Column(unique = true, nullable = false)
    private String photoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // S3 object key stored by the post-finish TUSd hook (e.g. "a1b2c3d4...")
    private String photoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSizeInKb;

    // Tracks the current upload lifecycle state (PENDING → UPLOADING → COMPLETED/FAILED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PhotoStatus photoStatus = PhotoStatus.PENDING;

    // Soft delete flag to avoid permanent data loss
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Timestamp set when the TUSd post-finish hook confirms the upload
    private LocalDateTime uploadCompletedAt;
}