package com.bragdev.frauddetection.common.mapper;

import com.bragdev.frauddetection.common.dto.CaseLabelDto;
import com.bragdev.frauddetection.common.model.FraudLabel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FraudLabelMapper {

    CaseLabelDto toDto(FraudLabel fraudLabel);
}
