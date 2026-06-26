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
    void testMarkFailureBlocksProvider() {
        // Block OPENAI for 10 seconds
        healthProvider.markFailure(LlmProviderType.OPENAI, 10_000);

        assertTrue(healthProvider.isBlocked(LlmProviderType.OPENAI));
        // GEMINI should remain unblocked
        assertFalse(healthProvider.isBlocked(LlmProviderType.GEMINI));
    }

    @Test
    void testBlockExpires() throws InterruptedException {
        // Block OPENAI for a very short duration (50ms)
        healthProvider.markFailure(LlmProviderType.OPENAI, 50);
        assertTrue(healthProvider.isBlocked(LlmProviderType.OPENAI));

        // Wait for block to expire
        Thread.sleep(60);

        assertFalse(healthProvider.isBlocked(LlmProviderType.OPENAI));
    }

    @Test
    void testMarkSuccessClearsBlock() {
        // Block OPENAI
        healthProvider.markFailure(LlmProviderType.OPENAI, 10_000);
        assertTrue(healthProvider.isBlocked(LlmProviderType.OPENAI));

        // Mark success
        healthProvider.markSuccess(LlmProviderType.OPENAI);

        assertFalse(healthProvider.isBlocked(LlmProviderType.OPENAI));
    }
}
