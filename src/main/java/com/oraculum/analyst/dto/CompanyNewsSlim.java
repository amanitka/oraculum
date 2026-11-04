package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import lombok.Builder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

@Builder
public record CompanyNewsSlim(
        @JsonProperty("citation_id") String citationId,
        String date,
        String source,
        String relevance,
        String headline,
        String summary,
        @JsonProperty("recency_weight") Float recencyWeight,
        @JsonProperty("recency_tier") String recencyTier
) {
    public static final String RELEVANCE_DIRECT = "COMPANY_DIRECT";
    public static final String RELEVANCE_MARKET_CONTEXT = "MARKET_CONTEXT";

    public static final String TIER_BREAKING = "BREAKING";
    public static final String TIER_RECENT = "RECENT";
    public static final String TIER_DEVELOPING = "DEVELOPING";
    public static final String TIER_CONTEXT = "CONTEXT";

    private static final Pattern CORPORATE_SUFFIX_PATTERN = Pattern.compile(
            "(?i)\\b(corp|corporation|inc|incorporated|ltd|limited|plc|co|company|holdings|group|sa|nv|ag)\\.?$"
    );

    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId) {
        return from(dto, citationId, RELEVANCE_DIRECT, null);
    }

    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId, String relevance) {
        return from(dto, citationId, relevance, null);
    }

    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId, String relevance, LocalDate referenceDate) {
        if (dto == null) {
            return null;
        }
        String dateStr = dto.publishedAt() != null ? dto.publishedAt().toLocalDate().toString() : null;
        long days = computeAgeDays(dto.publishedAt(), referenceDate);
        return CompanyNewsSlim.builder()
                .citationId(citationId)
                .date(dateStr)
                .source(dto.source())
                .relevance(relevance)
                .headline(dto.headline())
                .summary(dto.summary())
                .recencyWeight(calculateRecencyWeight(days))
                .recencyTier(calculateRecencyTier(days))
                .build();
    }

    public static float calculateRecencyWeight(long days) {
        if (days <= 2) return 1.00f;
        if (days <= 7) return 0.75f;
        if (days <= 14) return 0.50f;
        return 0.25f;
    }

    public static String calculateRecencyTier(long days) {
        if (days <= 2) return TIER_BREAKING;
        if (days <= 7) return TIER_RECENT;
        if (days <= 14) return TIER_DEVELOPING;
        return TIER_CONTEXT;
    }

    private static long computeAgeDays(OffsetDateTime publishedAt, LocalDate referenceDate) {
        if (publishedAt == null) {
            return 30;
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        long days = ChronoUnit.DAYS.between(publishedAt.toLocalDate(), ref);
        return Math.max(0, days);
    }

    public static boolean isDirectCompanyNews(CompanyNewsDto dto, String ticker, String companyName) {
        return RELEVANCE_DIRECT.equals(classifyRelevance(dto, ticker, companyName));
    }

    public static String classifyRelevance(CompanyNewsDto dto, String ticker, String companyName) {
        if (dto == null) {
            return RELEVANCE_MARKET_CONTEXT;
        }
        String combined = ((dto.headline() != null ? dto.headline() : "") + " "
                + (dto.summary() != null ? dto.summary() : "")).toLowerCase(Locale.ROOT);

        if (containsTicker(combined, ticker) || containsCompanyName(combined, companyName)) {
            return RELEVANCE_DIRECT;
        }
        return RELEVANCE_MARKET_CONTEXT;
    }

    private static boolean containsTicker(String lowerText, String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        String escaped = Pattern.quote(ticker.trim().toLowerCase(Locale.ROOT));
        return Pattern.compile("\\b" + escaped + "\\b").matcher(lowerText).find();
    }

    private static boolean containsCompanyName(String lowerText, String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return false;
        }
        String cleaned = CORPORATE_SUFFIX_PATTERN.matcher(companyName.trim()).replaceAll("").trim();
        cleaned = CORPORATE_SUFFIX_PATTERN.matcher(cleaned).replaceAll("").trim().toLowerCase(Locale.ROOT);
        if (cleaned.length() < 3) {
            return false;
        }
        return lowerText.contains(cleaned);
    }
}
