package com.oraculum.company.repository;

import com.oraculum.company.domain.DailyMarketSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyMarketSignalRepository extends JpaRepository<DailyMarketSignalEntity,
        DailyMarketSignalEntity.DailyMarketSignalId> {
    List<DailyMarketSignalEntity> findByCompanyIdAndTradeDateAfter(int companyId, LocalDate after);

    List<DailyMarketSignalEntity> findByCompanyIdAndTradeDateAfterAndFlagLastDayOfMonth(int companyId,
                                                                                        LocalDate after,
                                                                                        String flagLastDayOfMonth);
}