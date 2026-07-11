package com.oraculum.load.service;

import com.oraculum.load.dto.DataFileReadyEvent;

public interface ParquetFileLoadService {
    /**
     * Merges the data from the staging table into the target table.
     *
     * @param event The original Kafka event containing metadata.
     */
    void merge(DataFileReadyEvent event);

    default void postProcess(DataFileReadyEvent event) {
        // default empty implementation
    }
}
