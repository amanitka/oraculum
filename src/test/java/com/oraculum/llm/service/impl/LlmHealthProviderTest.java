package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmHealthProviderTest {

    private LlmHealthProvider healthProvider;

    @BeforeEach
    void setUp() {
        healthProvider = new LlmHealthProvider();
    }

    @Test
    void testInitiallyNotBlocked() {
        assertFalse(healthProvider.isBlocked(LlmProviderType.OPENAI));
        assertFalse(healthProvider.isBlocked(LlmProviderType.GEMINI));
    }

    @Test
    void testExponentialBackoffProgression() {
        // First failure: should block the provider
        healthProvider.markFailure(LlmProviderType.OPENAI);
        assertTrue(healthProvider.isBlocked(LlmProviderType.OPENAI));
        assertFalse(healthProvider.isBlocked(LlmProviderType.GEMINI));

        // Success clears the failure state
        healthProvider.markSuccess(LlmProviderType.OPENAI);
        assertFalse(healthProvider.isBlocked(LlmProviderType.OPENAI));
    }

    @Test
    void testMarkSuccessClearsBlock() {
        healthProvider.markFailure(LlmProviderType.OPENAI);
        assertTrue(healthProvider.isBlocked(LlmProviderType.OPENAI));

        healthProvider.markSuccess(LlmProviderType.OPENAI);
        assertFalse(healthProvider.isBlocked(LlmProviderType.OPENAI));
    }
}
