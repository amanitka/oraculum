package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentEvidence;
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
        service = new CitationVerifierService(new ObjectMapper(), new FinancialMetricDictionary());
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
    void testVerifyCitations_dsoAndWorkingCapital() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(58, Map.of("daysSalesOutstanding", 59.8, "daysInventoryOutstanding", 119.65));

        String report = "DSO was 59.8 days and DIO reached 119.6 [58].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("DSO was 59.8 days and DIO reached 119.6 [58].");
    }

    @Test
    void testVerifyCitations_dsoMismatch_markedUnverified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(58, Map.of("daysSalesOutstanding", 59.8));

        String report = "DSO was 80.0 days [58].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).contains("[58 ?]");
    }

    @Test
    void testVerifyCitations_cashFlowStatementMetrics() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(93, Map.of(
                "Net Cash from Operating Activities", 50300000000.0,
                "Change in Fixed Assets & Intangibles", -7470000000.0
        ));

        String report = "Operating cash flow was 50.3B and Capex was 7.47B [93].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Operating cash flow was 50.3B and Capex was 7.47B [93].");
    }

    @Test
    void testVerifyCitations_secQualitativeFiling_validWithoutRatioNumbers() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(89, Map.of(
                "documentType", "SEC_10Q",
                "documentSubtype", "SEC_MD",
                "accessionNumber", "0001045810-26-000075",
                "filingDate", "2026-08-26"
        ));

        String report = "Management cited supply chain constraints for H100 in 2026 [89].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Management cited supply chain constraints for H100 in 2026 [89].");
    }

    @Test
    void testVerifyCitations_adjacentCitationsMultiplePeriods_bothVerified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(58, Map.of("revenueYoyGrowth", 1.0585));
        citations.put(59, Map.of("revenueYoyGrowth", 0.852));

        String report = "Revenue rose 105.9% YoY from 85.2% in Q1 [58][59].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Revenue rose 105.9% YoY from 85.2% in Q1 [58][59].");
    }

    @Test
    void testVerifyCitations_proximityContextSeparatedCitations() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(58, Map.of("revenueYoyGrowth", 1.0585));
        citations.put(59, Map.of("revenueYoyGrowth", 0.852));

        String report = "Revenue rose 105.9% YoY [58], up from 85.2% in Q1 [59].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Revenue rose 105.9% YoY [58], up from 85.2% in Q1 [59].");
    }

    @Test
    void testVerifyCitations_multipleCitationsInSingleBracket() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(1, Map.of("eps", 8.99));
        citations.put(2, Map.of("eps", 12.87));

        String report = "The EPS is 8.99 [1, 2].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("The EPS is 8.99 [1, 2 ?].\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified. Citations marked with [!] reference missing or hallucinated sources.*");
    }

    @Test
    void testVerifyCitations_hallucinatedEpsNumberWithRevisionCount_markedUnverified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(319, Map.of(
                "eps_estimate_average", 8.99,
                "eps_estimate_revision_up_trailing_30_days", 27
        ));

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

    @Test
    void testVerifyCitations_nvdaCompoundRevenueSentence_allVerified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(58, Map.of(
                "revenue", 96221000000.0,
                "revenueYoyGrowth", 1.0585114
        ));
        citations.put(89, Map.of(
                "documentType", "SEC_10Q",
                "documentSubtype", "SEC_MD"
        ));
        citations.put(301, Map.of(
                "documentType", "SEC_8K",
                "documentSubtype", "SEC_EX99_1"
        ));

        String report = "Quarter ended July 2026 revenue reached $96.2B, up 105.9% YoY, with Data Center revenue up 117% YoY and next-quarter guidance of $108.0B ±2% [58][89][301].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Quarter ended July 2026 revenue reached $96.2B, up 105.9% YoY, with Data Center revenue up 117% YoY and next-quarter guidance of $108.0B ±2% [58][89][301].");
    }

    @Test
    void testVerifyCitations_macroCompoundSentence_allVerified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(6, Map.of("indicator", "DGORDER", "yoyChangePct", 8.432, "latestValue", 338458.0));
        citations.put(1, Map.of("indicator", "INDPRO", "yoyChangePct", 2.245, "latestValue", 103.9506));

        String report = "Durable goods orders rose 8.43% YoY and industrial production rose 2.25% YoY [6][1].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Durable goods orders rose 8.43% YoY and industrial production rose 2.25% YoY [6][1].");
    }

    @Test
    void testVerifyCitations_workingCapitalMultiPeriod_allVerified() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(113, Map.of("daysSalesOutstanding", 75.96993, "daysInventoryOutstanding", 150.2043));
        citations.put(114, Map.of("daysSalesOutstanding", 58.618057, "daysInventoryOutstanding", 143.66873));

        String report = "TTM days sales outstanding rose to 75.97 days from 58.62 days, and TTM days inventory outstanding rose to 150.20 days from 143.67 days [113][114].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("TTM days sales outstanding rose to 75.97 days from 58.62 days, and TTM days inventory outstanding rose to 150.20 days from 143.67 days [113][114].");
    }

    @Test
    void testVerifyRecordCitations_agentEvidence_supportedByValue_noQuestionMark() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(2, Map.of("gross_margin", 0.753));

        AgentEvidence ev = new AgentEvidence(
                "Gross Margin",
                "75.3%",
                "70.1%",
                "IMPROVING",
                "CALCULATED",
                "Gross margin expanded by 5.2 percentage points YoY [2]."
        );

        AgentEvidence verified = service.verifyRecordCitations(ev, citations, null);
        assertThat(verified.significance()).isEqualTo("Gross margin expanded by 5.2 percentage points YoY [2].");
    }

    @Test
    void testVerifyCitations_singleCitationComparative_noContradiction() {
        Map<Integer, Object> citations = new HashMap<>();
        citations.put(2, Map.of("gross_margin", 0.753));

        String report = "Gross margin expanded to 75.3% from 70.1% [2].";
        String result = service.verifyCitations(report, citations);

        assertThat(result).isEqualTo("Gross margin expanded to 75.3% from 70.1% [2].");
    }
}
