package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.AlphaVantageNewsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlphaVantageClientTest {

    private MockRestServiceServer mockServer;
    private AlphaVantageClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AlphaVantageClient(builder.build());
    }

    @Test
    void fetchNewsSentiment_sendsCorrectRequest_andReturnsParsedResponse() {
        String jsonResponse = """
                {
                    "items": "50",
                    "sentiment_score_definition": "x = 0.15",
                    "relevance_score_definition": "0 < x <= 1",
                    "feed": [
                        {
                            "title": "Apple News",
                            "url": "http://apple.com"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("/query?function=NEWS_SENTIMENT&limit=1000&time_from=20230101T0000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        AlphaVantageNewsResponse response = client.fetchNewsSentiment("20230101T0000");

        mockServer.verify();
        
        assertThat(response).isNotNull();
        assertThat(response.items()).isEqualTo("50");
        assertThat(response.sentimentScoreDefinition()).isEqualTo("x = 0.15");
        assertThat(response.relevanceScoreDefinition()).isEqualTo("0 < x <= 1");
        assertThat(response.feed()).hasSize(1);
        assertThat(response.feed().getFirst().title()).isEqualTo("Apple News");
        assertThat(response.feed().getFirst().url()).isEqualTo("http://apple.com");
    }

    @Test
    void fetchNewsSentiment_withoutTimeFrom_sendsCorrectRequest() {
        mockServer.expect(requestTo("/query?function=NEWS_SENTIMENT&limit=1000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"items\": \"0\"}", MediaType.APPLICATION_JSON));

        AlphaVantageNewsResponse response = client.fetchNewsSentiment(null);

        mockServer.verify();
        assertThat(response).isNotNull();
    }
}
