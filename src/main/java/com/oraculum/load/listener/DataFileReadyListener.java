package com.oraculum.load.listener;

import com.oraculum.load.message.DataFileReadyEvent;
import com.oraculum.load.service.DataFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileReadyListener {

    private final DataFileLoadService dataFileLoadService;

    @KafkaListener(topics = "${oraculum.kafka.topics.data-file-ready}", groupId = "${oraculum.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onDataFileReady(DataFileReadyEvent event) {
        log.info("Received data file ready event via Kafka: {}", event);
        dataFileLoadService.processDataFileEvent(event);
    }
}
