package com.oraculum.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oraculum")
public record OraculumProperties(Data data,
                                 Database database,
                                 Simfin simfin,
                                 AlphaVantage alphaVantage,
                                 Kafka kafka,
                                 Harvester harvester) {

    public record Data(SharePrice sharePrice,
                       News news) {

        public record SharePrice(int incrementalWindowDays) {
        }

        public record News(int incrementalWindowHours) {
        }
    }

    public record Database(String host,
                           int port,
                           String name,
                           String username,
                           String password,
                           Maintenance maintenance) {
        public record Maintenance(Boolean enabled,
                                  String vacuumCron,
                                  String partitionCron) {
        }
    }

    public record Simfin(String apiKey,
                         int chunkSize,
                         int refreshDays) {
    }

    public record AlphaVantage(String apiUrl,
                               String apiKey) {
    }

    public record Kafka(Topics topics,
                        String brokers,
                        String consumerGroup) {
        public record Topics(String dataFileReady,
                             String market,
                             String industry,
                             String news,
                             String harvesterRequest,
                             String analystRequest) {
        }
    }

    public record Harvester(String exportPath) {
    }
}
