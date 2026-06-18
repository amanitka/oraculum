package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.service.mapper.NewsArticleMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void companyDto_fromEntity_mapsCorrectly() {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(1);
        entity.setTicker("AAPL");
        entity.setMarket("US");
        entity.setCompanyName("Apple Inc.");
        entity.setIndustryId("IND1");
        entity.setIndustryName("Tech");
        entity.setSectorName("IT");
        entity.setIsin("US0378331005");
        entity.setDescription("Maker of iPhones.");
        entity.setEmployeeCount(150000L);
        entity.setCurrency("USD");
        entity.setCik("0000320193");
        OffsetDateTime now = OffsetDateTime.now();
        entity.setExtractedAt(now);

        CompanyDto dto = CompanyDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.ticker()).isEqualTo("AAPL");
        assertThat(dto.market()).isEqualTo("US");
        assertThat(dto.companyName()).isEqualTo("Apple Inc.");
        assertThat(dto.industryId()).isEqualTo("IND1");
        assertThat(dto.industryName()).isEqualTo("Tech");
        assertThat(dto.sectorName()).isEqualTo("IT");
        assertThat(dto.isin()).isEqualTo("US0378331005");
        assertThat(dto.description()).isEqualTo("Maker of iPhones.");
        assertThat(dto.employeeCount()).isEqualTo(150000L);
        assertThat(dto.currency()).isEqualTo("USD");
        assertThat(dto.cik()).isEqualTo("0000320193");
        assertThat(dto.extractedAt()).isEqualTo(now);
    }

    @Test
    void companyDto_fromEntity_nullHandling() {
        assertThat(CompanyDto.fromEntity(null)).isNull();
    }

    @Test
    void newsArticleDto_toNewsEntity_mapsCorrectly() {
        LocalDateTime timePublished = LocalDateTime.of(2023, 1, 1, 10, 0);
        OffsetDateTime extractedAt = OffsetDateTime.now();

        NewsArticleDto.TopicRelevanceDto topic = new NewsArticleDto.TopicRelevanceDto("Tech", 0.9f);

        NewsArticleDto dto = new NewsArticleDto(
                "n1",
                "Apple News",
                "http://apple.com/news",
                timePublished,
                List.of("John Doe"),
                "Summary",
                "Source",
                "Category",
                "Domain",
                List.of(topic),
                0.8f,
                "Positive",
                extractedAt,
                "Definition",
                "RelevanceDef",
                null
        );

        NewsArticleMapper mapper = new NewsArticleMapper();
        NewsEntity entity = mapper.toNewsEntity(dto);

        assertThat(entity.getId()).isEqualTo("n1");
        assertThat(entity.getTitle()).isEqualTo("Apple News");
        assertThat(entity.getUrl()).isEqualTo("http://apple.com/news");
        assertThat(entity.getTimePublished().toInstant()).isEqualTo(timePublished.toInstant(ZoneOffset.UTC));
        assertThat(entity.getAuthors()).containsExactly("John Doe");
        assertThat(entity.getSummary()).isEqualTo("Summary");
        assertThat(entity.getSource()).isEqualTo("Source");
        assertThat(entity.getCategoryWithinSource()).isEqualTo("Category");
        assertThat(entity.getSourceDomain()).isEqualTo("Domain");
        assertThat(entity.getTopics()).hasSize(1);
        assertThat(entity.getTopics().getFirst().topic()).isEqualTo("Tech");
        assertThat(entity.getOverallSentimentScore()).isEqualTo(0.8f);
        assertThat(entity.getOverallSentimentLabel()).isEqualTo("Positive");
        assertThat(entity.getExtractedAt()).isEqualTo(extractedAt);
        assertThat(entity.getSentimentScoreDefinition()).isEqualTo("Definition");
        assertThat(entity.getRelevanceScoreDefinition()).isEqualTo("RelevanceDef");
    }

    @Test
    void newsArticleDto_toNewsTickerEntities_mapsCorrectly() {
        LocalDateTime timePublished = LocalDateTime.of(2023, 1, 1, 10, 0);

        NewsArticleDto.NewsTickerSentimentDto sentiment = new NewsArticleDto.NewsTickerSentimentDto(
                "AAPL", 0.9f, 0.8f, "Bullish"
        );

        NewsArticleDto dto = new NewsArticleDto(
                "n1", "Title", "url", timePublished, null, null, null, null, null, null, null, null, null, null, null,
                List.of(sentiment)
        );

        NewsArticleMapper mapper = new NewsArticleMapper();
        List<NewsTickerEntity> entities = mapper.toNewsTickerEntities(dto);

        assertThat(entities).hasSize(1);
        NewsTickerEntity entity = entities.getFirst();

        assertThat(entity.getNewsId()).isEqualTo("n1");
        assertThat(entity.getTicker()).isEqualTo("AAPL");
        assertThat(entity.getTimePublished().toInstant()).isEqualTo(timePublished.toInstant(ZoneOffset.UTC));
        assertThat(entity.getRelevanceScore()).isEqualTo(0.9f);
        assertThat(entity.getTickerSentimentScore()).isEqualTo(0.8f);
        assertThat(entity.getTickerSentimentLabel()).isEqualTo("Bullish");
    }

    @Test
    void newsArticleDto_toNewsTickerEntities_handlesNull() {
        NewsArticleDto dto = new NewsArticleDto(
                "n1", "Title", "url", null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        NewsArticleMapper mapper = new NewsArticleMapper();
        List<NewsTickerEntity> entities = mapper.toNewsTickerEntities(dto);
        assertThat(entities).isEmpty();
    }

    @Test
    void newsTickerDto_from_mapsCorrectly() {
        NewsEntity news = new NewsEntity();
        news.setId("n1");
        news.setTitle("Title");
        news.setUrl("url");
        OffsetDateTime now = OffsetDateTime.now();
        news.setTimePublished(now);
        news.setAuthors(List.of("authors"));
        news.setSummary("summary");
        news.setSource("source");
        news.setCategoryWithinSource("cat");
        news.setSourceDomain("domain");
        news.setTopics(List.of(new NewsArticleDto.TopicRelevanceDto("topics", 1.0f)));
        news.setOverallSentimentScore(0.5f);
        news.setOverallSentimentLabel("Neutral");
        news.setExtractedAt(now);
        news.setSentimentScoreDefinition("def");
        news.setRelevanceScoreDefinition("rel");

        NewsTickerEntity tickerEntity = new NewsTickerEntity();
        tickerEntity.setTicker("AAPL");
        tickerEntity.setRelevanceScore(0.8f);
        tickerEntity.setTickerSentimentScore(0.9f);
        tickerEntity.setTickerSentimentLabel("Bullish");

        NewsTickerDto dto = NewsTickerDto.from(news, tickerEntity);

        assertThat(dto.id()).isEqualTo("n1");
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.url()).isEqualTo("url");
        assertThat(dto.timePublished()).isEqualTo(now);
        assertThat(dto.authors()).containsExactly("authors");
        assertThat(dto.summary()).isEqualTo("summary");
        assertThat(dto.source()).isEqualTo("source");
        assertThat(dto.categoryWithinSource()).isEqualTo("cat");
        assertThat(dto.sourceDomain()).isEqualTo("domain");
        assertThat(dto.topics()).hasSize(1);
        assertThat(dto.topics().getFirst().topic()).isEqualTo("topics");
        assertThat(dto.overallSentimentScore()).isEqualTo(0.5f);
        assertThat(dto.overallSentimentLabel()).isEqualTo("Neutral");
        assertThat(dto.extractedAt()).isEqualTo(now);
        assertThat(dto.sentimentScoreDefinition()).isEqualTo("def");
        assertThat(dto.relevanceScoreDefinition()).isEqualTo("rel");

        assertThat(dto.ticker()).isEqualTo("AAPL");
        assertThat(dto.relevanceScore()).isEqualTo(0.8f);
        assertThat(dto.tickerSentimentScore()).isEqualTo(0.9f);
        assertThat(dto.tickerSentimentLabel()).isEqualTo("Bullish");
    }
}
