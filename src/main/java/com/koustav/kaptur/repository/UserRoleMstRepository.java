package com.koustav.kaptur.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.UserRoleMst;
import com.koustav.kaptur.model.enums.Role;

/**
 * Repository for UserRoleMst entity.
 * Provides lookups for role master data by role code.
 */
@Repository
public interface UserRoleMstRepository extends JpaRepository<UserRoleMst, UUID> {

    /**
     * Finds a role master record by its role code enum value.
     * Used when assigning roles to event members.
     */
    Optional<UserRoleMst> findByRoleCode(Role roleCode);
}
