package com.oraculum.load.service;

import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;

public interface DataFileLoadService {

    /**
     * Processes a single data file readiness event by finding the appropriate
     * loader strategy based on the dataset and executing the merge process.
     *
     * @param event The data file readiness event received from the message queue.
     */
    void processDataFileEvent(DataFileReadyEvent event);

    /**
     * Handles a batch completion signal, triggering any post-processing that must
     * run exactly once after all parts of a multi-part dataset have been loaded
     * (e.g. {@code sp_compute_sec_holding_delta}, Tier 1 CIK promotion).
     *
     * @param event The batch complete event received from the message queue.
     */
    void processBatchCompleteEvent(DataBatchCompleteEvent event);
}

