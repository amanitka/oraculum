package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentEvidence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CitationVerifierService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\d,\\s]+)\\]");

    private final ObjectMapper objectMapper;
    private final CitationClaimExtractor claimExtractor;
    private final CitationPayloadMatcher payloadMatcher;
    private final RecordCitationWalker recordWalker;

    @Autowired
    public CitationVerifierService(ObjectMapper objectMapper,
                                   CitationClaimExtractor claimExtractor,
                                   CitationPayloadMatcher payloadMatcher,
                                   RecordCitationWalker recordWalker) {
        this.objectMapper = objectMapper;
        this.claimExtractor = claimExtractor;
        this.payloadMatcher = payloadMatcher;
        this.recordWalker = recordWalker;
    }

    public CitationVerifierService(ObjectMapper objectMapper, FinancialMetricDictionary metricDictionary) {
        CitationClaimExtractor extractor = new CitationClaimExtractor(metricDictionary);
        this.objectMapper = objectMapper;
        this.claimExtractor = extractor;
        this.payloadMatcher = new CitationPayloadMatcher(extractor);
        this.recordWalker = new RecordCitationWalker();
    }

    public String verifyCitations(String reportMd, Map<Integer, Object> citations) {
        return verifyCitations(reportMd, citations, true, null);
    }

    public String verifyCitations(String reportMd, Map<Integer, Object> citations,
                                  boolean appendFootnote, CitationMetrics metrics) {
        if (reportMd == null || citations == null || citations.isEmpty()) {
            return reportMd;
        }
        StringBuilder corrected = new StringBuilder();
        Matcher matcher = CITATION_PATTERN.matcher(reportMd);
        int lastIndex = 0;
        boolean hasFlags = false;

        while (matcher.find()) {
            List<String> validatedIds = verifyMatchCluster(reportMd, matcher, citations, metrics);
            hasFlags |= validatedIds.stream().anyMatch(id -> id.endsWith("?") || id.endsWith("!"));
            corrected.append(reportMd, lastIndex, matcher.start());
            corrected.append("[").append(String.join(", ", validatedIds)).append("]");
            lastIndex = matcher.end();
        }
        corrected.append(reportMd.substring(lastIndex));
        appendVerificationFootnote(corrected, appendFootnote, hasFlags);
        return corrected.toString();
    }

    public <T> T verifyRecordCitations(T record, Map<Integer, Object> citations, CitationMetrics metrics) {
        return recordWalker.walkRecord(
                record,
                str -> verifyCitations(str, citations, false, metrics),
                ev -> verifyAgentEvidence(ev, citations, metrics)
        );
    }

    private List<String> verifyMatchCluster(String reportMd, Matcher matcher,
                                            Map<Integer, Object> citations, CitationMetrics metrics) {
        String[] ids = matcher.group(1).split(",");
        boolean singleCitation = claimExtractor.isSingleCitation(ids, reportMd, matcher.start(), matcher.end());
        String context = claimExtractor.extractContext(reportMd, matcher.start());
        List<ContextClaim> claims = claimExtractor.extractClaimsFromContext(context);
        return validateCitationIds(ids, claims, citations, metrics, singleCitation);
    }

    private void appendVerificationFootnote(StringBuilder report, boolean appendFootnote, boolean hasFlags) {
        if (hasFlags && appendFootnote) {
            report.append("\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims ")
                    .append("that could not be strictly verified. Citations marked with [!] reference ")
                    .append("missing or hallucinated sources.*");
        }
    }

    private AgentEvidence verifyAgentEvidence(AgentEvidence ev, Map<Integer, Object> citations, CitationMetrics metrics) {
        if (ev == null || ev.significance() == null) {
            return ev;
        }
        String verifiedSignificance = verifyEvidenceSignificance(ev, citations, metrics);
        return new AgentEvidence(ev.metric(), ev.value(), ev.previousValue(), ev.trend(), ev.sourceType(), verifiedSignificance);
    }

    private String verifyEvidenceSignificance(AgentEvidence ev, Map<Integer, Object> citations, CitationMetrics metrics) {
        Matcher matcher = CITATION_PATTERN.matcher(ev.significance());
        if (!matcher.find()) {
            return ev.significance();
        }
        List<Double> evidenceNumbers = claimExtractor.extractEvidenceNumbers(ev);
        StringBuilder corrected = new StringBuilder();
        int lastIndex = 0;
        matcher.reset();
        while (matcher.find()) {
            String[] ids = matcher.group(1).split(",");
            List<String> validatedIds = validateEvidenceCitationIds(ids, ev, evidenceNumbers, citations, metrics);
            corrected.append(ev.significance(), lastIndex, matcher.start());
            corrected.append("[").append(String.join(", ", validatedIds)).append("]");
            lastIndex = matcher.end();
        }
        corrected.append(ev.significance().substring(lastIndex));
        return corrected.toString();
    }

    private List<String> validateEvidenceCitationIds(String[] ids, AgentEvidence ev, List<Double> evidenceNumbers,
                                                     Map<Integer, Object> citations, CitationMetrics metrics) {
        List<String> validated = new ArrayList<>(ids.length);
        for (String idStr : ids) {
            validated.add(validateSingleEvidenceCitation(idStr.trim(), ev, evidenceNumbers, citations, metrics));
        }
        return validated;
    }

    private String validateSingleEvidenceCitation(String idStr, AgentEvidence ev, List<Double> evidenceNumbers,
                                                  Map<Integer, Object> citations, CitationMetrics metrics) {
        try {
            int id = Integer.parseInt(idStr);
            if (!citations.containsKey(id)) {
                if (metrics != null) metrics.incrementBroken();
                return id + " !";
            }
            JsonNode root = objectMapper.valueToTree(citations.get(id));
            if (payloadMatcher.isEvidenceSupportedByPayload(root, evidenceNumbers)) {
                if (metrics != null) metrics.incrementValid();
                return String.valueOf(id);
            }
            List<ContextClaim> claims = claimExtractor.extractClaimsFromContext(ev.significance());
            return verifyQuantitativePayload(id, root, claims, metrics, false);
        } catch (NumberFormatException e) {
            return idStr;
        }
    }

    private List<String> validateCitationIds(String[] ids, List<ContextClaim> claims,
                                             Map<Integer, Object> citations, CitationMetrics metrics,
                                             boolean isSingleCitation) {
        List<String> validatedIds = new ArrayList<>(ids.length);
        for (String idStr : ids) {
            validatedIds.add(validateSingleCitation(idStr.trim(), claims, citations, metrics, isSingleCitation));
        }
        return validatedIds;
    }

    private String validateSingleCitation(String idStr, List<ContextClaim> claims,
                                          Map<Integer, Object> citations, CitationMetrics metrics,
                                          boolean isSingleCitation) {
        try {
            int id = Integer.parseInt(idStr);
            return checkCitation(id, claims, citations, metrics, isSingleCitation);
        } catch (NumberFormatException e) {
            return idStr;
        }
    }

    private String checkCitation(int id, List<ContextClaim> claims,
                                 Map<Integer, Object> citations, CitationMetrics metrics,
                                 boolean isSingleCitation) {
        if (!citations.containsKey(id)) {
            if (metrics != null) metrics.incrementBroken();
            return id + " !";
        }
        if (claims.isEmpty()) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        JsonNode root = objectMapper.valueToTree(citations.get(id));
        if (payloadMatcher.isQualitativeFiling(root)) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        return verifyQuantitativePayload(id, root, claims, metrics, isSingleCitation);
    }

    private String verifyQuantitativePayload(int id, JsonNode root,
                                             List<ContextClaim> claims, CitationMetrics metrics,
                                             boolean isSingleCitation) {
        if (payloadMatcher.isQuantitativePayloadVerified(root, claims, isSingleCitation)) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        if (metrics != null) metrics.incrementUnverified();
        log.debug("Citation mismatch detected: ID {} payload did not verify claims {}", id, claims);
        return id + " ?";
    }
}
