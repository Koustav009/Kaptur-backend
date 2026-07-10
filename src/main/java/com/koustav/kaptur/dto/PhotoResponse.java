package com.koustav.kaptur.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning photo metadata in listing responses.
 * Includes the TUSd download URL so the client can fetch/stream the file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoResponse {

    private UUID photoId;
    private String filename;
    private String fileType;
    private Long fileSizeInKb;
    private String photoPath;
    private String photoStatus;
    private UUID uploadedByKptId;
    private String uploadedByName;
    private LocalDateTime createdAt;
    private LocalDateTime uploadCompletedAt;

    // Direct TUSd download URL: http://localhost:1080/files/{photoId}
    private String downloadUrl;
}
