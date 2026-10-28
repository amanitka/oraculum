package com.oraculum.analyst.service.citation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialMetricDictionaryTest {

    private FinancialMetricDictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new FinancialMetricDictionary();
    }

    @Test
    void testGetCandidateProperties_workingCapital() {
        List<String> props = dictionary.getCandidateProperties("The DSO jumped to 59.8 days while DIO was 119.6");

        assertThat(props).contains("daysSalesOutstanding", "daysInventoryOutstanding");
    }

    @Test
    void testGetCandidateProperties_cashFlow() {
        List<String> props = dictionary.getCandidateProperties("Strong operating cash flow and high capex");

        assertThat(props).contains("Net Cash from Operating Activities", "Change in Fixed Assets & Intangibles");
    }

    @Test
    void testGetCandidateProperties_consensusRevisions() {
        List<String> props = dictionary.getCandidateProperties("Analyst revisions up were 27 while EPS estimate was $8.99");

        assertThat(props).contains("eps_estimate_revision_up_trailing_30_days", "eps_estimate_average");
    }

    @Test
    void testGetCandidateProperties_emptyOrNull() {
        assertThat(dictionary.getCandidateProperties(null)).isEmpty();
        assertThat(dictionary.getCandidateProperties("   ")).isEmpty();
        assertThat(dictionary.getCandidateProperties("The weather in California is sunny")).isEmpty();
    }
}
