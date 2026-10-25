package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.AlphaVantageEarningsEstimatesResponse;
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
    void fetchEarningsEstimates_sendsCorrectRequest_andReturnsParsedResponse() {
        String jsonResponse = """
                {
                    "symbol": "AAPL",
                    "estimates": [
                        {
                            "date": "2026-06-30",
                            "horizon": "fiscal quarter",
                            "eps_estimate_average": 1.55
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("/query?function=EARNINGS_ESTIMATES&symbol=AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        AlphaVantageEarningsEstimatesResponse response = client.fetchEarningsEstimates("AAPL");

        mockServer.verify();

        assertThat(response).isNotNull();
        assertThat(response.symbol()).isEqualTo("AAPL");
        assertThat(response.estimates()).hasSize(1);
        assertThat(response.estimates().getFirst().epsEstimateAverage()).isEqualByComparingTo("1.55");
    }
}
