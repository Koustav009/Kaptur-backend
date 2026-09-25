package com.koustav.kaptur.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_ROLE_MST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleMst {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roleId;

    // role_code is stored as a plain String because USER_ROLE_MST holds roles from
    // BOTH enums: SystemRole (SUPER_ADMIN, USER) and EventRole (ADMIN, PHOTOMAN, GUEST).
    // Java has a single type per field, so we can't use one enum for both groups here.
    @Column(nullable = false, unique = true, length = 50)
    private String roleCode; // "SUPER_ADMIN", "USER", "ADMIN", "PHOTOMAN", "GUEST"

    @Column(nullable = false, length = 100)
    private String roleName; // e.g., "Administrator", "Photographer", "User"

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean isSystemRole = false; // System roles cannot be deleted

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

