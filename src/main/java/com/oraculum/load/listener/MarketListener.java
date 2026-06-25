package com.oraculum.load.listener;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.MarketDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketListener {

    private final CompanyMetadataApi companyMetadataApi;

    @KafkaListener(topics = "${oraculum.kafka.topics.market}", groupId = "${oraculum.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMarket(MarketDto market) {
        try {
            log.debug("Received market: {}", market.marketId());
            companyMetadataApi.createOrUpdateMarket(market);
        } catch (Exception e) {
            log.error("Failed to process market {}, discarding message", market.marketId(), e);
        }
    }
}
