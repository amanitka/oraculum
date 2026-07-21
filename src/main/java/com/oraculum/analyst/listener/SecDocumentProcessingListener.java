package com.oraculum.analyst.listener;

import com.oraculum.analyst.agent.document.service.SecDocumentProcessingAgent;
import com.oraculum.analyst.api.event.ProcessPendingSecDocumentsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecDocumentProcessingListener {

    private final SecDocumentProcessingAgent secDocumentProcessingAgent;

    @Async
    @EventListener
    public void onProcessPendingSecDocumentsEvent(ProcessPendingSecDocumentsEvent event) {
        log.info("Received event to process pending SEC documents (limit={}, maxPriority={})", event.limit(), event.maxPriority());
        try {
            secDocumentProcessingAgent.processPendingDocuments(event.limit());
        } catch (Exception e) {
            log.error("Failed to execute SEC document processing asynchronously: {}", e.getMessage(), e);
        }
    }
}
