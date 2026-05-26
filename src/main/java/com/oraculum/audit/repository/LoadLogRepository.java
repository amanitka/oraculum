package com.oraculum.audit.repository;

import com.oraculum.audit.domain.LoadLogEntity;
import com.oraculum.audit.domain.LoadLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoadLogRepository extends JpaRepository<LoadLogEntity, Long> {

    List<LoadLogEntity> findByDataset(String dataset);

    Optional<LoadLogEntity> findByDatasetAndRunIdAndFileChecksumAndStatus(String dataset, String runId,
                                                                          String fileChecksum, LoadLogStatus status);
}
