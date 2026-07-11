package com.koustav.kaptur.model.enums;

/**
 * System-level roles that apply across the entire application.
 * These are embedded in the JWT access token for fast, stateless authorization.
 * 
 * SUPER_ADMIN - Full system access (manage all users, events, system settings)
 * USER - Standard user (can create/join events, upload photos)
 */
public enum SystemRole {
    SUPER_ADMIN,
    USER
}
