package com.koustav.kaptur.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMembersId implements Serializable {

    private Long event; // ← matches the ID type of the Event entity
    private Long user;  // ← matches the ID type of the User entity
}
