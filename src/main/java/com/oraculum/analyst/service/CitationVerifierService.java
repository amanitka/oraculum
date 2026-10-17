package com.oraculum.analyst.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitationVerifierService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\d,\\s]+)\\]");
    private static final Pattern PRECEDING_CLUSTER_PATTERN = Pattern.compile("(?:\\[[\\d,\\s]+\\][\\s,;]*)+$");
    private static final Pattern TRAILING_CLUSTER_PATTERN = Pattern.compile("^[\\s,;]*\\[[\\d,\\s]+\\]");
    private static final Pattern CLAUSE_SPLIT_PATTERN = Pattern.compile(
            "(?:,|;)?\\s*\\b(?:and|while|whereas|with|but|versus|vs\\.?)\\b|;",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:,\\d{3})*(?:\\.\\d+)?)");
    private static final Pattern NEGATIVE_WORD_PATTERN = Pattern.compile(
            "\\b(fell|dropped|declined|decreased|down|loss)\\b"
    );
    private static final Pattern LEVEL_PREPOSITION_PATTERN = Pattern.compile("\\b(from|to)\\b");
    private static final Pattern MONTH_PREFIX_PATTERN = Pattern.compile(
            ".*\\b(january|february|march|april|may|june|july|august|september|october|november|december|"
                    + "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\.?\\s+$"
    );
    private static final Pattern ABBREVIATION_SUFFIX_PATTERN = Pattern.compile(
            ".*\\b(vs|u\\.s|inc|corp|co|ltd|est|approx|no|fig)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_METRIC_SUFFIX_PATTERN = Pattern.compile(
            "^(?:-[kqf]|(?:st|nd|rd|th|nm)\\b|[qh]\\d).*"
    );

    private static final int MAX_CONTEXT_LENGTH = 400;
    private static final double[] VALID_SCALES = {
            1.0, 100.0, 1_000.0, 10_000.0, 1_000_000.0, 1_000_000_000.0, 1_000_000_000_000.0
    };
    private static final double MATCH_TOLERANCE = 0.03;

    private final ObjectMapper objectMapper;
    private final FinancialMetricDictionary metricDictionary;

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
        boolean hasUnverified = false;
        boolean hasBroken = false;

        while (matcher.find()) {
            String[] ids = matcher.group(1).split(",");
            boolean singleCitation = isSingleCitation(ids, reportMd, matcher.start(), matcher.end());
            String context = extractContext(reportMd, matcher.start());
            List<ContextClaim> claims = extractClaimsFromContext(context);
            List<String> validatedIds = validateCitationIds(ids, claims, citations, metrics, singleCitation);

            hasUnverified |= validatedIds.stream().anyMatch(id -> id.endsWith("?"));
            hasBroken |= validatedIds.stream().anyMatch(id -> id.endsWith("!"));

            corrected.append(reportMd, lastIndex, matcher.start());
            corrected.append("[").append(String.join(", ", validatedIds)).append("]");
            lastIndex = matcher.end();
        }
        corrected.append(reportMd.substring(lastIndex));
        appendVerificationFootnote(corrected, appendFootnote, hasUnverified, hasBroken);
        return corrected.toString();
    }

    private void appendVerificationFootnote(StringBuilder report, boolean appendFootnote,
                                            boolean hasUnverified, boolean hasBroken) {
        if ((hasUnverified || hasBroken) && appendFootnote) {
            report.append("\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims ")
                    .append("that could not be strictly verified. Citations marked with [!] reference ")
                    .append("missing or hallucinated sources.*");
        }
    }

    public <T> T verifyRecordCitations(T record, Map<Integer, Object> citations, CitationMetrics metrics) {
        if (record == null || !record.getClass().isRecord()) {
            return record;
        }
        try {
            return buildVerifiedRecord(record, record.getClass(), citations, metrics);
        } catch (Exception e) {
            log.error("Failed to verify record citations for {}", record.getClass().getName(), e);
            return record;
        }
    }

    private <T> T buildVerifiedRecord(T record, Class<?> clazz,
                                      Map<Integer, Object> citations, CitationMetrics metrics) throws Exception {
        RecordComponent[] components = clazz.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            Method accessor = components[i].getAccessor();
            args[i] = verifyComponentValue(accessor.invoke(record), citations, metrics);
        }
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return (T) constructor.newInstance(args);
    }

    private Object verifyComponentValue(Object value, Map<Integer, Object> citations, CitationMetrics metrics) {
        if (value instanceof String str) {
            return verifyCitations(str, citations, false, metrics);
        }
        if (value instanceof List<?> list) {
            return verifyListCitations(list, citations, metrics);
        }
        if (value != null && value.getClass().isRecord()) {
            return verifyRecordCitations(value, citations, metrics);
        }
        return value;
    }

    private List<Object> verifyListCitations(List<?> list, Map<Integer, Object> citations, CitationMetrics metrics) {
        List<Object> verifiedList = new ArrayList<>(list.size());
        for (Object item : list) {
            verifiedList.add(verifyComponentValue(item, citations, metrics));
        }
        return verifiedList;
    }

    private String extractContext(String reportMd, int matchStart) {
        int clusterStart = findClusterStart(reportMd, matchStart);
        int contextStart = Math.max(0, clusterStart - MAX_CONTEXT_LENGTH);
        int periodBoundary = findSentenceBoundary(reportMd, clusterStart);
        int newlineBoundary = reportMd.lastIndexOf('\n', clusterStart - 1) + 1;
        int bracketBoundary = findPreviousClauseBracket(reportMd, clusterStart);

        int safeStart = Math.max(contextStart, Math.max(periodBoundary, Math.max(newlineBoundary, bracketBoundary)));
        return reportMd.substring(safeStart, clusterStart);
    }

    private int findClusterStart(String reportMd, int matchStart) {
        int lookbackStart = Math.max(0, matchStart - 120);
        String prefix = reportMd.substring(lookbackStart, matchStart);
        Matcher matcher = PRECEDING_CLUSTER_PATTERN.matcher(prefix);
        if (matcher.find()) {
            return lookbackStart + matcher.start();
        }
        return matchStart;
    }

    private boolean isSingleCitation(String[] ids, String reportMd, int matchStart, int matchEnd) {
        return ids.length == 1
                && findClusterStart(reportMd, matchStart) == matchStart
                && !hasTrailingCluster(reportMd, matchEnd);
    }

    private boolean hasTrailingCluster(String reportMd, int matchEnd) {
        int lookaheadEnd = Math.min(reportMd.length(), matchEnd + 120);
        String suffix = reportMd.substring(matchEnd, lookaheadEnd);
        return TRAILING_CLUSTER_PATTERN.matcher(suffix).find();
    }

    private int findSentenceBoundary(String reportMd, int limit) {
        int searchPos = limit - 1;
        while (searchPos >= 0) {
            int periodIdx = reportMd.lastIndexOf(". ", searchPos);
            if (periodIdx == -1) {
                return 0;
            }
            String beforePeriod = reportMd.substring(Math.max(0, periodIdx - 10), periodIdx);
            if (!ABBREVIATION_SUFFIX_PATTERN.matcher(beforePeriod).matches()) {
                return periodIdx + 2;
            }
            searchPos = periodIdx - 1;
        }
        return 0;
    }

    private int findPreviousClauseBracket(String reportMd, int clusterStart) {
        int prevBracketIdx = reportMd.lastIndexOf(']', clusterStart - 1);
        if (prevBracketIdx != -1 && prevBracketIdx < clusterStart - 1) {
            String between = reportMd.substring(prevBracketIdx + 1, clusterStart).trim();
            if (!between.isEmpty() && !between.matches("^[\\[\\]\\s,;]+$")) {
                return prevBracketIdx + 1;
            }
        }
        return 0;
    }

    private List<ContextClaim> extractClaimsFromContext(String context) {
        String cleanContext = CITATION_PATTERN.matcher(context).replaceAll(" ");
        String[] clauses = CLAUSE_SPLIT_PATTERN.split(cleanContext);
        if (clauses.length <= 1) {
            List<String> targets = getSafeCandidateProperties(context);
            return extractNumbersFromText(cleanContext).stream()
                    .map(num -> new ContextClaim(num, targets))
                    .toList();
        }
        return buildMultiClauseClaims(clauses, context);
    }

    private List<ContextClaim> buildMultiClauseClaims(String[] clauses, String fullContext) {
        List<ContextClaim> claims = new ArrayList<>();
        List<String> pendingTargets = List.of();
        boolean anyTargetsMatched = false;

        for (String clause : clauses) {
            List<Double> numbers = extractNumbersFromText(clause);
            List<String> targets = getSafeCandidateProperties(clause);
            anyTargetsMatched |= !targets.isEmpty();

            if (numbers.isEmpty()) {
                if (!targets.isEmpty()) pendingTargets = targets;
            } else {
                List<String> effectiveTargets = !targets.isEmpty() ? targets : pendingTargets;
                numbers.forEach(num -> claims.add(new ContextClaim(num, effectiveTargets)));
                pendingTargets = List.of();
            }
        }
        return applyFallbackTargetsIfNeeded(claims, anyTargetsMatched, fullContext);
    }

    private List<ContextClaim> applyFallbackTargetsIfNeeded(List<ContextClaim> claims,
                                                            boolean anyTargetsMatched, String fullContext) {
        if (anyTargetsMatched || claims.isEmpty()) {
            return claims;
        }
        List<String> fullTargets = getSafeCandidateProperties(fullContext);
        if (fullTargets.isEmpty()) {
            return claims;
        }
        return claims.stream().map(c -> new ContextClaim(c.value(), fullTargets)).toList();
    }

    private List<String> getSafeCandidateProperties(String text) {
        List<String> properties = metricDictionary.getCandidateProperties(text);
        return properties != null ? properties : List.of();
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
        if (isQualitativeFiling(root)) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        return verifyQuantitativePayload(id, root, claims, metrics, isSingleCitation);
    }

    private String verifyQuantitativePayload(int id, JsonNode root,
                                             List<ContextClaim> claims, CitationMetrics metrics,
                                             boolean isSingleCitation) {
        List<Double> allJsonNumbers = new ArrayList<>();
        extractNumbersFromJson(root, allJsonNumbers, List.of());

        boolean verified = claims.stream()
                .anyMatch(claim -> isClaimVerified(claim, root, allJsonNumbers));

        if (verified && (!isSingleCitation || !hasContradictedClaim(claims, root))) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        if (metrics != null) metrics.incrementUnverified();
        log.debug("Citation mismatch detected: ID {} payload did not verify claims {}", id, claims);
        return id + " ?";
    }

    private boolean hasContradictedClaim(List<ContextClaim> claims, JsonNode root) {
        for (ContextClaim claim : claims) {
            if (!claim.targetFields().isEmpty() && isIndicatorPayloadApplicable(root, claim.targetFields())) {
                List<Double> targetedNumbers = new ArrayList<>();
                extractNumbersFromJson(root, targetedNumbers, claim.targetFields());
                if (!targetedNumbers.isEmpty() && targetedNumbers.stream().noneMatch(tNum -> isMatch(claim.value(), tNum))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isClaimVerified(ContextClaim claim, JsonNode root, List<Double> allJsonNumbers) {
        if (!claim.targetFields().isEmpty()) {
            if (!isIndicatorPayloadApplicable(root, claim.targetFields())) {
                return false;
            }
            List<Double> targetedNumbers = new ArrayList<>();
            extractNumbersFromJson(root, targetedNumbers, claim.targetFields());
            if (!targetedNumbers.isEmpty()) {
                return targetedNumbers.stream().anyMatch(tNum -> isMatch(claim.value(), tNum));
            }
        }
        return allJsonNumbers.stream().anyMatch(jNum -> isMatch(claim.value(), jNum));
    }

    private boolean isIndicatorPayloadApplicable(JsonNode root, List<String> targetFields) {
        if (!root.has("indicator") || targetFields.isEmpty()) {
            return true;
        }
        String ind = root.get("indicator").asString().toLowerCase();
        String title = root.has("indicatorTitle") ? root.get("indicatorTitle").asString().toLowerCase() : "";
        return targetFields.stream().anyMatch(t -> {
            String lowerT = t.toLowerCase();
            return ind.contains(lowerT) || title.contains(lowerT) || lowerT.contains(ind);
        });
    }

    private boolean isQualitativeFiling(JsonNode root) {
        if (!root.has("documentSubtype") && !root.has("documentType")) {
            return false;
        }
        return !root.has("peRatio") && !root.has("grossMargin")
                && !root.has("daysSalesOutstanding") && !root.has("totalRevenue");
    }

    private List<Double> extractNumbersFromText(String text) {
        List<Double> numbers = new ArrayList<>();
        String lowerContext = text.toLowerCase();
        Matcher matcher = NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {
            parseAndCollectNumber(matcher.group(1), matcher.start(), matcher.end(), lowerContext, numbers);
        }
        return numbers;
    }

    private void parseAndCollectNumber(String numStr, int matchStart, int matchEnd,
                                       String lowerContext, List<Double> numbers) {
        try {
            double val = Double.parseDouble(numStr.replace(",", ""));
            if (isIgnoredTokenNumber(numStr, val, matchStart, matchEnd, lowerContext)) {
                return;
            }
            if (val > 0) {
                String prefix = lowerContext.substring(Math.max(0, matchStart - 40), matchStart);
                if (shouldNegateNumber(prefix)) {
                    val = -val;
                }
            }
            numbers.add(val);
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean isIgnoredTokenNumber(String numStr, double val, int matchStart,
                                         int matchEnd, String lowerContext) {
        boolean isInteger = numStr.indexOf('.') == -1;
        if (isInteger && ((val >= 1900 && val <= 2100) || (val >= 0 && val <= 4))) {
            return true;
        }
        if (matchStart > 0 && Character.isLetter(lowerContext.charAt(matchStart - 1))) {
            return true;
        }
        String suffix = lowerContext.substring(matchEnd, Math.min(lowerContext.length(), matchEnd + 4));
        if (NON_METRIC_SUFFIX_PATTERN.matcher(suffix).matches()) {
            return true;
        }
        return isInteger && val >= 1 && val <= 31 && !suffix.startsWith("%")
                && MONTH_PREFIX_PATTERN.matcher(lowerContext.substring(0, matchStart)).matches();
    }

    private boolean shouldNegateNumber(String prefix) {
        Matcher matcher = NEGATIVE_WORD_PATTERN.matcher(prefix);
        int lastEnd = -1;
        while (matcher.find()) {
            lastEnd = matcher.end();
        }
        return lastEnd != -1 && !LEVEL_PREPOSITION_PATTERN.matcher(prefix.substring(lastEnd)).find();
    }

    private void extractNumbersFromJson(JsonNode node, List<Double> numbers, List<String> targetFields) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                if (isTargetProperty(entry.getKey(), targetFields)) {
                    extractFieldNumbers(entry.getKey(), entry.getValue(), numbers);
                } else if (entry.getValue().isObject() || entry.getValue().isArray()) {
                    extractNumbersFromJson(entry.getValue(), numbers, targetFields);
                }
            });
        } else if (node.isArray()) {
            node.forEach(item -> extractNumbersFromJson(item, numbers, targetFields));
        }
    }

    private boolean isTargetProperty(String key, List<String> targetFields) {
        if (targetFields.isEmpty()) {
            return true;
        }
        String lowerKey = key.toLowerCase();
        return targetFields.stream()
                .anyMatch(t -> lowerKey.contains(t.toLowerCase()));
    }

    private void extractFieldNumbers(String key, JsonNode value, List<Double> numbers) {
        if (value.isNumber()) {
            double num = value.asDouble();
            numbers.add(num);
            if (isDirectionalCashFlowProperty(key)) {
                numbers.add(Math.abs(num));
            }
        } else if (value.isString()) {
            numbers.addAll(extractNumbersFromText(value.asString()));
        }
    }

    private boolean isDirectionalCashFlowProperty(String key) {
        String lower = key.toLowerCase();
        return lower.contains("fixed assets") || lower.contains("capex")
                || lower.contains("capitalexpenditure") || lower.contains("dividends")
                || lower.contains("repurchase") || lower.contains("buyback")
                || lower.contains("sellsvalue") || lower.contains("working capital");
    }

    private boolean isMatch(double contextVal, double jsonVal) {
        if (jsonVal == 0.0) {
            return Math.abs(contextVal) < 0.0001;
        }
        for (double scale : VALID_SCALES) {
            double scaledJson = jsonVal * scale;
            if (scaledJson != 0 && Math.abs(contextVal - scaledJson) / Math.abs(scaledJson) <= MATCH_TOLERANCE) {
                return true;
            }
            double invScaledJson = jsonVal / scale;
            if (invScaledJson != 0 && Math.abs(contextVal - invScaledJson) / Math.abs(invScaledJson) <= MATCH_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private record ContextClaim(double value, List<String> targetFields) {
    }
}
