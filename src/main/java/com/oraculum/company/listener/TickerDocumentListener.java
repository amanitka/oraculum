package com.oraculum.company.listener;

import com.oraculum.company.api.event.TickerDocumentLoadEvent;
import com.oraculum.company.service.impl.TickerDocumentSyncStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickerDocumentListener {

    private final TickerDocumentSyncStatusService tickerDocumentSyncStatusService;

    @EventListener
    public void onDataFileLoaded(TickerDocumentLoadEvent event) {
        if (event.fileStatuses() == null || event.fileStatuses().isEmpty()) {
            return;
        }
        log.info("Processing ticker document load event for {} items", event.fileStatuses().size());
        tickerDocumentSyncStatusService.processDocumentLoadEvent(event);
    }

}
