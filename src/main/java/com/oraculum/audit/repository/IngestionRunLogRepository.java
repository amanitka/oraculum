package com.oraculum.audit.repository;

import com.oraculum.audit.domain.IngestionRunLogEntity;
import com.oraculum.audit.domain.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngestionRunLogRepository extends JpaRepository<IngestionRunLogEntity, Integer> {

    List<IngestionRunLogEntity> findByDataset(String dataset);

    Optional<IngestionRunLogEntity> findByDatasetAndRunIdAndFileChecksumAndStatus(
            String dataset, String runId, String fileChecksum, IngestionStatus status);
}
