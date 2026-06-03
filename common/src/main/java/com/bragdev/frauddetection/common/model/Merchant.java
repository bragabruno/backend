package com.bragdev.frauddetection.common.model;

import com.bragdev.frauddetection.common.enums.RiskTier;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 4)
    private String mcc;

    @Enumerated(EnumType.STRING)
    private RiskTier riskTier;

    @Column(length = 2)
    private String country;
}
