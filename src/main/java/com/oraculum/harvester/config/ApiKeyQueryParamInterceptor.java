package com.oraculum.harvester.config;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

public class ApiKeyQueryParamInterceptor implements ClientHttpRequestInterceptor {

    private final String paramName;
    private final String apiKey;

    public ApiKeyQueryParamInterceptor(String paramName, String apiKey) {
        this.paramName = paramName;
        this.apiKey = apiKey;
    }

    @Override
    public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        URI newUri = UriComponentsBuilder.fromUri(request.getURI())
                .queryParam(paramName, apiKey)
                .build()
                .toUri();
        HttpRequest modifiedRequest = new HttpRequestWrapper(request) {
            @Override
            public @NonNull URI getURI() {
                return newUri;
            }
        };
        return execution.execute(modifiedRequest, body);
    }
}
