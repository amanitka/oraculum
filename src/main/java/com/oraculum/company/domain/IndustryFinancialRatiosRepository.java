package com.oraculum.company.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndustryFinancialRatiosRepository extends JpaRepository<IndustryFinancialRatiosEntity, IndustryFinancialRatiosId> {
    List<IndustryFinancialRatiosEntity> findByIndustryName(String industryName);
}
