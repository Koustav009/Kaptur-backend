package com.koustav.kaptur.model.enums;

/**
 * Represents the upload lifecycle state of a photo.
 * 
 * PENDING   – Record created in DB, awaiting the TUSd pre-create hook to validate.
 * UPLOADING – TUSd pre-create hook passed, file chunks are being uploaded by the client.
 * COMPLETED – TUSd post-finish hook received, file is fully stored in S3.
 * FAILED    – Upload was terminated by TUSd or an error occurred during upload.
 */
public enum PhotoStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}