package com.oraculum.analyst.service;

import com.oraculum.analyst.api.AnalysisUsageApi;
import com.oraculum.analyst.api.dto.UserAnalysisUsage;
import com.oraculum.analyst.repository.CompanyAnalysisRepository;
import com.oraculum.user.api.dto.AnalysisLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisUsageService implements AnalysisUsageApi {

    private final CompanyAnalysisRepository companyAnalysisRepository;

    @Override
    public void checkLimit(Long userId, AnalysisLimit limit) {
        if (limit == null) return;
        
        OffsetDateTime startDate = calculateStartDate(limit);
        long currentCount = companyAnalysisRepository.countByRequestedByAndCreatedAtAfter(userId, startDate);
        
        if (currentCount >= limit.count()) {
            log.warn("User ID {} exceeded analysis limit of {}", userId, limit);
            throw new RuntimeException("You have reached your analysis limit of " + limit + ". Please wait or contact the admin.");
        }
    }

    @Override
    public UserAnalysisUsage getUsage(Long userId, AnalysisLimit limit) {
        if (limit == null) {
            return new UserAnalysisUsage(0, null, null);
        }

        OffsetDateTime startDate = calculateStartDate(limit);
        long currentCount = companyAnalysisRepository.countByRequestedByAndCreatedAtAfter(userId, startDate);

        return new UserAnalysisUsage(currentCount, (long) limit.count(), limit.period().name());
    }

    private OffsetDateTime calculateStartDate(AnalysisLimit limit) {
        return switch (limit.period()) {
            case D -> OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
            case W -> OffsetDateTime.now(ZoneOffset.UTC).minusWeeks(1);
            case M -> OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1);
        };
    }
}
