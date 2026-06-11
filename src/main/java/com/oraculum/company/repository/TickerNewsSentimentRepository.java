package com.oraculum.company.repository;

import com.oraculum.company.domain.TickerNewsSentimentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TickerNewsSentimentRepository extends JpaRepository<TickerNewsSentimentEntity, String> {
    Optional<TickerNewsSentimentEntity> findByTicker(String ticker);
}
