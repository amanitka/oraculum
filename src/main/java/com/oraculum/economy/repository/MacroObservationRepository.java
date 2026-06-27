package com.oraculum.economy.repository;

import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.domain.MacroObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MacroObservationRepository extends JpaRepository<MacroObservationEntity, Long> {
    List<MacroObservationEntity> findByIndicatorCodeOrderByObservationDateDesc(MacroIndicator indicator);

    List<MacroObservationEntity> findByIndicatorCodeIn(List<MacroIndicator> indicators);
}
