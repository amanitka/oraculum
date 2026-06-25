package com.oraculum.load.listener;

import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.dto.NewsArticleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsListener {

    private final CompanyNewsApi companyNewsApi;

    @KafkaListener(topics = "${oraculum.kafka.topics.news}", groupId = "${oraculum.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onNewsBatch(List<NewsArticleDto> articles) {
        try {
            log.info("Received news batch: {} articles", articles.size());
            companyNewsApi.createOrUpdateNewsBatch(articles);
        } catch (Exception e) {
            log.error("Failed to process news batch, discarding message", e);
        }
    }
}
