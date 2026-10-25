package com.oraculum.harvester.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubCompanyNewsItem(
        String category,
        Long datetime,
        String headline,
        Long id,
        String image,
        String related,
        String source,
        String summary,
        String url
) {}
