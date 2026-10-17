package com.oraculum.analyst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitationVerifierServiceTest {

    private CitationVerifierService service;

    @BeforeEach
    void setUp() {
        service = new CitationVerifierService(new ObjectMapper());
    }

    @Test
    void testVerifyCitations_noMismatch() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(310, Map.of("eps_estimate_average", 8.9885));

        String report = "FY2027 EPS consensus average $8.99 [310].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("FY2027 EPS consensus average $8.99 [310].");
    }

    @Test
    void testVerifyCitations_mismatch() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(309, Map.of("eps_estimate_average", 12.869));

        String report = "FY2027 EPS consensus average $8.99 [309].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("FY2027 EPS consensus average $8.99 [309 ?].\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified. Citations marked with [!] reference missing or hallucinated sources.*");
    }

    @Test
    void testVerifyCitations_percentage() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("gross_margin", 0.7414543));

        String report = "Margin was 74.1% [1].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Margin was 74.1% [1].");
    }

    @Test
    void testVerifyCitations_largeNumber() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("revenue", 81615000000.0));

        String report = "Revenue reached $81.6B [1].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Revenue reached $81.6B [1].");
    }

    @Test
    void testVerifyCitations_qualitativeNoNumbers() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("summary", "Company is doing great"));

        String report = "The company has a positive outlook [1].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("The company has a positive outlook [1].");
    }

    @Test
    void testVerifyCitations_multipleCitations() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("eps", 8.99));
        citations.put(2, Map.of("eps", 12.87));

        String report = "The EPS is 8.99 [1, 2].";
        String result = service.verifyCitations(report, citations);

        // 8.99 matches 1, but doesn't match 2
        assertThat(result).isEqualTo("The EPS is 8.99 [1, 2 ?].\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified. Citations marked with [!] reference missing or hallucinated sources.*");
    }

    @Test
    void testVerifyCitations_hallucinatedEpsNumberWithRevisionCount_markedUnverified() {
        Map<Integer, Object> citations = new HashMap<>();
        // Payload has 27 revisions, but annual EPS is 8.99, NOT 2.7462
        citations.put(319, Map.of(
                "eps_estimate_average", 8.99,
                "eps_estimate_revision_up_trailing_30_days", 27
        ));

        // Text claims EPS is 2.7462 and 27 revisions.
        // 2.7462 must NOT match 27 via power-of-10, and must NOT be marked valid just because 27 matched!
        String report = "Q4 EPS consensus was 2.7462 with 27 revisions [319].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).contains("[319 ?]");
    }

    @Test
    void testVerifyCitations_missingSource_markedBroken() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("revenue", 100));

        String report = "Revenue was 100 [999].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).contains("[999 !]");
    }
}
