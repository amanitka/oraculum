package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TickerRepository extends JpaRepository<TickerEntity, Integer> {
    Optional<TickerEntity> findByTickerAndMarket(String ticker, String market);
}