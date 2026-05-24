package com.oraculum.company.repository;

import com.oraculum.company.domain.MarketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketRepository extends JpaRepository<MarketEntity, String> {
}