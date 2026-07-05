package com.oraculum.company.repository;

import com.oraculum.company.domain.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Integer> {
    Optional<CompanyEntity> findByTickerAndMarket(String ticker, String market);

    List<CompanyEntity> findByMarket(String market);
}
