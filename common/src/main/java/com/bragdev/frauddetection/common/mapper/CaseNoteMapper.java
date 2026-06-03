package com.bragdev.frauddetection.common.mapper;

import com.bragdev.frauddetection.common.dto.CaseNoteDto;
import com.bragdev.frauddetection.common.model.CaseNote;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CaseNoteMapper {

    CaseNoteDto toDto(CaseNote caseNote);
}
