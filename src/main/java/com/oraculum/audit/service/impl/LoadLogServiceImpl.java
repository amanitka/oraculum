package com.oraculum.audit.service.impl;

import com.oraculum.audit.api.dto.LoadLogDto;
import com.oraculum.audit.domain.LoadLogEntity;
import com.oraculum.audit.domain.LoadLogStatus;
import com.oraculum.audit.repository.LoadLogRepository;
import com.oraculum.audit.service.LoadLogService;
import com.oraculum.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoadLogServiceImpl implements LoadLogService {

    private final LoadLogRepository loadLogRepository;

    private LoadLogEntity getRunLogEntity(Long id) {
        return loadLogRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(LoadLogEntity.class, id));
    }

    @Override
    public List<LoadLogDto> getRunLogsByDataset(String dataset) {
        return loadLogRepository.findByDataset(dataset).stream().map(LoadLogDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public LoadLogDto getRunLogById(Long id) {
        return LoadLogDto.fromEntity(getRunLogEntity(id));
    }

    @Override
    public boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum) {
        return loadLogRepository.findByDatasetAndRunIdAndFileChecksumAndStatus(dataset, runId, fileChecksum,
                LoadLogStatus.SUCCESS).isPresent();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoadLogDto startRunLog(String dataset, String runId, String fileChecksum) {
        LoadLogEntity runLog = new LoadLogEntity();
        runLog.setDataset(dataset);
        runLog.setRunId(runId);
        runLog.setFileChecksum(fileChecksum);
        runLog.setStatus(LoadLogStatus.RUNNING);
        return LoadLogDto.fromEntity(loadLogRepository.save(runLog));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoadLogDto completeRunLog(LoadLogDto runLogDto) {
        LoadLogEntity runLog = getRunLogEntity(runLogDto.getId());
        runLog.setStatus(LoadLogStatus.SUCCESS);
        runLog.setLoadedRows(runLogDto.getLoadedRows());
        runLog.setMergedRows(runLogDto.getMergedRows());
        return LoadLogDto.fromEntity(loadLogRepository.save(runLog));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoadLogDto failRunLog(LoadLogDto runLogDto) {
        LoadLogEntity runLog = getRunLogEntity(runLogDto.getId());
        runLog.setStatus(LoadLogStatus.FAILED);
        runLog.setErrorText(runLogDto.getErrorText());
        return LoadLogDto.fromEntity(loadLogRepository.save(runLog));
    }
}
