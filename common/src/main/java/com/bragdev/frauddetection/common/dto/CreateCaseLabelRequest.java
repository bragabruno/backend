package com.bragdev.frauddetection.common.dto;

import com.bragdev.frauddetection.common.enums.LabelType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseLabelRequest {

    @NotNull
    private LabelType label;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double confidence;

    private String reason;
}
