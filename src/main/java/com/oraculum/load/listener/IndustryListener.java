package com.oraculum.load.listener;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.IndustryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndustryListener {

    private final CompanyMetadataApi companyMetadataApi;

    @KafkaListener(topics = "${oraculum.kafka.topics.industry}", groupId = "${oraculum.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onIndustry(IndustryDto industry) {
        try {
            log.debug("Received industry: {}", industry.industryName());
            companyMetadataApi.createOrUpdateIndustry(industry);
        } catch (Exception e) {
            log.error("Failed to process industry {}, discarding message", industry.industryId(), e);
        }
    }
}
