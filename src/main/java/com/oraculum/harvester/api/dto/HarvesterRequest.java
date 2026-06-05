package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.UUID;

/**
 * Base class and nested subtypes for harvester refresh requests published to Kafka.
 * JSON fields use snake_case to match the Python Pydantic models on the harvester side.
 */
@Getter
public abstract class HarvesterRequest {

    @JsonProperty("correlation_id")
    private final UUID correlationId = UUID.randomUUID();

    @JsonProperty("issued_at")
    private final String issuedAt = java.time.Instant.now().toString();

    @JsonProperty("request_type")
    public abstract String getRequestType();
}
