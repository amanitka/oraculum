package com.oraculum.audit.service.impl;

import com.oraculum.audit.api.dto.IngestionRunLogDto;
import com.oraculum.audit.domain.IngestionRunLogEntity;
import com.oraculum.audit.domain.IngestionStatus;
import com.oraculum.audit.repository.IngestionRunLogRepository;
import com.oraculum.audit.service.IngestionRunLogService;
import com.oraculum.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngestionRunLogServiceImpl implements IngestionRunLogService {

    private final IngestionRunLogRepository ingestionRunLogRepository;

    @Override
    public List<IngestionRunLogDto> getAllRunLogs() {
        return ingestionRunLogRepository.findAll().stream()
                .map(IngestionRunLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<IngestionRunLogDto> getRunLogsByDataset(String dataset) {
        return ingestionRunLogRepository.findByDataset(dataset).stream()
                .map(IngestionRunLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IngestionRunLogDto getRunLogById(Integer id) {
        return ingestionRunLogRepository.findById(id)
                .map(IngestionRunLogDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(IngestionRunLogEntity.class, id));
    }

    @Override
    public boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum) {
        return ingestionRunLogRepository
                .findByDatasetAndRunIdAndFileChecksumAndStatus(dataset, runId, fileChecksum, IngestionStatus.SUCCESS)
                .isPresent();
    }

    @Override
    @Transactional
    public IngestionRunLogEntity startRunLog(String dataset, String runId, String fileChecksum) {
        IngestionRunLogEntity runLog = new IngestionRunLogEntity();
        runLog.setDataset(dataset);
        runLog.setRunId(runId);
        runLog.setFileChecksum(fileChecksum);
        runLog.setStatus(IngestionStatus.RUNNING);
        return ingestionRunLogRepository.save(runLog);
    }

    @Override
    @Transactional
    public void completeRunLog(IngestionRunLogEntity runLog, int loadedRows, int mergedRows, int durationMs) {
        runLog.setStatus(IngestionStatus.SUCCESS);
        runLog.setLoadedRows(loadedRows);
        runLog.setMergedRows(mergedRows);
        runLog.setDurationMs(durationMs);
        ingestionRunLogRepository.save(runLog);
    }

    @Override
    @Transactional
    public void failRunLog(IngestionRunLogEntity runLog, String errorText, int durationMs) {
        runLog.setStatus(IngestionStatus.FAILED);
        runLog.setErrorText(errorText);
        runLog.setDurationMs(durationMs);
        ingestionRunLogRepository.save(runLog);
    }
}
