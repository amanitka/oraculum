package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TickerDocumentRawRepository extends JpaRepository<TickerDocumentRawEntity, TickerDocumentRawEntity.TickerDocumentRawId> {

    @Modifying
    @Query("UPDATE TickerDocumentRawEntity e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id AND e.reportPeriod = :reportPeriod")
    int updateStatus(@Param("id") String id, @Param("reportPeriod") LocalDate reportPeriod, @Param("status") String status);
}
