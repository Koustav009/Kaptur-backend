package com.koustav.kaptur.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned after initiating a photo upload.
 * Contains the server-assigned photoId (UUID) and the TUSd base URL
 * where the client should start the resumable upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoUploadResponse {

    // UUID-based unique identifier for this photo (also used as TUSd upload ID)
    private UUID photoId;

    // The TUSd server base URL (e.g. http://localhost:1080/files/)
    // Client appends the photoId to this when creating the TUS upload
    private String tusdUploadUrl;
}
