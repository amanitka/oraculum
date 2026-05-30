package com.oraculum.company.repository;

import com.oraculum.company.domain.NewsTickerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface NewsTickerRepository extends JpaRepository<NewsTickerEntity, NewsTickerEntity.NewsTickerId> {
    Page<NewsTickerEntity> findByTickerAndTimePublishedAfter(String ticker, OffsetDateTime after, Pageable pageable);
}