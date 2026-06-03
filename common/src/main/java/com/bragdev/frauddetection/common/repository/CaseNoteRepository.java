package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, UUID> {

    List<CaseNote> findByCaseIdOrderByCreatedAtAsc(UUID caseId);
}
