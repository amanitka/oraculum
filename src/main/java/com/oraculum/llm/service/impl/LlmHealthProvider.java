package com.oraculum.llm.service.impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LlmHealthProvider {
    private final Map<String, State> state = new ConcurrentHashMap<>();

    public boolean isBlocked(String provider) {
        State s = state.get(provider);
        return s != null && System.currentTimeMillis() < s.blockedUntil;
    }

    public void markFailure(String provider, long blockMs) {
        State s = state.computeIfAbsent(provider, _ -> new State());
        s.blockedUntil = System.currentTimeMillis() + blockMs;
    }

    public void markSuccess(String provider) {
        state.remove(provider);
    }

    private static class State {
        long blockedUntil;
    }
}
