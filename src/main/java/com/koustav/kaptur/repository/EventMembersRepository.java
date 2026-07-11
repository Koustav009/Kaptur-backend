package com.koustav.kaptur.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.EventMembersDtl;

/**
 * Repository for EventMembersDtl entity. Each record represents an accepted
 * member of an event. A unique constraint on (event_id, user_id) ensures a user
 * can only be a member of an event once.
 */
@Repository
public interface EventMembersRepository extends JpaRepository<EventMembersDtl, UUID> {

    // Find all events a specific user is part of
    List<EventMembersDtl> findByUserKptId(UUID userId);

    // Find all members of a specific event
    List<EventMembersDtl> findByEventEvntId(UUID eventId);
}