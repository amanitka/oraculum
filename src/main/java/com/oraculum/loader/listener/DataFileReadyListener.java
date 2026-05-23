package com.oraculum.loader.listener;

import com.oraculum.loader.message.DataFileReadyEvent;
import com.oraculum.loader.service.ParquetFileLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileReadyListener {

    private final Map<String, ParquetFileLoader> fileLoaders;

    @KafkaListener(topics = "${oraculum.kafka.topics.dataFileReady}", groupId = "${oraculum.kafka.consumerGroup}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onDataFileReady(DataFileReadyEvent event) {
        log.info("Received data file ready event: {}", event);
        // Strategy Pattern: Use the 'dataset' field to find the correct loader bean.
        Optional.ofNullable(fileLoaders.get(event.dataset())).ifPresentOrElse(loader -> {
            try {
                log.info("Found loader for dataset '{}'. Initiating merge process.", event.dataset());
                loader.merge(event.path());
                log.info("Successfully processed file for dataset '{}'.", event.dataset());
            } catch (Exception e) {
                log.error("Error processing file for dataset '{}'. Path: {}", event.dataset(), event.path(), e);
                // Depending on requirements, you might want to send this to a dead-letter queue.
            }
        }, () -> log.warn("No ParquetFileLoader bean found for dataset: '{}'. Message will be ignored.",
                event.dataset()));
    }
}
