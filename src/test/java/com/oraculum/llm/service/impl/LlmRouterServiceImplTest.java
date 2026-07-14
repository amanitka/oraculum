package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.service.LlmExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LlmRouterServiceImpl routerService;

    @BeforeEach
    void setUp() {
        Map<LlmTierType, Map<LlmProviderType, String>> models = Map.of(LlmTierType.STANDARD,
                Map.of(LlmProviderType.OPENAI, "gpt-4o", LlmProviderType.GEMINI, "gemini-1.5-pro"));

        LlmProperties properties = new LlmProperties(new LlmProperties.Common(0.7,
                1000,
                List.of(LlmProviderType.OPENAI, LlmProviderType.GEMINI),
                60, 3, 1000L),
                Map.of(),
                models);

        Map<LlmProviderType, ChatClient> clients = Map.of(LlmProviderType.OPENAI,
                openaiClient,
                LlmProviderType.GEMINI,
                geminiClient);

        routerService = new LlmRouterServiceImpl(clients, executionService, health, properties, eventPublisher);
    }

    @Test
    void testSuccessfulGenerationWithFirstProvider() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";
        when(health.isBlocked(LlmProviderType.OPENAI)).thenReturn(false);
        when(executionService.executeCall(any())).thenReturn(CompletableFuture.completedFuture(new LlmResponse<>(expectedResult, null)));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, prompt, String.class));

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

        when(health.isBlocked(any())).thenReturn(false);

        // First provider fails
        when(executionService.executeCall(argThat(req -> req != null && req.client() == openaiClient))).thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // Second provider succeeds
        when(executionService.executeCall(argThat(req -> req != null && req.client() == geminiClient))).thenReturn(CompletableFuture.completedFuture(new LlmResponse<>(
                expectedResult,
                null)));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, prompt, String.class));

        // Assert
        assertEquals(expectedResult, result.result());
        verify(health).markFailure(LlmProviderType.OPENAI);
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

        when(executionService.executeCall(any())).thenReturn(CompletableFuture.completedFuture(new LlmResponse<>(expectedResult, null)));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, prompt, String.class));

        // Assert
        assertEquals(expectedResult, result.result());
        verify(executionService, never()).executeCall(argThat(req -> req != null && req.client() == openaiClient));
        verify(executionService).executeCall(argThat(req -> req != null && req.client() == geminiClient));
    }

    @Test
    void testAllProvidersFailThrowsException() {
        // Arrange
        when(health.isBlocked(any())).thenReturn(false);
        when(executionService.executeCall(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, "prompt", String.class)));

        assertEquals("All providers failed", exception.getMessage());
        verify(health, times(1)).markFailure(LlmProviderType.OPENAI);
        verify(health, times(1)).markFailure(LlmProviderType.GEMINI);
    }

    @Test
    void testMissingTierConfigurationThrowsException() {
        // Arrange
        // Empty models map
        LlmProperties propertiesWithMissingTier = new LlmProperties(new LlmProperties.Common(0.7,
                1000,
                List.of(LlmProviderType.OPENAI),
                60, 3, 1000L), Map.of(), Map.of());
        routerService = new LlmRouterServiceImpl(Map.of(LlmProviderType.OPENAI, openaiClient),
                executionService,
                health,
                propertiesWithMissingTier,
                eventPublisher);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, "prompt", String.class)));

        assertTrue(exception.getMessage().contains("Configuration missing for tier: STANDARD"));
    }

    @Test
    void testFallbackOverrideRespectsRequestedOrder() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";

        when(health.isBlocked(any())).thenReturn(false);
        when(executionService.executeCall(any())).thenReturn(CompletableFuture.completedFuture(new LlmResponse<>(expectedResult, null)));

        LlmCallRequest<String> request = LlmCallRequest.withFallbackOverride(
                LlmTierType.STANDARD, prompt, String.class,
                null, null, null,
                List.of(LlmProviderType.GEMINI, LlmProviderType.OPENAI)
        );

        // Act
        LlmResponse<String> result = routerService.executeCall(request);

        // Assert
        assertEquals(expectedResult, result.result());
        verify(executionService).executeCall(argThat(req -> req != null && req.client() == geminiClient));
        verify(executionService, never()).executeCall(argThat(req -> req != null && req.client() == openaiClient));
    }

    @Test
    void testProviderSkippedOnNullModel() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResult = "Test response";

        // Setup properties where OPENAI model is null, but GEMINI model is set
        Map<LlmTierType, Map<LlmProviderType, String>> models = Map.of(LlmTierType.STANDARD,
                new java.util.HashMap<>() {{
                    put(LlmProviderType.OPENAI, null); // Skip this
                    put(LlmProviderType.GEMINI, "gemini-1.5-pro");
                }});

        LlmProperties propertiesWithNullModel = new LlmProperties(new LlmProperties.Common(0.7,
                1000,
                List.of(LlmProviderType.OPENAI, LlmProviderType.GEMINI),
                60, 3, 1000L), Map.of(), models);

        routerService = new LlmRouterServiceImpl(
                Map.of(LlmProviderType.OPENAI, openaiClient, LlmProviderType.GEMINI, geminiClient),
                executionService,
                health,
                propertiesWithNullModel,
                eventPublisher
        );

        when(health.isBlocked(any())).thenReturn(false);
        when(executionService.executeCall(any())).thenReturn(CompletableFuture.completedFuture(new LlmResponse<>(expectedResult, null)));

        // Act
        LlmResponse<String> result = routerService.executeCall(LlmCallRequest.of(LlmTierType.STANDARD, prompt, String.class));

        // Assert
        assertEquals(expectedResult, result.result());
        verify(executionService, never()).executeCall(argThat(req -> req != null && req.client() == openaiClient));
        verify(executionService).executeCall(argThat(req -> req != null && req.client() == geminiClient));
    }

    @Test
    void testSingleProviderBypassesHealthCheck() {
        // Arrange
        String prompt = "Test prompt";

        LlmCallRequest<String> request = LlmCallRequest.withFallbackOverride(
                LlmTierType.STANDARD, prompt, String.class,
                null, null, null,
                List.of(LlmProviderType.OPENAI) // Single provider
        );

        when(executionService.executeCall(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> routerService.executeCall(request));

        assertEquals("All providers failed", exception.getMessage());
        verify(health, never()).isBlocked(any());
        verify(health, never()).markFailure(any());
        verify(health, never()).markSuccess(any());
    }
}
