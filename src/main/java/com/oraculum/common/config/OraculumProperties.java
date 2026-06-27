package com.oraculum.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oraculum")
public record OraculumProperties(Data data,
                                 Database database,
                                 Kafka kafka,
                                 Harvester harvester) {

    public record Data(SharePrice sharePrice,
                       News news,
                       InsiderTransactions insiderTransactions) {

        public record SharePrice(int incrementalWindowDays) {
        }

        public record News(int incrementalWindowHours) {
        }

        public record InsiderTransactions(String cron) {
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

    public record Kafka(Topics topics,
                        String brokers,
                        String consumerGroup) {
        public record Topics(String dataFileReady,
                             String market,
                             String industry,
                             String news,
                             String harvesterRequest) {
        }
    }

    public record Harvester(String exchangeDirectory,
                            ExchangeCleanup exchangeCleanup,
                            AlphaVantage alphaVantage,
                            Fred fred) {
        public record ExchangeCleanup(Boolean enabled,
                                      Integer retentionDays,
                                      String cron) {
        }

        public record AlphaVantage(String apiKey,
                                   String baseUrl,
                                   int dailyLimit,
                                   int reservedCalls) {
        }

        public record Fred(String apiKey,
                           String baseUrl) {
        }
    }
}
