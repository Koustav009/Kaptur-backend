package com.koustav.kaptur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.kaptur.model.Event;



@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCreatedByUserId(Long userId);
}
