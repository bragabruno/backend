package com.bragdev.frauddetection.common.mapper;

import com.bragdev.frauddetection.common.dto.RiskScoreDto;
import com.bragdev.frauddetection.common.model.RiskScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RiskScoreMapper {

    RiskScoreDto toDto(RiskScore riskScore);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RiskScore toEntity(RiskScoreDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(RiskScoreDto dto, @MappingTarget RiskScore riskScore);
}
