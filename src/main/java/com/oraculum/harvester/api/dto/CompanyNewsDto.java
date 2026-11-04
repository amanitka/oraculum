package com.oraculum.harvester.api.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record CompanyNewsDto(
        long id,
        OffsetDateTime publishedAt,
        String headline,
        String summary,
        String source,
        String url,
        List<String> related
) {}
