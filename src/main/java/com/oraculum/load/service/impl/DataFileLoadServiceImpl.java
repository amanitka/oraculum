package com.oraculum.load.service.impl;

import com.oraculum.audit.api.LoadLogApi;
import com.oraculum.audit.api.dto.LoadLogDto;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.service.DataFileLoadService;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileLoadServiceImpl implements DataFileLoadService {

    private final Map<String, ParquetFileLoadService> fileLoaders;
    private final LoadLogApi loadLogApi;

    private LoadLogDto createRunLog(DataFileReadyEvent event) {
        log.info("Starting load for dataset '{}' (run_id={}, records={})", event.dataset(), event.runId(),
                event.recordCount());
        return loadLogApi.startRunLog(event.dataset(), event.runId(), event.fileChecksum());
    }

    private void completeRunLog(DataFileReadyEvent event, LoadLogDto runLog) {
        runLog.setLoadedRows(event.recordCount());
        runLog.setMergedRows(event.recordCount());
        loadLogApi.completeRunLog(runLog);
    }

    private void failRunLog(Exception e, LoadLogDto runLog) {
        runLog.setErrorText(e.getMessage());
        loadLogApi.failRunLog(runLog);
    }

    private void processEventByLoader(DataFileReadyEvent event, ParquetFileLoadService loader) {
        LoadLogDto runLog = createRunLog(event);
        try {
            loader.merge(event);
            completeRunLog(event, runLog);
            log.info("Successfully loaded dataset '{}' (run_id={}, rows={})", event.dataset(),
                    event.runId(), event.recordCount());
        } catch (Exception e) {
            failRunLog(e, runLog);
            log.error("Failed to load dataset '{}' (run_id={})", event.dataset(), event.runId(), e);
        }
    }

    @Override
    public void processDataFileEvent(DataFileReadyEvent event) {
        if (loadLogApi.isAlreadyProcessed(event.dataset(), event.runId(), event.fileChecksum())) {
            log.info("Skipping already processed event: {} (run_id={})", event.dataset(), event.runId());
            return;
        }
        ParquetFileLoadService loader = fileLoaders.get(event.dataset());
        if (loader == null) {
            log.warn("No loader found for dataset '{}'. Message will be ignored.", event.dataset());
            return;
        }
        processEventByLoader(event, loader);
    }
}
