package com.oraculum.analyst.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyAnalysisViewRepository extends JpaRepository<CompanyAnalysisViewEntity, UUID> {
    
    Page<CompanyAnalysisViewEntity> findByIsLatestTrueOrderByCreatedAtDesc(Pageable pageable);

    List<CompanyAnalysisViewEntity> findByCompanyIdOrderByCreatedAtDesc(Integer companyId);
}
