package com.oraculum.analyst.repository;

import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyAnalysisRepository extends JpaRepository<CompanyAnalysisEntity, UUID> {
    Page<CompanyAnalysisEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}