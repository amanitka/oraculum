package com.oraculum.economy.repository;

import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.domain.MacroSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MacroSummaryRepository extends JpaRepository<MacroSummaryEntity, MacroIndicator> {
}
