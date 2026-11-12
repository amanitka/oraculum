package com.oraculum.llm.config;

import com.openai.errors.BadRequestException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.errors.UnprocessableEntityException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Determines whether an LLM execution exception should be retried by Resilience4j.
 * Connection-establishment failures and non-retryable client errors fail fast so the
 * router can immediately fall back to the next configured provider.
 */
public class LlmRetryExceptionPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return !hasNonRetryableCause(throwable, visited);
    }

    private boolean hasNonRetryableCause(Throwable throwable, Set<Throwable> visited) {
        if (throwable == null || !visited.add(throwable)) {
            return false;
        }
        if (isNonRetryableException(throwable)) {
            return true;
        }
        if (hasNonRetryableCause(throwable.getCause(), visited)) {
            return true;
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            if (hasNonRetryableCause(suppressed, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonRetryableException(Throwable throwable) {
        return isClientOrDataError(throwable) || isConnectionFailure(throwable);
    }

    private boolean isClientOrDataError(Throwable throwable) {
        return throwable instanceof RateLimitException
                || throwable instanceof BadRequestException
                || throwable instanceof UnauthorizedException
                || throwable instanceof PermissionDeniedException
                || throwable instanceof NotFoundException
                || throwable instanceof UnprocessableEntityException
                || throwable instanceof OpenAIInvalidDataException;
    }

    private boolean isConnectionFailure(Throwable throwable) {
        if (throwable instanceof ConnectException
                || throwable instanceof NoRouteToHostException
                || throwable instanceof UnknownHostException) {
            return true;
        }
        if (throwable instanceof SocketTimeoutException && throwable.getMessage() != null) {
            return throwable.getMessage().toLowerCase(Locale.ROOT).contains("connect timed out");
        }
        return false;
    }
}
