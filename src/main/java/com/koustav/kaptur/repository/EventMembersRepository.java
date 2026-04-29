package com.koustav.kaptur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.EventMembers;
import com.koustav.kaptur.model.EventMembersId;

/**
 * Repository for EventMembers entity.
 * Since EventMembers uses a composite primary key (EventMembersId),
 * we specify it as the second generic parameter.
 */
@Repository
public interface EventMembersRepository extends JpaRepository<EventMembers, EventMembersId> {
    
    // Find all events a specific user is part of
    List<EventMembers> findByUserId(Long userId);
    
    // Find all members of a specific event
    List<EventMembers> findByEventId(Long eventId);
}
