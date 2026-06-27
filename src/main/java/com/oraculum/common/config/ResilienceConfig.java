package com.oraculum.common.config;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(@NonNull EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                retry.getEventPublisher()
                        .onRetry(e -> log.warn("Retry attempt #{} for '{}' failed",
                                e.getNumberOfRetryAttempts(), retry.getName()))
                        .onError(e -> log.error("Retry for '{}' failed permanently: {}",
                                retry.getName(), Optional.ofNullable(e.getLastThrowable()).map(Throwable::getMessage)
                                        .orElse("No error message provided")));
            }

            @Override
            public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<Retry> entryRemovedEvent) {
            }

            @Override
            public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }
}
