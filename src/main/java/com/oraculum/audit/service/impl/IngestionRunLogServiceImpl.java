package com.oraculum.audit.service.impl;

import com.oraculum.audit.api.dto.IngestionRunLogDto;
import com.oraculum.audit.domain.IngestionRunLogEntity;
import com.oraculum.audit.domain.IngestionStatus;
import com.oraculum.audit.repository.IngestionRunLogRepository;
import com.oraculum.audit.service.IngestionRunLogService;
import com.oraculum.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngestionRunLogServiceImpl implements IngestionRunLogService {

    private final IngestionRunLogRepository ingestionRunLogRepository;

    private IngestionRunLogEntity getRunLogEntity(Long id) {
        return ingestionRunLogRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(IngestionRunLogEntity.class, id));
    }

    @Override
    public List<IngestionRunLogDto> getRunLogsByDataset(String dataset) {
        return ingestionRunLogRepository.findByDataset(dataset).stream().map(IngestionRunLogDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public IngestionRunLogDto getRunLogById(Long id) {
        return IngestionRunLogDto.fromEntity(getRunLogEntity(id));
    }

    @Override
    public boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum) {
        return ingestionRunLogRepository.findByDatasetAndRunIdAndFileChecksumAndStatus(dataset, runId, fileChecksum,
                IngestionStatus.SUCCESS).isPresent();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRunLogDto startRunLog(String dataset, String runId, String fileChecksum) {
        IngestionRunLogEntity runLog = new IngestionRunLogEntity();
        runLog.setDataset(dataset);
        runLog.setRunId(runId);
        runLog.setFileChecksum(fileChecksum);
        runLog.setStatus(IngestionStatus.RUNNING);
        return IngestionRunLogDto.fromEntity(ingestionRunLogRepository.save(runLog));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRunLogDto completeRunLog(IngestionRunLogDto runLogDto) {
        IngestionRunLogEntity runLog = getRunLogEntity(runLogDto.getId());
        runLog.setStatus(IngestionStatus.SUCCESS);
        runLog.setLoadedRows(runLogDto.getLoadedRows());
        runLog.setMergedRows(runLogDto.getMergedRows());
        runLog.setDurationMs(runLogDto.getDurationMs());
        return IngestionRunLogDto.fromEntity(ingestionRunLogRepository.save(runLog));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRunLogDto failRunLog(IngestionRunLogDto runLogDto) {
        IngestionRunLogEntity runLog = getRunLogEntity(runLogDto.getId());
        runLog.setStatus(IngestionStatus.FAILED);
        runLog.setErrorText(runLogDto.getErrorText());
        runLog.setDurationMs(runLogDto.getDurationMs());
        return IngestionRunLogDto.fromEntity(ingestionRunLogRepository.save(runLog));
    }
}