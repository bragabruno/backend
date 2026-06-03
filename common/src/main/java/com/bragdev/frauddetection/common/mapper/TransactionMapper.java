package com.bragdev.frauddetection.common.mapper;

import com.bragdev.frauddetection.common.dto.TransactionDto;
import com.bragdev.frauddetection.common.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "latestRiskScore", ignore = true)
    TransactionDto toDto(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Transaction toEntity(TransactionDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(TransactionDto dto, @MappingTarget Transaction transaction);
}
