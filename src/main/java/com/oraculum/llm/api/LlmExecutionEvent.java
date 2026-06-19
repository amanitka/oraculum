package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmMetrics;
import java.util.UUID;

public record LlmExecutionEvent(
    UUID correlationId,
    CorrelationType correlationType,
    String source,
    LlmMetrics metrics,
    String prompt,
    Object responseResult
) {}
