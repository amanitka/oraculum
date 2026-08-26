package com.oraculum.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Signal that all {@link DataFileReadyEvent} parts for a given {@code correlationId}
 * have been published. Contains no file data — its sole purpose is to tell the
 * consumer that post-processing (e.g. stored procedures, Tier 1 promotion) is safe to run.
 *
 * <p>Published on the same topic and with the same Kafka message key as the data events,
 * guaranteeing it is processed strictly after all parts in the same partition.
 *
 * @param dataset         The dataset this batch belongs to (e.g. {@code "sec_13f_holding"}).
 * @param correlationId   Matches the {@code correlationId} of all preceding data parts.
 * @param totalParts      Total number of {@link DataFileReadyEvent}s published in this batch.
 * @param periodOfReport  Quarter end date — passed directly to post-processing procedures.
 * @param createdAt       Timestamp when the event was created.
 */
public record DataBatchCompleteEvent(String dataset,
                                     @JsonProperty("correlation_id")
                                     String correlationId,
                                     @JsonProperty("total_parts")
                                     int totalParts,
                                     @JsonProperty("period_of_report")
                                     LocalDate periodOfReport,
                                     @JsonProperty("created_at")
                                     ZonedDateTime createdAt)
        implements HarvesterOutputEvent {
}

