package com.fotoowl.clone.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fotoowl.clone.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByCreatedByUserId(Long userId);
}
