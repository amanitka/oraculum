package com.oraculum.load.service;

import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;

public interface ParquetFileLoadService {
    /**
     * Merges the data from the staging table into the target table.
     *
     * @param event The original Kafka event containing metadata.
     */
    void merge(DataFileReadyEvent event);

    /**
     * Called after every successful {@link #merge} for per-part post-processing.
     * Default is a no-op — override only if needed.
     */
    default void postProcess(DataFileReadyEvent event) {
    }

    /**
     * Called once after all parts of a multi-part batch have been loaded,
     * triggered by a {@link DataBatchCompleteEvent}. Use this for operations
     * that must run exactly once per quarter (e.g. stored procedures, tier promotion).
     * Default is a no-op — override only for multi-part datasets.
     */
    default void postBatchComplete(DataBatchCompleteEvent event) {
    }
}

