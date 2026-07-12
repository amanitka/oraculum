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
        log.info("Starting load for dataset '{}' (correlationId={}, records={})", event.dataset(), event.correlationId(),
                event.recordCount());
        return loadLogApi.startRunLog(event.dataset(), event.correlationId(), event.fileChecksum());
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

    private void mergeEventData(DataFileReadyEvent event, ParquetFileLoadService loader) {
        if (event.recordCount() == 0) {
            log.info("Received empty dataset event. Skipping parquet load, updating sync statuses.");
        } else {
            loader.merge(event);
        }

        loader.postProcess(event);
    }

    private void processEventByLoader(DataFileReadyEvent event, ParquetFileLoadService loader) {
        LoadLogDto runLog = createRunLog(event);
        try {
            mergeEventData(event, loader);
            completeRunLog(event, runLog);
            log.info("Successfully loaded dataset '{}' (correlationId={}, rows={})", event.dataset(),
                    event.correlationId(), event.recordCount());
        } catch (Exception e) {
            failRunLog(e, runLog);
            log.error("Failed to load dataset '{}' (correlationId={})", event.dataset(), event.correlationId(), e);
        }
    }

    @Override
    public void processDataFileEvent(DataFileReadyEvent event) {
        if (loadLogApi.isAlreadyProcessed(event.dataset(), event.correlationId(), event.fileChecksum())) {
            log.info("Skipping already processed event: {} (correlationId={})", event.dataset(), event.correlationId());
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
