package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Integer> {
    List<CompanyEntity> findByMarketAndTickerIn(String market, List<String> tickers);
}
