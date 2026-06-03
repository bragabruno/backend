package com.bragdev.frauddetection.common.mapper;

import com.bragdev.frauddetection.common.dto.CaseSummaryDto;
import com.bragdev.frauddetection.common.dto.CaseDetailDto;
import com.bragdev.frauddetection.common.model.FraudCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FraudCaseMapper {

    CaseSummaryDto toSummaryDto(FraudCase fraudCase);

    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "riskScore", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "labels", ignore = true)
    CaseDetailDto toDetailDto(FraudCase fraudCase);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    FraudCase toEntity(CaseSummaryDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    void updateEntity(CaseSummaryDto dto, @MappingTarget FraudCase fraudCase);
}
