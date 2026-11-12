package com.oraculum.llm.config;

import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties.Common;
import com.oraculum.llm.config.LlmProperties.ProviderConfig;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LlmClientConfigTest {

    private final LlmClientConfig config = new LlmClientConfig();

    @Test
    void defaultsToUnlimitedRequestTimeoutAndZeroSdkRetries() {
        LlmProperties properties = createProperties(
                new Common(0.0, 1000, List.of(LlmProviderType.LMSTUDIO), null, null, null, null),
                Map.of(LlmProviderType.LMSTUDIO, new ProviderConfig("http://localhost:1234/v1", "key", null, null, null))
        );

        OpenAiChatModel model = config.buildChatModel(properties, LlmProviderType.LMSTUDIO);

        assertThat(model.getOptions().getTimeout()).isEqualTo(Duration.ZERO);
        assertThat(model.getOptions().getMaxRetries()).isZero();
    }

    @Test
    void providerOverridesTakePrecedenceOverCommonSettings() {
        LlmProperties properties = createProperties(
                new Common(0.2, 1000, List.of(LlmProviderType.LMSTUDIO), 120, 5, 2, null),
                Map.of(LlmProviderType.LMSTUDIO, new ProviderConfig("http://localhost:1234/v1", "key", 3, 600, 1))
        );

        OpenAiChatModel model = config.buildChatModel(properties, LlmProviderType.LMSTUDIO);

        assertThat(model.getOptions().getTimeout()).isEqualTo(Duration.ofSeconds(600));
        assertThat(model.getOptions().getMaxRetries()).isEqualTo(1);
    }

    @Test
    void nonPositiveTimeoutConfigMapsToUnlimitedDurationZero() {
        LlmProperties properties = createProperties(
                new Common(0.0, 1000, List.of(LlmProviderType.OPENAI), 0, null, null, null),
                Map.of(LlmProviderType.OPENAI, new ProviderConfig("https://api.openai.com/v1", "key", null, -1, null))
        );

        OpenAiChatModel model = config.buildChatModel(properties, LlmProviderType.OPENAI);

        assertThat(model.getOptions().getTimeout()).isEqualTo(Duration.ZERO);
    }

    @Test
    void throwsWhenProviderCredentialsAreMissing() {
        LlmProperties properties = createProperties(
                new Common(0.0, 1000, List.of(LlmProviderType.LMSTUDIO), null, null, null, null),
                Map.of()
        );

        assertThatThrownBy(() -> config.buildChatModel(properties, LlmProviderType.LMSTUDIO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LMSTUDIO");
    }

    @Test
    void chatClientsBuildsClientForEachConfiguredProvider() {
        LlmProperties properties = createProperties(
                new Common(0.0, 1000, List.of(LlmProviderType.LMSTUDIO, LlmProviderType.DEEPSEEK), null, 5, 0, null),
                Map.of(
                        LlmProviderType.LMSTUDIO, new ProviderConfig("http://localhost:1234/v1", "lm", 3, null, null),
                        LlmProviderType.DEEPSEEK, new ProviderConfig("https://api.deepseek.com", "ds", null, null, null)
                )
        );

        Map<LlmProviderType, ChatClient> clients = config.chatClients(properties);

        assertThat(clients).containsOnlyKeys(LlmProviderType.LMSTUDIO, LlmProviderType.DEEPSEEK);
    }

    @Test
    void connectTimeoutCustomizerAppliesConfiguredConnectTimeoutPerCall() throws IOException {
        SpringAiOpenAiHttpClient.Builder builder = SpringAiOpenAiHttpClient.builder();
        config.connectTimeoutCustomizer(Duration.ofSeconds(3)).customize(builder);
        var okClient = builder.build().getOkHttpClient();
        Interceptor interceptor = okClient.interceptors().getLast();

        Interceptor.Chain initialChain = mock(Interceptor.Chain.class);
        Interceptor.Chain updatedChain = mock(Interceptor.Chain.class);
        Request request = new Request.Builder().url("http://localhost:1234/v1/chat/completions").build();
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", null))
                .build();

        when(initialChain.withConnectTimeout(3000, TimeUnit.MILLISECONDS)).thenReturn(updatedChain);
        when(updatedChain.request()).thenReturn(request);
        when(updatedChain.proceed(request)).thenReturn(response);

        Response actual = interceptor.intercept(initialChain);

        assertThat(actual).isSameAs(response);
        verify(initialChain).withConnectTimeout(3000, TimeUnit.MILLISECONDS);
        verify(updatedChain).proceed(request);
    }

    @Test
    void openAiClientAppliesConnectTimeoutWithoutLimitingReadOrWriteTimeouts() {
        String body = """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "test-model",
                  "choices": [{
                    "index": 0,
                    "message": {"role": "assistant", "content": "ok"},
                    "finish_reason": "stop"
                  }],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 1, "total_tokens": 6}
                }
                """;
        AtomicInteger observedConnectTimeout = new AtomicInteger(-1);
        AtomicInteger observedReadTimeout = new AtomicInteger(-1);
        AtomicInteger observedWriteTimeout = new AtomicInteger(-1);

        LlmProperties properties = createProperties(
                new Common(0.0, 100, List.of(LlmProviderType.LMSTUDIO), null, 3, 0, null),
                Map.of(LlmProviderType.LMSTUDIO, new ProviderConfig("http://127.0.0.1:1234/v1", "test-key", 3, null, 0))
        );
        OpenAiChatModel configured = config.buildChatModel(properties, LlmProviderType.LMSTUDIO);

        OpenAiChatModel testModel = OpenAiChatModel.builder()
                .options(configured.getOptions())
                .httpClientBuilderCustomizer(config.connectTimeoutCustomizer(Duration.ofSeconds(3)))
                .httpClientBuilderCustomizer(builder -> builder.interceptor(chain -> {
                    observedConnectTimeout.set(chain.connectTimeoutMillis());
                    observedReadTimeout.set(chain.readTimeoutMillis());
                    observedWriteTimeout.set(chain.writeTimeoutMillis());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(body, MediaType.get("application/json")))
                            .build();
                }))
                .build();

        var response = testModel.call(new Prompt("ping"));

        assertThat(Objects.requireNonNull(response.getResult()).getOutput().getText()).isEqualTo("ok");
        assertThat(observedConnectTimeout.get()).isEqualTo(3000);
        assertThat(observedReadTimeout.get()).isZero();
        assertThat(observedWriteTimeout.get()).isZero();
    }

    private LlmProperties createProperties(Common common, Map<LlmProviderType, ProviderConfig> providers) {
        return new LlmProperties(
                common,
                providers,
                Map.of(LlmTierType.MINI, Map.of(LlmProviderType.LMSTUDIO, "test-model"))
        );
    }
}
