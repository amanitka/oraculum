package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmRequestDto;
import com.oraculum.llm.api.dto.LlmResponseDto;
import com.oraculum.llm.api.dto.LlmResponseFormatDto;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.config.LlmProperties.Deployment;
import com.oraculum.llm.exception.StructuredOutputException;
import com.oraculum.llm.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmServiceImpl implements LlmService {

    private static final String INCOMPLETE_ERROR_TEXT = "incomplete structured response";
    private static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final LlmProperties properties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    public LlmServiceImpl(LlmProperties properties, JsonMapper jsonMapper) {
        this(properties, jsonMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    LlmServiceImpl(LlmProperties properties, JsonMapper jsonMapper, HttpClient httpClient) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.httpClient = httpClient;
    }

    @Override
    public LlmResponseDto complete(LlmRequestDto request) {
        List<Map<String, Object>> messages = request.messages();
        String model = request.model();
        int maxTokens = request.maxTokens();
        double temperature = request.temperature();
        LlmResponseFormatDto responseFormat = request.responseFormat();
        List<Deployment> deployments =
                properties.getDeployments().stream().filter(d -> model.equals(d.getAlias())).sorted(Comparator.comparingInt(Deployment::getOrder)).toList();

        if (deployments.isEmpty()) {
            throw new IllegalArgumentException("No deployments found for alias '" + model + "'");
        }

        log.info("Executing LLM request for tier '{}' with {} deployments.", model, deployments.size());

        Exception lastException = null;
        for (int i = 0; i < deployments.size(); i++) {
            Deployment deployment = deployments.get(i);
            log.info("Attempting deployment #{}: model='{}', base_url='{}'", i + 1, deployment.getModel(),
                    deployment.getApiBase());
            try {
                return tryCompleteWithDeployment(deployment, messages, maxTokens, temperature, responseFormat);
            } catch (Exception e) {
                log.warn("Deployment {} for tier {} failed. Trying next. Error: {}", deployment.getModel(), model,
                        e.getMessage());
                lastException = e;
            }
        }

        throw new IllegalStateException("All deployments for tier " + model + " failed.", lastException);
    }

    private LlmResponseDto tryCompleteWithDeployment(Deployment deployment, List<Map<String, Object>> messages,
                                                     int maxTokens, double temperature,
                                                     LlmResponseFormatDto responseFormat) {
        int maxRetries = Math.max(properties.getRouterSettings().getNumRetries(), 1);
        Duration backoff = DEFAULT_BACKOFF;
        int maxRetryTokens = properties.getRouterSettings().getMaxTokens();
        if (maxRetryTokens <= 0) {
            maxRetryTokens = maxTokens;
        }
        int currentMaxTokens = Math.min(maxTokens, maxRetryTokens);

        if (currentMaxTokens < maxTokens) {
            log.warn("Requested max_tokens {} exceeds guardrail {}; capping request.", maxTokens, maxRetryTokens);
        }

        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return executeRequest(deployment, messages, currentMaxTokens, temperature, responseFormat);
            } catch (TransientLlmException | IOException e) {
                attempt++;
                log.warn("LLM call failed with transient error ({}). Retrying in {}s... ({}/{})",
                        e.getClass().getSimpleName(), backoff.toSeconds(), attempt, maxRetries);
                if (attempt >= maxRetries) {
                    log.error("LLM call failed after multiple retries.");
                    throw new IllegalStateException("LLM call failed after multiple retries.", e);
                }
                sleep(backoff);
                backoff = backoff.multipliedBy(2);
            } catch (StructuredOutputException e) {
                attempt++;
                log.warn("LLM structured output invalid ({}). Retrying in {}s... ({}/{})", e.getMessage(),
                        backoff.toSeconds(), attempt, maxRetries);
                if (attempt >= maxRetries) {
                    log.error("LLM structured output failed after multiple retries.");
                    throw new IllegalStateException("LLM structured output failed after multiple retries.", e);
                }
                if (isIncompleteStructuredResponse(e) && currentMaxTokens < maxRetryTokens) {
                    int nextMaxTokens = Math.min(Math.max(currentMaxTokens * 2, currentMaxTokens + 1), maxRetryTokens);
                    log.warn("Increasing max_tokens for retry from {} to {} after truncation.", currentMaxTokens,
                            nextMaxTokens);
                    currentMaxTokens = nextMaxTokens;
                }
                sleep(backoff);
                backoff = backoff.multipliedBy(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("LLM call interrupted.", e);
            } catch (RuntimeException e) {
                log.error("An unexpected error occurred during the LLM call.", e);
                throw e;
            }
        }

        throw new IllegalStateException("LLM call failed after all retries.");
    }

    private LlmResponseDto executeRequest(Deployment deployment, List<Map<String, Object>> messages, int maxTokens,
                                          double temperature, LlmResponseFormatDto responseFormat) throws IOException
            , InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", deployment.getModel());
        payload.put("messages", messages);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        if (responseFormat != null) {
            payload.put("response_format", buildResponseFormat(responseFormat));
        }

        String requestBody = jsonMapper.writeValueAsString(payload);
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(resolveCompletionsUrl(deployment.getApiBase()))).timeout(REQUEST_TIMEOUT).header("Content-Type", "application/json").header("Authorization", "Bearer " + deployment.getApiKey()).POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

        long start = System.nanoTime();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long end = System.nanoTime();

        if (isTransientStatus(response.statusCode())) {
            throw new TransientLlmException("LLM call failed with status " + response.statusCode());
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("LLM call failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = jsonMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new IllegalStateException("LLM response missing choices.");
        }

        JsonNode choice = choices.get(0);
        String completionText = choice.path("message").path("content").asText("");
        String finishReason = readOptionalText(choice.path("finish_reason"));

        if (responseFormat instanceof LlmResponseFormatDto.JsonSchema jsonSchema) {
            validateStructuredResponse(completionText, finishReason, jsonSchema.schemaType());
        }

        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        String responseModel = readOptionalText(root.path("model"));
        if (!StringUtils.hasText(responseModel)) {
            responseModel = deployment.getModel();
        }

        return new LlmResponseDto(completionText, responseModel, promptTokens, completionTokens,
                Duration.ofNanos(end - start).toMillis(), finishReason);
    }

    private Object buildResponseFormat(LlmResponseFormatDto responseFormat) {
        if (responseFormat instanceof LlmResponseFormatDto.Raw rawFormat) {
            return rawFormat.raw();
        }
        return Map.of("type", "json_object");
    }

    private void validateStructuredResponse(String completionText, String finishReason, Class<?> schemaType) {
        try {
            jsonMapper.readValue(completionText, schemaType);
        } catch (Exception e) {
            if ("length".equals(finishReason) || "max_tokens".equals(finishReason)) {
                throw new StructuredOutputException("incomplete structured response (finish_reason=" + finishReason + ")", e);
            }
            throw new StructuredOutputException("structured response failed validation", e);
        }
    }

    private boolean isIncompleteStructuredResponse(StructuredOutputException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains(INCOMPLETE_ERROR_TEXT);
    }

    private String resolveCompletionsUrl(String apiBase) {
        String base = apiBase == null ? "" : apiBase.trim();
        if (base.endsWith("/")) {
            return base + "chat/completions";
        }
        return base + "/chat/completions";
    }

    private boolean isTransientStatus(int status) {
        return status == 429 || status >= 500;
    }

    private String readOptionalText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during LLM backoff.", e);
        }
    }

    private static class TransientLlmException extends RuntimeException {

        private TransientLlmException(String message) {
            super(message);
        }
    }
}
