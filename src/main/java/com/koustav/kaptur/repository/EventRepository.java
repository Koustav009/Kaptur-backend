package com.koustav.kaptur.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Finds all events created by a specific user. Spring Data JPA parses this
     * method name to create a query.
     */
    List<Event> findByCreatedByKptId(UUID userId);

    /**
     * Finds events by their custom public ID (evntid).
     */
    java.util.Optional<Event> findByEvntId(UUID evntid);
}
