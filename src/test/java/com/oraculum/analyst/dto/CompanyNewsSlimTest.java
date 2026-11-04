package com.oraculum.analyst.dto;

import com.oraculum.harvester.api.dto.CompanyNewsDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyNewsSlimTest {

    @Test
    void calculateRecencyWeightAndTier_shouldClassifyCorrectly() {
        // Breaking: <= 2 days
        assertThat(CompanyNewsSlim.calculateRecencyWeight(0)).isEqualTo(1.00f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(0)).isEqualTo("BREAKING");
        assertThat(CompanyNewsSlim.calculateRecencyWeight(2)).isEqualTo(1.00f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(2)).isEqualTo("BREAKING");

        // Recent: 3 to 7 days
        assertThat(CompanyNewsSlim.calculateRecencyWeight(3)).isEqualTo(0.75f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(3)).isEqualTo("RECENT");
        assertThat(CompanyNewsSlim.calculateRecencyWeight(7)).isEqualTo(0.75f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(7)).isEqualTo("RECENT");

        // Developing: 8 to 14 days
        assertThat(CompanyNewsSlim.calculateRecencyWeight(8)).isEqualTo(0.50f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(8)).isEqualTo("DEVELOPING");
        assertThat(CompanyNewsSlim.calculateRecencyWeight(14)).isEqualTo(0.50f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(14)).isEqualTo("DEVELOPING");

        // Context: > 14 days
        assertThat(CompanyNewsSlim.calculateRecencyWeight(15)).isEqualTo(0.25f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(15)).isEqualTo("CONTEXT");
        assertThat(CompanyNewsSlim.calculateRecencyWeight(30)).isEqualTo(0.25f);
        assertThat(CompanyNewsSlim.calculateRecencyTier(30)).isEqualTo("CONTEXT");
    }

    @Test
    void from_withReferenceDate_shouldComputeRecency() {
        LocalDate refDate = LocalDate.of(2026, 10, 20);
        OffsetDateTime publishedAt = OffsetDateTime.of(2026, 10, 19, 10, 0, 0, 0, ZoneOffset.UTC); // 1 day ago

        CompanyNewsDto dto = CompanyNewsDto.builder()
                .id(1L)
                .headline("Nvidia announces new architecture")
                .summary("Details about the architecture")
                .source("Reuters")
                .publishedAt(publishedAt)
                .build();

        CompanyNewsSlim slim = CompanyNewsSlim.from(dto, "c1", CompanyNewsSlim.RELEVANCE_DIRECT, refDate);

        assertThat(slim).isNotNull();
        assertThat(slim.citationId()).isEqualTo("c1");
        assertThat(slim.recencyWeight()).isEqualTo(1.00f);
        assertThat(slim.recencyTier()).isEqualTo("BREAKING");
        assertThat(slim.date()).isEqualTo("2026-10-19");
    }

    @Test
    void from_whenPublishedAtIsNull_shouldDefaultToContext() {
        CompanyNewsDto dto = CompanyNewsDto.builder()
                .id(2L)
                .headline("General industry update")
                .source("Bloomberg")
                .publishedAt(null)
                .build();

        CompanyNewsSlim slim = CompanyNewsSlim.from(dto, "c2");

        assertThat(slim).isNotNull();
        assertThat(slim.recencyWeight()).isEqualTo(0.25f);
        assertThat(slim.recencyTier()).isEqualTo("CONTEXT");
    }

    @Test
    void classifyRelevance_shouldDetectDirectTickerAndName() {
        CompanyNewsDto direct = CompanyNewsDto.builder()
                .headline("NVDA hits all-time high")
                .summary("Tech rally continues")
                .build();
        assertThat(CompanyNewsSlim.classifyRelevance(direct, "NVDA", "NVIDIA Corp")).isEqualTo(CompanyNewsSlim.RELEVANCE_DIRECT);

        CompanyNewsDto market = CompanyNewsDto.builder()
                .headline("Broad semiconductor index edges higher")
                .summary("General macro commentary")
                .build();
        assertThat(CompanyNewsSlim.classifyRelevance(market, "NVDA", "NVIDIA Corp")).isEqualTo(CompanyNewsSlim.RELEVANCE_MARKET_CONTEXT);
    }
}
