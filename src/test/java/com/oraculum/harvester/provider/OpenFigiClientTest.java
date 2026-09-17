package com.oraculum.harvester.provider;

import com.oraculum.harvester.provider.dto.openfigi.OpenFigiMappingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenFigiClientTest {

    private MockRestServiceServer mockServer;
    private OpenFigiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OpenFigiClient(builder.build());
    }

    @Test
    void fetchTickersForCusips_whenEmpty_returnsEmptyList() {
        List<OpenFigiMappingResponse> result = client.fetchTickersForCusips(List.of(), "US");
        assertThat(result).isEmpty();
    }

    @Test
    void fetchTickersForCusips_sendsPostRequest_andParsesResponse() {
        String jsonResponse = """
                [
                    {
                        "data": [
                            {
                                "figi": "BBG000BBQCY0",
                                "name": "ADVANCED MICRO DEVICES",
                                "ticker": "AMD",
                                "exchCode": "US",
                                "compositeFIGI": "BBG000BBQCY0",
                                "securityType": "Common Stock"
                            }
                        ]
                    }
                ]
                """;

        mockServer.expect(requestTo("/v3/mapping"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        [
                            {"idType": "ID_CUSIP", "idValue": "007903107", "exchCode": "US"}
                        ]
                        """))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<OpenFigiMappingResponse> response = client.fetchTickersForCusips(List.of("007903107"), "US");

        mockServer.verify();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().hasData()).isTrue();
        assertThat(response.getFirst().data().getFirst().ticker()).isEqualTo("AMD");
    }
}
