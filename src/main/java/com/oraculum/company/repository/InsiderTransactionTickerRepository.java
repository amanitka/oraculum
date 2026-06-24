package com.oraculum.company.repository;

import com.oraculum.company.domain.InsiderTransactionTickerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsiderTransactionTickerRepository extends JpaRepository<InsiderTransactionTickerEntity, String> {

    @Query("SELECT MAX(e.filingDate) FROM InsiderTransactionTickerEntity e")
    Optional<LocalDateTime> findMaxFilingDate();

    List<InsiderTransactionTickerEntity> findByTickerAndTradeDateAfterOrderByFilingDateDesc(String ticker, LocalDate after);

}
