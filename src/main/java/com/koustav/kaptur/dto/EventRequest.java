package com.koustav.kaptur.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating an Event.
 * We use validation annotations to ensure the data is correct before it reaches the service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    @NotBlank(message = "Event title is required")
    private String eventTitle;

    private String description;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    private String eventLocation;
}
