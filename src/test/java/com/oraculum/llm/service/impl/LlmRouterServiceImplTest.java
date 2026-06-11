package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.domain.LlmProviderType;
import com.oraculum.llm.service.LlmExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmRouterServiceImplTest {

    @Mock
    private LlmExecutionService executionService;

    @Mock
    private LlmHealthProvider health;

    @Mock
    private ChatClient openaiClient;

    @Mock
    private ChatClient geminiClient;

    private LlmRouterServiceImpl routerService;

    @BeforeEach
    void setUp() {
        Map<LlmTierType, Map<LlmProviderType, String>> models = Map.of(LlmTierType.STANDARD,
                Map.of(LlmProviderType.OPENAI, "gpt-4o", LlmProviderType.GEMINI, "gemini-1.5-pro"));

        LlmProperties properties = new LlmProperties(new LlmProperties.Common(0.7,
                1000,
                List.of(LlmProviderType.OPENAI, LlmProviderType.GEMINI),
                60),
                new LlmProperties.Retry(3, 1000),
                Map.of(),
                models);

        Map<LlmProviderType, ChatClient> clients = Map.of(LlmProviderType.OPENAI,
                openaiClient,
                LlmProviderType.GEMINI,
                geminiClient);

        routerService = new LlmRouterServiceImpl(clients, executionService, health, properties);
    }

    @Test
    void testSuccessfulGenerationWithFirstProvider() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";
        when(health.isBlocked(LlmProviderType.OPENAI)).thenReturn(false);
        when(executionService.executeCall(any())).thenReturn(new LlmResponse<>(expectedResult, null));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmTierType.STANDARD, prompt, String.class);

        // Assert
        assertEquals(expectedResult, result.result());
        verify(health).markSuccess(LlmProviderType.OPENAI);
        verify(health, never()).markSuccess(LlmProviderType.GEMINI);
        verify(executionService).executeCall(argThat(req -> req != null && req.client() == openaiClient && req.model()
                .equals("gpt-4o") && req.prompt().equals(prompt)));
    }

    @Test
    void testFallbackToSecondProviderWhenFirstFails() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";

        when(health.isBlocked(LlmProviderType.OPENAI)).thenReturn(false);
        when(health.isBlocked(LlmProviderType.GEMINI)).thenReturn(false);

        // First provider fails
        when(executionService.executeCall(argThat(req -> req != null && req.client() == openaiClient))).thenThrow(new RuntimeException(
                "API Error"));

        // Second provider succeeds
        when(executionService.executeCall(argThat(req -> req != null && req.client() == geminiClient))).thenReturn(new LlmResponse<>(
                expectedResult,
                null));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmTierType.STANDARD, prompt, String.class);

        // Assert
        assertEquals(expectedResult, result.result());
        verify(health).markFailure(LlmProviderType.OPENAI, 30_000);
        verify(health).markSuccess(LlmProviderType.GEMINI);
    }

    @Test
    void testSkipBlockedProvider() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";

        // Openai is blocked
        when(health.isBlocked(LlmProviderType.OPENAI)).thenReturn(true);
        when(health.isBlocked(LlmProviderType.GEMINI)).thenReturn(false);

        when(executionService.executeCall(any())).thenReturn(new LlmResponse<>(expectedResult, null));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmTierType.STANDARD, prompt, String.class);

        // Assert
        assertEquals(expectedResult, result.result());
        verify(executionService, never()).executeCall(argThat(req -> req != null && req.client() == openaiClient));
        verify(executionService).executeCall(argThat(req -> req != null && req.client() == geminiClient));
    }

    @Test
    void testAllProvidersFailThrowsException() {
        // Arrange
        when(health.isBlocked(any())).thenReturn(false);
        when(executionService.executeCall(any())).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> routerService.executeCall(LlmTierType.STANDARD, "prompt", String.class));

        assertEquals("All providers failed", exception.getMessage());
        verify(health, times(1)).markFailure(LlmProviderType.OPENAI, 30_000);
        verify(health, times(1)).markFailure(LlmProviderType.GEMINI, 30_000);
    }

    @Test
    void testMissingTierConfigurationThrowsException() {
        // Arrange
        // Empty models map
        LlmProperties propertiesWithMissingTier = new LlmProperties(new LlmProperties.Common(0.7,
                1000,
                List.of(LlmProviderType.OPENAI),
                60), new LlmProperties.Retry(3, 1000), Map.of(), Map.of());
        routerService = new LlmRouterServiceImpl(Map.of(LlmProviderType.OPENAI, openaiClient),
                executionService,
                health,
                propertiesWithMissingTier);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> routerService.executeCall(LlmTierType.STANDARD, "prompt", String.class));

        assertTrue(exception.getMessage().contains("Configuration missing for tier: STANDARD"));
    }
}