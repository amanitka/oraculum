package com.oraculum.load.listener;

import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.HarvesterOutputEvent;
import com.oraculum.load.service.DataFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener for all events published by the Python harvester on the
 * {@code data-file-ready} topic.
 *
 * <p>{@link HarvesterOutputEvent} is a sealed interface annotated with
 * {@code @JsonTypeInfo}. Jackson reads the {@code event_type} field from the
 * raw JSON string and polymorphically deserializes it into the correct record type.
 * The switch expression then dispatches to the appropriate handler with no casting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileReadyListener {

    private final DataFileLoadService dataFileLoadService;

    @KafkaListener(topics = "${oraculum.kafka.topics.data-file-ready}",
            groupId = "${oraculum.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(HarvesterOutputEvent event) {
        log.info("Received harvester event: type={}", event.getClass().getSimpleName());
        switch (event) {
            case DataFileReadyEvent e    -> dataFileLoadService.processDataFileEvent(e);
            case DataBatchCompleteEvent e -> dataFileLoadService.processBatchCompleteEvent(e);
        }
    }
}
