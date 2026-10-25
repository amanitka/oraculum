package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.FinnhubCompanyNewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FinnhubClientTest {

    private MockRestServiceServer mockServer;
    private FinnhubClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new FinnhubClient(builder.build());
    }

    @Test
    void fetchCompanyNews_sendsCorrectRequest_andReturnsParsedResponse() {
        String jsonResponse = """
                [
                    {
                        "category": "company news",
                        "datetime": 1569550360,
                        "headline": "Apple Announces Event",
                        "id": 25286,
                        "image": "https://example.com/img.jpg",
                        "related": "AAPL",
                        "source": "Reuters",
                        "summary": "Apple announces next generation hardware event.",
                        "url": "https://reuters.com/apple-news"
                    }
                ]
                """;

        mockServer.expect(requestTo("/api/v1/company-news?symbol=AAPL&from=2026-01-01&to=2026-01-31"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<FinnhubCompanyNewsItem> response = client.fetchCompanyNews("AAPL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        mockServer.verify();

        assertThat(response).hasSize(1);
        FinnhubCompanyNewsItem item = response.getFirst();
        assertThat(item.id()).isEqualTo(25286L);
        assertThat(item.headline()).isEqualTo("Apple Announces Event");
        assertThat(item.source()).isEqualTo("Reuters");
        assertThat(item.datetime()).isEqualTo(1569550360L);
        assertThat(item.summary()).isEqualTo("Apple announces next generation hardware event.");
        assertThat(item.url()).isEqualTo("https://reuters.com/apple-news");
    }

    @Test
    void fetchCompanyNews_emptyResponse_returnsEmptyList() {
        mockServer.expect(requestTo("/api/v1/company-news?symbol=AAPL&from=2026-01-01&to=2026-01-31"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<FinnhubCompanyNewsItem> response = client.fetchCompanyNews("AAPL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        mockServer.verify();
        assertThat(response).isEmpty();
    }
}
