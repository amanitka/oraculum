package com.oraculum.llm.service.impl;

import com.oraculum.llm.domain.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmExecutionServiceImplTest {

    private LlmExecutionServiceImpl executionService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        executionService = new LlmExecutionServiceImpl();
    }

    @Test
    void testExecuteCall() {
        // Arrange
        String prompt = "Hello AI";
        String model = "gpt-4o";
        double temperature = 0.7;
        String expectedResponse = "Mocked Response";

        LlmRequest<String> request = new LlmRequest<>(chatClient, prompt, model, temperature, String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(prompt)).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(String.class)).thenReturn(expectedResponse);

        // Act
        String result = executionService.executeCall(request);

        // Assert
        assertEquals(expectedResponse, result);

        verify(chatClient).prompt();
        verify(requestSpec).user(prompt);
        verify(requestSpec).options(argThat(opts -> {
            if (opts instanceof OpenAiChatOptions.Builder builder) {
                OpenAiChatOptions options = builder.build();
                return model.equals(options.getModel()) && Double.valueOf(temperature).equals(options.getTemperature());
            }
            return false;
        }));
        verify(requestSpec).call();
        verify(responseSpec).entity(String.class);
    }
}
