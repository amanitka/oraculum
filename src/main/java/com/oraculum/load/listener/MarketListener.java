package com.oraculum.load.listener;

import com.oraculum.company.api.CompanyLoadApi;
import com.oraculum.company.api.dto.MarketDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketListener {

    private final CompanyLoadApi companyLoadApi;

    @KafkaListener(topics = "${oraculum.kafka.topics.market}", groupId = "${oraculum.kafka.consumerGroup}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMarket(MarketDto market) {
        log.info("Received market: {}", market.marketId());
        companyLoadApi.createOrUpdateMarket(market);
    }
}
