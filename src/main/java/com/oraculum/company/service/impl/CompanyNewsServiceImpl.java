package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.dto.NewsArticleDto;
import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.company.api.dto.TickerNewsSentimentDto;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.repository.NewsRepository;
import com.oraculum.company.repository.NewsTickerRepository;
import com.oraculum.company.repository.TickerNewsSentimentRepository;
import com.oraculum.company.service.mapper.NewsArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyNewsServiceImpl implements CompanyNewsApi {

    private final NewsRepository newsRepository;
    private final NewsTickerRepository newsTickerRepository;
    private final TickerNewsSentimentRepository tickerNewsSentimentRepository;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getNewsLastTimePublished() {
        return newsRepository.findMaxTimePublished();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after) {
        OffsetDateTime afterDateTime = after.atStartOfDay().atOffset(ZoneOffset.UTC);
        List<NewsTickerEntity> tickerRows = newsTickerRepository.findByTickerAndTimePublishedAfter(ticker, afterDateTime);
        List<String> newsIds = tickerRows.stream().map(NewsTickerEntity::getNewsId).collect(Collectors.toList());
        Map<String, NewsEntity> newsById = newsRepository.findByIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(NewsEntity::getId, Function.identity()));
        return tickerRows.stream()
                .filter(t -> newsById.containsKey(t.getNewsId()))
                .map(t -> NewsTickerDto.from(newsById.get(t.getNewsId()), t))
                .sorted(Comparator.comparing(NewsTickerDto::timePublished).reversed()) // Sort by timePublished descending
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TickerNewsSentimentDto> getNewsSentimentByTicker(String ticker) {
        return tickerNewsSentimentRepository.findByTicker(ticker).map(TickerNewsSentimentDto::fromEntity);
    }

    @Override
    @Transactional
    public void createOrUpdateNewsBatch(List<NewsArticleDto> articles) {
        newsRepository.saveAll(articles.stream().map(newsArticleMapper::toNewsEntity).collect(Collectors.toList()));
        newsTickerRepository.saveAll(articles.stream().flatMap(a -> newsArticleMapper.toNewsTickerEntities(a).stream()).collect(Collectors.toList()));
    }
}
