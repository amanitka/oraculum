package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentPendingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TickerDocumentPendingRepository extends JpaRepository<TickerDocumentPendingEntity, TickerDocumentPendingEntity.TickerDocumentPendingId> {

    @Query("""
               SELECT e
               FROM TickerDocumentPendingEntity e
               WHERE e.reportPeriod >= :minReportedPeriod
                 AND e.documentPriority <= :maxPriority
                 AND e.status IN (com.oraculum.company.api.domain.TickerDocumentProcessingStatus.PENDING)
            """)
    List<TickerDocumentPendingEntity> findPendingDocuments(@Param("maxPriority") int maxPriority, @Param("minReportedPeriod") LocalDate minReportedPeriod, Pageable pageable);

    @Query("""
            SELECT e
            FROM TickerDocumentPendingEntity e
            WHERE e.ticker = :ticker
              AND e.market = :market
              AND e.reportPeriod >= :minReportedPeriod
              AND e.documentPriority <= :maxPriority
              AND e.status IN (com.oraculum.company.api.domain.TickerDocumentProcessingStatus.PENDING, com.oraculum.company.api.domain.TickerDocumentProcessingStatus.FAILED)
            """)
    List<TickerDocumentPendingEntity> findPendingDocumentsByTickerAndMarket(@Param("ticker") String ticker, @Param("market") String market, @Param("maxPriority") int maxPriority, @Param("minReportedPeriod") LocalDate minReportedPeriod);
}
