package com.oraculum.load.service;

import com.oraculum.load.dto.DataFileReadyEvent;

public interface ParquetFileLoadService {
    /**
     * Merge records from a Parquet file natively into a target database table.
     *
     * @param event The original Kafka event containing metadata.
     */
    void merge(DataFileReadyEvent event);
}
