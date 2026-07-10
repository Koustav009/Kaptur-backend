package com.koustav.kaptur.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning Event data to the client.
 * This prevents us from exposing internal database details or lazy-loading issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private UUID evntId;
    private String eventTitle;
    private String description;
    private LocalDate eventDate;
    private String eventLocation;
    private UUID creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
