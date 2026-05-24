package com.oraculum.company.repository;

import com.oraculum.company.domain.NewsTickerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsTickerRepository extends JpaRepository<NewsTickerEntity, NewsTickerEntity.NewsTickerId> {
    List<NewsTickerEntity> findByTicker(String ticker);
}