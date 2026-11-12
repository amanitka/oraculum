package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.domain.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        String prompt = "Hello AI";
        String model = "gpt-4o";
        double temperature = 0.7;
        int maxCompletionTokens = 1000;
        String expectedResponse = "Mocked Response";

        LlmRequest<String> request = new LlmRequest<>(chatClient,
                prompt,
                LlmProviderType.OPENAI,
                model,
                temperature,
                maxCompletionTokens,
                String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(prompt)).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.responseEntity(String.class)).thenReturn(new ResponseEntity<>(null, expectedResponse));

        LlmResponse<String> result = executionService.executeCall(request);

        assertEquals(expectedResponse, result.result());

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
        verify(responseSpec).responseEntity(String.class);
    }

    @Test
    void propagatesExceptionFromClientCall() {
        LlmRequest<String> request = new LlmRequest<>(chatClient, "Hello AI", LlmProviderType.LMSTUDIO,
                "gemma-4-12b", 0.0, 1000, String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("Hello AI")).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.responseEntity(String.class)).thenThrow(new IllegalStateException("Connection refused"));

        assertThatThrownBy(() -> executionService.executeCall(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Connection refused");
    }
}
