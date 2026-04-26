package com.koustav.kaptur.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMembersId implements Serializable {

    private String event; // ← must match field NAME in entity (not column name)
    private String user; // ← must match field NAME in entity
}
