package com.koustav.kaptur.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.koustav.kaptur.model.UserRoleMst;
import com.koustav.kaptur.model.enums.EventRole;
import com.koustav.kaptur.model.enums.SystemRole;
import com.koustav.kaptur.repository.UserRoleMstRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the USER_ROLE_MST table with every role defined in our two role enums:
 *
 *   SystemRole (system-wide roles): SUPER_ADMIN, USER
 *   EventRole  (event-scoped roles): ADMIN, PHOTOMAN, GUEST
 *
 * We use CommandLineRunner so this runs once at application startup, AFTER Spring
 * has finished wiring all beans (including the JPA repositories we inject below).
 *
 * The seeder is IDEMPOTENT: it checks whether each role already exists before
 * inserting, so restarting the app never creates duplicate rows. This replaces
 * the old manual SQL seed script — no more "ADMIN role not found" errors on a
 * fresh database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final UserRoleMstRepository userRoleMstRepository;

    @Override
    public void run(String... args) {
        log.info("Running role seeder for USER_ROLE_MST...");

        // System-level roles live in the SystemRole enum and are marked isSystemRole = true.
        // Event-level roles live in the EventRole enum and are marked isSystemRole = false.
        seedRole(SystemRole.SUPER_ADMIN, "Super Administrator", "Full system access", true);
        seedRole(SystemRole.USER, "User", "Standard user", true);
        seedRole(EventRole.ADMIN, "Administrator", "Event administrator", false);
        seedRole(EventRole.PHOTOMAN, "Photographer", "Photo contributor", false);
        seedRole(EventRole.GUEST, "Guest", "Read-only access", false);

        log.info("Role seeding complete.");
    }

    /**
     * Inserts one role row if a role with the same role_code does not already exist.
     * We look up by the enum's name() because role_code is stored as a String column.
     */
    private void seedRole(Enum<?> roleCode, String roleName, String description, boolean isSystemRole) {
        String code = roleCode.name();
        if (userRoleMstRepository.findByRoleCode(code).isPresent()) {
            log.debug("Role '{}' already exists — skipping.", code);
            return;
        }

        UserRoleMst role = new UserRoleMst();
        role.setRoleCode(code);
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setIsSystemRole(isSystemRole);
        role.setIsActive(true);

        userRoleMstRepository.save(role);
        log.info("Seeded role '{}' ({})", code, roleName);
    }
}
