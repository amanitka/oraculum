package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitationManagementService {

    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[([\\d,\\s\\?]+)\\]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
    private static final String FOOTNOTE_DISCLAIMER =
            "\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified against the raw numerical data.*";

    private final JsonMapper jsonMapper;

    public void injectUsedCitationsToTrace(AgentContext ctx) {
        if (ctx == null || ctx.state() == null) return;
        String fullTraceStr = JsonUtils.toJson(jsonMapper, ctx.state().getAgentTrace(), "");
        Map<Integer, Object> allCitations = ctx.state().getCitationRegistry().getCitations();
        Map<Integer, Object> prunedCitations = extractReferencedCitations(fullTraceStr, allCitations);

        ctx.state().putAgentTrace(AgentWorkflowState.TRACE_CITATIONS_KEY, prunedCitations);
        log.info("Injected {} active citations into agent trace (pruned from total {})",
                prunedCitations.size(), allCitations.size());
    }

    public Map<Integer, Object> extractReferencedCitations(String traceJson, Map<Integer, Object> allCitations) {
        Map<Integer, Object> referenced = new TreeMap<>();
        if (traceJson == null || allCitations == null || allCitations.isEmpty()) {
            return referenced;
        }

        Matcher m = BRACKET_PATTERN.matcher(traceJson);
        while (m.find()) {
            collectCitationIdsFromBracket(m.group(1), allCitations, referenced);
        }
        return referenced;
    }

    private void collectCitationIdsFromBracket(String bracketContent, Map<Integer, Object> allCitations,
                                               Map<Integer, Object> destination) {
        Matcher idMatcher = DIGIT_PATTERN.matcher(bracketContent);
        while (idMatcher.find()) {
            try {
                int id = Integer.parseInt(idMatcher.group());
                if (allCitations.containsKey(id)) {
                    destination.put(id, allCitations.get(id));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public AnalysisResult formatReportWithFootnote(AnalysisResult output) {
        if (output == null) return null;
        String report = output.executiveSummary();
        if (report != null && report.contains("?]") && !report.contains("*Note: Citations marked with [?]")) {
            return output.withExecutiveSummary(report + FOOTNOTE_DISCLAIMER);
        }
        return output;
    }
}
