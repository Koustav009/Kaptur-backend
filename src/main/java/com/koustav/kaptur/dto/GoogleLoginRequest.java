package com.koustav.kaptur.dto;

import lombok.Data;

/**
 * Data Transfer Object for Google Login. We receive the user's name, email, and
 * Google ID from the Flutter app.
 */
@Data
public class GoogleLoginRequest {
    // private String name;
    // private String email;
    private String id;
    // private String photoUrl;
}
