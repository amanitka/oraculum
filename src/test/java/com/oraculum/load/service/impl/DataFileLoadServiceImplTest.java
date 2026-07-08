package com.oraculum.load.service.impl;

import com.oraculum.audit.api.LoadLogApi;
import com.oraculum.audit.api.dto.LoadLogDto;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.service.ParquetFileLoadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataFileLoadServiceImplTest {

    @Mock
    private ParquetFileLoadService dummyLoader;

    @Mock
    private LoadLogApi loadLogApi;

    private DataFileLoadServiceImpl dataFileLoadService;

    @BeforeEach
    void setUp() {
        dataFileLoadService = new DataFileLoadServiceImpl(
                Map.of("test_dataset", dummyLoader),
                loadLogApi
        );
    }

    @Test
    void processDataFileEvent_whenAlreadyProcessed_skips() {
        DataFileReadyEvent event = new DataFileReadyEvent("event", "test_dataset", "file.parquet", "template", "variant", 1, "run1", "checksum", 100, null, java.time.ZonedDateTime.now());
        when(loadLogApi.isAlreadyProcessed("test_dataset", "run1", "checksum")).thenReturn(true);

        dataFileLoadService.processDataFileEvent(event);

        verify(dummyLoader, never()).merge(any());
        verify(loadLogApi, never()).startRunLog(anyString(), anyString(), anyString());
    }

    @Test
    void processDataFileEvent_whenNoLoaderFound_skips() {
        DataFileReadyEvent event = new DataFileReadyEvent("event", "unknown_dataset", "file.parquet", "template", "variant", 1, "run1", "checksum", 100, null, java.time.ZonedDateTime.now());
        when(loadLogApi.isAlreadyProcessed("unknown_dataset", "run1", "checksum")).thenReturn(false);

        dataFileLoadService.processDataFileEvent(event);

        verify(dummyLoader, never()).merge(any());
        verify(loadLogApi, never()).startRunLog(anyString(), anyString(), anyString());
    }

    @Test
    void processDataFileEvent_whenValid_loadsAndCompletesLog() {
        DataFileReadyEvent event = new DataFileReadyEvent("event", "test_dataset", "file.parquet", "template", "variant", 1, "run1", "checksum", 100, null, java.time.ZonedDateTime.now());
        LoadLogDto logDto = new LoadLogDto();

        when(loadLogApi.isAlreadyProcessed("test_dataset", "run1", "checksum")).thenReturn(false);
        when(loadLogApi.startRunLog("test_dataset", "run1", "checksum")).thenReturn(logDto);

        dataFileLoadService.processDataFileEvent(event);

        verify(dummyLoader).merge(any(DataFileReadyEvent.class));
        verify(loadLogApi).completeRunLog(logDto);
    }

    @Test
    void processDataFileEvent_whenMergeFails_failsLog() {
        DataFileReadyEvent event = new DataFileReadyEvent("event", "test_dataset", "file.parquet", "template", "variant", 1, "run1", "checksum", 100, null, java.time.ZonedDateTime.now());
        LoadLogDto logDto = new LoadLogDto();

        when(loadLogApi.isAlreadyProcessed("test_dataset", "run1", "checksum")).thenReturn(false);
        when(loadLogApi.startRunLog("test_dataset", "run1", "checksum")).thenReturn(logDto);
        doThrow(new RuntimeException("Merge failed")).when(dummyLoader).merge(any(DataFileReadyEvent.class));

        dataFileLoadService.processDataFileEvent(event);

        verify(loadLogApi).failRunLog(logDto);
        verify(loadLogApi, never()).completeRunLog(any());
    }
}
