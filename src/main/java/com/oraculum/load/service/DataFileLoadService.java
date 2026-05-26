package com.oraculum.load.service;

import com.oraculum.load.message.DataFileReadyEvent;

public interface DataFileLoadService {

    /**
     * Processes a single data file readiness event.
     * <p>
     * This method is responsible for finding the appropriate loader strategy based on the
     * event's dataset and executing the merge process. It also handles logging,
     * error management, and any other cross-cutting concerns related to the event processing.
     *
     * @param event The data file readiness event received from the message queue.
     */
    void processDataFileEvent(DataFileReadyEvent event);
}
