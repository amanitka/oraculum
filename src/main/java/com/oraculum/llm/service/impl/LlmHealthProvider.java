package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LlmHealthProvider {

    private static final long INITIAL_BLOCK_MS = 30_000;       // 30 seconds
    private static final long MAX_BLOCK_MS = 30 * 60 * 1000;   // 30 minutes cap
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final Map<LlmProviderType, State> state = new ConcurrentHashMap<>();

    public boolean isBlocked(LlmProviderType provider) {
        State s = state.get(provider);
        return s != null && System.currentTimeMillis() < s.blockedUntil.get();
    }

    public void markFailure(LlmProviderType provider) {
        State s = state.computeIfAbsent(provider, ignored -> new State());
        int failures = s.consecutiveFailures.incrementAndGet();
        long blockMs = Math.min(
                (long) (INITIAL_BLOCK_MS * Math.pow(BACKOFF_MULTIPLIER, failures - 1)),
                MAX_BLOCK_MS);
        long newBlockedUntil = System.currentTimeMillis() + blockMs;
        s.blockedUntil.accumulateAndGet(newBlockedUntil, Math::max);
        log.warn("Provider {} failed (attempt {}). Blocked for {}s.",
                provider, failures, blockMs / 1000);
    }

    public void markSuccess(LlmProviderType provider) {
        state.remove(provider);  // Full reset on success
    }

    private static class State {
        final AtomicLong blockedUntil = new AtomicLong(0);
        final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    }
}
