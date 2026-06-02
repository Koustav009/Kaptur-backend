package com.koustav.kaptur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating a photo upload.
 * The client sends the file metadata; the server generates a UUID photoId
 * and returns the TUSd upload URL where the client should upload the file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoUploadRequest {

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "File type (MIME) is required")
    private String fileType;

    @NotNull(message = "File size in KB is required")
    private Long fileSizeInKb;
}