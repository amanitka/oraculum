package com.oraculum.llm;

import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmTierType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that makes a REAL call to an LLM provider.
 * To run this test, you MUST set at least one of the following environment variables:
 * - GROQ_API_KEY
 * - GEMINI_API_KEY
 * For example:
 * GROQ_API_KEY=gsk-... ./mvnw test -Dtest=LlmIntegrationTest
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariables({@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".*"),
        @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".*")})
public class LlmIntegrationTest {

    @Autowired
    private LlmRouterApi llmRouterApi;

    @Test
    void shouldMakeRealCallToLlm() {
        // Arrange
        String prompt = "Say exactly 'hi' and nothing else.";

        // Act
        // This makes a real network call!
        String response = llmRouterApi.generate(LlmTierType.MINI, prompt, String.class);

        // Assert
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("hi"));
        System.out.println("LLM Response: " + response);
    }
}