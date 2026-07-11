package com.koustav.kaptur.model.enums;

/**
 * Event-level roles that apply within a specific event context.
 * These are stored in the USER_ROLE_MST table and assigned via EVENT_MEMBERS_DTL.
 * Looked up from DB when accessing event-specific resources.
 * 
 * ADMIN - Event administrator (can manage event settings, members, delete photos)
 * PHOTOMAN - Photographer (can upload and manage their own photos)
 * GUEST - Read-only access (can view photos but not upload)
 */
public enum EventRole {
    ADMIN,
    PHOTOMAN,
    GUEST
}
