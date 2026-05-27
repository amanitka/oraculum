package com.oraculum.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oraculum")
@Getter
@Setter
public class OraculumProperties {

    private final Database database = new Database();
    private final Simfin simfin = new Simfin();
    private final AlphaVantage alphaVantage = new AlphaVantage();
    private final Kafka kafka = new Kafka();

    @Getter
    @Setter
    public static class Database {
        private String host;
        private int port;
        private String name;
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class Simfin {
        private String apiKey;
        private int chunkSize;
        private int refreshDays;
    }

    @Getter
    @Setter
    public static class AlphaVantage {
        private String apiUrl;
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Kafka {
        private final Topics topics = new Topics();
        private String brokers;
        private String consumerGroup;

        @Getter
        @Setter
        public static class Topics {
            private String dataFileReady;
            private String market;
            private String industry;
            private String news;
            private String harvesterRequest;
        }
    }
}
