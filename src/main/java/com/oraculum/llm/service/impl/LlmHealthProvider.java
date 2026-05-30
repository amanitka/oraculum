package com.oraculum.llm.service.impl;

import com.oraculum.llm.domain.LlmProviderType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LlmHealthProvider {
    private final Map<LlmProviderType, State> state = new ConcurrentHashMap<>();

    public boolean isBlocked(LlmProviderType provider) {
        State s = state.get(provider);
        return s != null && System.currentTimeMillis() < s.blockedUntil;
    }

    public void markFailure(LlmProviderType provider, long blockMs) {
        State s = state.computeIfAbsent(provider, _ -> new State());
        s.blockedUntil = System.currentTimeMillis() + blockMs;
    }

    public void markSuccess(LlmProviderType provider) {
        state.remove(provider);
    }

    private static class State {
        long blockedUntil;
    }
}
