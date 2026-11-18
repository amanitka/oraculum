package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.ui.views.components.export.AnalysisPdfCitationHelper.CitationEntry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPdfCitationHelperTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void collectReferencedCitations_whenCitationsExist_returnsOnlyReferencedInOrder() {
        JsonNode citations = jsonMapper.readTree("""
                {
                  "1": { "_source": "SEC 10-K", "_variant": "FY2025", "revenue": 391035000000 },
                  "2": { "_source": "Share Price", "close": 210.96 },
                  "99": { "_source": "Unused Source", "note": "should be omitted" }
                }
                """);
        AnalysisResult result = AnalysisResult.builder()
                .thesis("Strong cash generation [2].")
                .executiveSummary("Revenue reached record highs [1, 2 ?\u26A0\uFE0F].")
                .build();

        List<CitationEntry> entries = AnalysisPdfCitationHelper.collectReferencedCitations(result, citations);

        assertThat(entries).extracting(CitationEntry::id).containsExactly("2", "1");
        assertThat(entries.getFirst().source()).isEqualTo("Share Price");
        assertThat(entries.get(1).source()).isEqualTo("SEC 10-K (FY2025)");
    }

    @Test
    void collectReferencedCitations_whenCitationsNull_returnsEmptyList() {
        AnalysisResult result = AnalysisResult.builder().thesis("Some claim [1].").build();

        assertThat(AnalysisPdfCitationHelper.collectReferencedCitations(result, null)).isEmpty();
        assertThat(AnalysisPdfCitationHelper.renderCitationsAppendix(List.of())).isEmpty();
    }

    @Test
    void formatPdfCitation_formatsVerifiableAndUnverifiableLinks() {
        assertThat(AnalysisPdfCitationHelper.formatPdfCitation("49", ""))
                .isEqualTo("<sup class=\"cite\"><a class=\"cite-link\" href=\"#cite-49\">49</a></sup>");
        assertThat(AnalysisPdfCitationHelper.formatPdfCitation("50", "?"))
                .isEqualTo("<sup class=\"cite cite-unverifiable\"><a class=\"cite-link\" href=\"#cite-50\">50 ?</a></sup>");
    }
}
