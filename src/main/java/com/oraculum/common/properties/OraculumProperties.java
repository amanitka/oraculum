package com.oraculum.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oraculum")
public record OraculumProperties(Database database, Simfin simfin, AlphaVantage alphaVantage, Kafka kafka) {
    public record Database(String host, int port, String name, String username, String password) {
    }

    public record Simfin(String apiKey, int chunkSize, int refreshDays) {
    }

    public record AlphaVantage(String apiUrl, String apiKey) {
    }

    public record Kafka(Topics topics, String brokers, String consumerGroup) {
        public record Topics(String dataFileReady, String market, String industry, String news,
                             String harvesterRequest) {
        }
    }
}
