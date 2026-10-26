package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.harvester.api.dto.CompanyNewsDto;

import java.util.Locale;
import java.util.regex.Pattern;

public record CompanyNewsSlim(
        @JsonProperty("citation_id") String citationId,
        String date,
        String source,
        String relevance,
        String headline,
        String summary
) {
    public static final String RELEVANCE_DIRECT = "COMPANY_DIRECT";
    public static final String RELEVANCE_MARKET_CONTEXT = "MARKET_CONTEXT";

    private static final Pattern CORPORATE_SUFFIX_PATTERN = Pattern.compile(
            "(?i)\\b(corp|corporation|inc|incorporated|ltd|limited|plc|co|company|holdings|group|sa|nv|ag)\\.?$"
    );

    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId) {
        return from(dto, citationId, RELEVANCE_DIRECT);
    }

    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId, String relevance) {
        if (dto == null) {
            return null;
        }
        String dateStr = dto.publishedAt() != null ? dto.publishedAt().toLocalDate().toString() : null;
        return new CompanyNewsSlim(
                citationId,
                dateStr,
                dto.source(),
                relevance,
                dto.headline(),
                dto.summary()
        );
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
