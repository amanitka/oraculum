package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerDocumentViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TickerDocumentViewRepository extends JpaRepository<TickerDocumentViewEntity, TickerDocumentViewEntity.TickerDocumentViewId> {
    List<TickerDocumentViewEntity> findByTickerAndMarket(String ticker, String market);
}
