package com.oraculum.harvester.service;

import com.oraculum.harvester.domain.ApiUsageEntity;
import com.oraculum.harvester.domain.ProviderType;
import com.oraculum.harvester.repository.ApiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiUsageTrackerService {

    private final ApiUsageRepository repository;

    private ApiUsageEntity createApiUsageEntity(ProviderType provider, LocalDate today) {
        ApiUsageEntity entity = new ApiUsageEntity();
        entity.setProvider(provider);
        entity.setLastUsageDate(today);
        entity.setCallCount(0);
        return entity;
    }

    /**
     * Checks if the provider can make a call based on the daily limit.
     */
    @Transactional(readOnly = true)
    public boolean canMakeCall(ProviderType provider, int dailyLimit) {
        return repository.findById(provider)
                .map(usage -> {
                    LocalDate today = LocalDate.now();
                    if (usage.getLastUsageDate().isBefore(today)) {
                        return true; // New day, limit resets
                    }
                    return usage.getCallCount() < dailyLimit;
                })
                .orElse(true); // No usage recorded yet
    }

    /**
     * Increments the call count for the provider, resetting it if it's a new day.
     */
    @Transactional
    public void recordCall(ProviderType provider) {
        LocalDate today = LocalDate.now();
        ApiUsageEntity usage = repository.findById(provider).orElseGet(() -> createApiUsageEntity(provider, today));
        if (usage.getLastUsageDate().isBefore(today)) {
            usage.setLastUsageDate(today);
            usage.setCallCount(1);
        } else {
            usage.setCallCount(usage.getCallCount() + 1);
        }

        repository.save(usage);
        log.debug("Recorded API call for {}. Today's count: {}", provider, usage.getCallCount());
    }
}
