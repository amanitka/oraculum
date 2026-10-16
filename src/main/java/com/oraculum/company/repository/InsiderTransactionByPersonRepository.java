package com.oraculum.company.repository;

import com.oraculum.company.domain.InsiderTransactionByPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsiderTransactionByPersonRepository extends JpaRepository<InsiderTransactionByPersonEntity, String> {
    List<InsiderTransactionByPersonEntity> findByTickerOrderByValueSold12mDesc(String ticker);
}
