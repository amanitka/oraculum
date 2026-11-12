package com.oraculum.llm.config;

import com.openai.core.http.Headers;
import com.openai.errors.BadRequestException;
import com.openai.errors.InternalServerException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.errors.UnprocessableEntityException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class LlmRetryExceptionPredicateTest {

    private final LlmRetryExceptionPredicate predicate = new LlmRetryExceptionPredicate();
    private final Headers emptyHeaders = Headers.builder().build();

    @Test
    void returnsFalseForNull() {
        assertThat(predicate.test(null)).isFalse();
    }

    @Test
    void doesNotRetryConnectionEstablishmentFailures() {
        assertThat(predicate.test(new OpenAIIoException("Request failed", new ConnectException("Connection refused")))).isFalse();
        assertThat(predicate.test(new OpenAIIoException("Request failed", new NoRouteToHostException("No route")))).isFalse();
        assertThat(predicate.test(new OpenAIIoException("Request failed", new UnknownHostException("unknown")))).isFalse();
        assertThat(predicate.test(new OpenAIIoException("Request failed", new SocketTimeoutException("Connect timed out")))).isFalse();
        assertThat(predicate.test(new CompletionException(new OpenAIIoException("Request failed", new ConnectException("Failed"))))).isFalse();
    }

    @Test
    void doesNotRetrySuppressedConnectionFailure() {
        IOException wrapper = new IOException("Wrapper");
        wrapper.addSuppressed(new ConnectException("Connection refused"));

        assertThat(predicate.test(new OpenAIIoException("Request failed", wrapper))).isFalse();
    }

    @Test
    void doesNotRetryClientOrDataErrors() {
        assertThat(predicate.test(RateLimitException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(BadRequestException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(UnauthorizedException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(PermissionDeniedException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(NotFoundException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(UnprocessableEntityException.builder().headers(emptyHeaders).build())).isFalse();
        assertThat(predicate.test(new OpenAIInvalidDataException("Invalid JSON"))).isFalse();
    }

    @Test
    void retriesTransientPostConnectAndServerErrors() {
        assertThat(predicate.test(new OpenAIIoException("Request failed", new SocketTimeoutException("Read timed out")))).isTrue();
        assertThat(predicate.test(new OpenAIIoException("Request failed", new IOException("Connection reset")))).isTrue();
        assertThat(predicate.test(InternalServerException.builder().statusCode(503).headers(emptyHeaders).build())).isTrue();
    }

    @Test
    void handlesCyclicExceptionCausesSafely() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);

        assertThat(predicate.test(first)).isTrue();
    }
}
