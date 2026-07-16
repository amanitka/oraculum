package com.oraculum.company.repository;

import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.domain.TickerDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TickerDocumentRepository extends JpaRepository<TickerDocumentEntity, TickerDocumentEntity.TickerDocumentId> {
}
