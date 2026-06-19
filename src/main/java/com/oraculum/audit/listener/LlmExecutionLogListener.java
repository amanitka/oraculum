package com.oraculum.audit.listener;

import com.oraculum.audit.service.LlmExecutionLogService;
import com.oraculum.llm.api.LlmExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmExecutionLogListener {

    private final LlmExecutionLogService llmExecutionLogService;

    @Async
    @EventListener
    public void onLlmExecutionEvent(LlmExecutionEvent event) {
        try {
            llmExecutionLogService.createLog(event);
        } catch (Exception e) {
            log.error("Failed to log LLM execution asynchronously via JPA", e);
        }
    }
}
