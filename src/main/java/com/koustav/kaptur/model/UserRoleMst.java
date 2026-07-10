package com.koustav.kaptur.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.koustav.kaptur.model.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(nullable = false, unique = true, length = 50)
    @Enumerated(EnumType.STRING)
    private Role roleCode; // e.g., "ADMIN", "PHOTOMAN", "USER"

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
