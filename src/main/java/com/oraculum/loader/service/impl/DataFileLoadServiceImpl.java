package com.oraculum.loader.service.impl;

import com.oraculum.loader.message.DataFileReadyEvent;
import com.oraculum.loader.service.DataFileLoadService;
import com.oraculum.loader.service.ParquetFileLoadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileLoadServiceImpl implements DataFileLoadService {

    private final Map<String, ParquetFileLoadService> fileLoaders;

    @Override
    @Transactional
    public void processDataFileEvent(DataFileReadyEvent event) {
        log.info("Processing data file ready event: {}", event);
        Optional.ofNullable(fileLoaders.get(event.dataset())).ifPresentOrElse(loader -> {
            try {
                log.info("Found loader for dataset '{}'. Initiating merge process.", event.dataset());
                loader.merge(event.path());
                log.info("Successfully processed file for dataset '{}'.", event.dataset());
            } catch (Exception e) {
                log.error("Error processing file for dataset '{}'. Path: {}", event.dataset(), event.path(), e);
            }
        }, () -> log.warn("No ParquetFileLoader bean found for dataset: '{}'. Message will be ignored.",
                event.dataset()));
    }
}
