package com.oraculum.loader.listener;

import com.oraculum.loader.message.DataFileReadyEvent;
import com.oraculum.loader.service.DataFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFileReadyListener {

    private final DataFileLoadService dataFileLoadService;

    @KafkaListener(topics = "${oraculum.kafka.topics.dataFileReady}", groupId = "${oraculum.kafka.consumerGroup}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onDataFileReady(DataFileReadyEvent event) {
        log.info("Received data file ready event via Kafka: {}", event);
        dataFileLoadService.processDataFileEvent(event);
    }
}
