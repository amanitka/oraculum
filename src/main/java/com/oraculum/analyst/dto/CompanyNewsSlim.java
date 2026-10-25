package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.harvester.api.dto.CompanyNewsDto;

public record CompanyNewsSlim(
        @JsonProperty("citation_id") String citationId,
        String date,
        String source,
        String headline,
        String summary
) {
    public static CompanyNewsSlim from(CompanyNewsDto dto, String citationId) {
        if (dto == null) {
            return null;
        }
        String dateStr = dto.publishedAt() != null ? dto.publishedAt().toLocalDate().toString() : null;
        return new CompanyNewsSlim(
                citationId,
                dateStr,
                dto.source(),
                dto.headline(),
                dto.summary()
        );
    }
}
