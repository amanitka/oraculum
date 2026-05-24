package com.oraculum.company.repository;

import com.oraculum.company.domain.SharePriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharePriceRepository extends JpaRepository<SharePriceEntity, SharePriceEntity.SharePriceId> {
    List<SharePriceEntity> findByTicker(String ticker);
}