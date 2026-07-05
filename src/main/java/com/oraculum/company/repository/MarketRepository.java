package com.oraculum.company.repository;

import com.oraculum.company.domain.MarketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketRepository extends JpaRepository<MarketEntity, String> {
    
    @Query("SELECT m.marketId FROM MarketEntity m")
    List<String> findAllMarketIds();
}
