package com.oraculum.load.listener;

import com.oraculum.company.api.CompanyLoadApi;
import com.oraculum.company.api.dto.IndustryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndustryListener {

    private final CompanyLoadApi companyLoadApi;

    @KafkaListener(topics = "${oraculum.kafka.topics.industry}", groupId = "${oraculum.kafka.consumerGroup}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onIndustry(IndustryDto industry) {
        log.info("Received industry: {}", industry.industryId());
        companyLoadApi.createOrUpdateIndustry(industry);
    }
}
