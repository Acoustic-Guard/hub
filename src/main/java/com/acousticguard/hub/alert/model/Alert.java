package com.acousticguard.hub.alert.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Alert {

    @Id
    private Long id;
}
