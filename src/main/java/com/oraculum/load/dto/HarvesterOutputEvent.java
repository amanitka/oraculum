package com.oraculum.load.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed marker interface for all events published by the Python harvester
 * to the {@code data-file-ready} Kafka topic.
 *
 * <p>Jackson uses the {@code event_type} field to polymorphically deserialize
 * the raw JSON string into the correct concrete record type. The
 * {@link org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter}
 * in {@code KafkaConsumerConfig} drives this via the listener method's parameter type.
 *
 * <p>To add a new event type:
 * <ol>
 *   <li>Create a new record implementing this interface.</li>
 *   <li>Add a {@link JsonSubTypes.Type} entry below.</li>
 *   <li>Add a {@code case} arm in {@link com.oraculum.load.listener.DataFileReadyListener}.</li>
 * </ol>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataFileReadyEvent.class, name = "oraculum.data_file_ready"),
        @JsonSubTypes.Type(value = DataBatchCompleteEvent.class, name = "oraculum.data_batch_complete")
})
public sealed interface HarvesterOutputEvent
        permits DataFileReadyEvent, DataBatchCompleteEvent {
}
