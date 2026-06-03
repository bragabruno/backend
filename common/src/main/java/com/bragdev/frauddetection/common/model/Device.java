package com.bragdev.frauddetection.common.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String fingerprint;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private boolean trusted;

    @Column(name = "first_seen", updatable = false)
    private Instant firstSeen;

    @Column(name = "last_seen")
    private Instant lastSeen;
}
