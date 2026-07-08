package com.oraculum.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.load.api.dto.DataFileStatus;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents a message indicating that a new data file is ready for processing.
 * This record is used for deserializing JSON messages from Kafka.
 *
 * @param eventType     The type of the event (e.g., "simfin.data_file_ready").
 * @param dataset       The name of the dataset the file belongs to (e.g., "share_price"). This is used as a key to
 *                      find the correct loader.
 * @param path          The absolute path to the data file.
 * @param template      The template of the dataset, if applicable (e.g., "insurance").
 * @param variant       The variant of the dataset, if applicable (e.g., "ttm").
 * @param schemaVersion The version of the data schema.
 * @param runId         The unique identifier for the run that generated the file.
 * @param fileChecksum  The checksum of the file for integrity verification.
 * @param recordCount   The number of records in the file.
 * @param fileStatuses  The status updates for the requested extraction items.
 * @param createdAt     The timestamp when the event was created.
 */
public record DataFileReadyEvent(@JsonProperty("event_type") String eventType, String dataset, String path,
                                 String template, String variant, @JsonProperty("schema_version") int schemaVersion,
                                 @JsonProperty("run_id") String runId,
                                 @JsonProperty("file_checksum") String fileChecksum,
                                 @JsonProperty("record_count") int recordCount,
                                 @JsonProperty("file_statuses") List<DataFileStatus> fileStatuses,
                                 @JsonProperty("created_at") ZonedDateTime createdAt) {
    public DataFileReadyEvent {
        if (fileStatuses == null) {
            fileStatuses = List.of();
        }
    }
}
