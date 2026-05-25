package com.oraculum.loader.service.impl;

import com.oraculum.audit.api.IngestionRunLogApi;
import com.oraculum.audit.api.dto.IngestionRunLogDto;
import com.oraculum.loader.message.DataFileReadyEvent;
import com.oraculum.loader.service.DataFileLoadService;
import com.oraculum.loader.service.ParquetFileLoadService;
import com.oraculum.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileLoadServiceImpl implements DataFileLoadService {

    private final Map<String, ParquetFileLoadService> fileLoaders;
    private final IngestionRunLogApi ingestionRunLogApi;

    private IngestionRunLogDto createRunLog(DataFileReadyEvent event) {
        log.info("Starting load for dataset '{}' (run_id={}, records={})", event.dataset(), event.runId(),
                event.recordCount());
        return ingestionRunLogApi.startRunLog(event.dataset(), event.runId(), event.fileChecksum());
    }

    private void completeRunLog(DataFileReadyEvent event, IngestionRunLogDto runLog, int durationMs) {
        runLog.setLoadedRows(event.recordCount());
        runLog.setMergedRows(event.recordCount());
        runLog.setDurationMs(durationMs);
        ingestionRunLogApi.completeRunLog(runLog);
    }

    private void failRunLog(Exception e, IngestionRunLogDto runLog, int durationMs) {
        runLog.setErrorText(e.getMessage());
        runLog.setDurationMs(durationMs);
        ingestionRunLogApi.failRunLog(runLog);
    }

    private void processEventByLoader(DataFileReadyEvent event, ParquetFileLoadService loader) {
        IngestionRunLogDto runLog = createRunLog(event);
        long startTime = System.nanoTime();
        try {
            loader.merge(event.path());
            int durationMs = TimeUtil.getDurationMs(startTime);
            completeRunLog(event, runLog, durationMs);
            log.info("Successfully loaded dataset '{}' (run_id={}, rows={}, duration={}ms)", event.dataset(),
                    event.runId(), event.recordCount(), durationMs);
        } catch (Exception e) {
            int durationMs = TimeUtil.getDurationMs(startTime);
            failRunLog(e, runLog, durationMs);
            log.error("Failed to load dataset '{}' (run_id={}, duration={}ms)", event.dataset(), event.runId(),
                    durationMs, e);
        }
    }

    @Override
    public void processDataFileEvent(DataFileReadyEvent event) {
        if (ingestionRunLogApi.isAlreadyProcessed(event.dataset(), event.runId(), event.fileChecksum())) {
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
