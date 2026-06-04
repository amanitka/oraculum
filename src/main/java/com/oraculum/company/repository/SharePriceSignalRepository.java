package com.oraculum.company.repository;

import com.oraculum.company.domain.SharePriceSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SharePriceSignalRepository extends JpaRepository<SharePriceSignalEntity,
        SharePriceSignalEntity.SharePriceSignalId> {
    List<SharePriceSignalEntity> findByCompanyIdAndTradeDateAfter(int companyId, LocalDate after);

    List<SharePriceSignalEntity> findByCompanyIdAndTradeDateAfterAndFlagLastDayOfMonth(int companyId,
                                                                                        LocalDate after,
                                                                                        String flagLastDayOfMonth);
}