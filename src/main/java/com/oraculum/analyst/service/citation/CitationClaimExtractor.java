package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentEvidence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CitationClaimExtractor {

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

    private final FinancialMetricDictionary metricDictionary;

    public boolean isSingleCitation(String[] ids, String reportMd, int matchStart, int matchEnd) {
        return ids.length == 1
                && findClusterStart(reportMd, matchStart) == matchStart
                && !hasTrailingCluster(reportMd, matchEnd);
    }

    public String extractContext(String reportMd, int matchStart) {
        int clusterStart = findClusterStart(reportMd, matchStart);
        int contextStart = Math.max(0, clusterStart - MAX_CONTEXT_LENGTH);
        int periodBoundary = findSentenceBoundary(reportMd, clusterStart);
        int newlineBoundary = reportMd.lastIndexOf('\n', clusterStart - 1) + 1;
        int bracketBoundary = findPreviousClauseBracket(reportMd, clusterStart);

        int safeStart = Math.max(contextStart, Math.max(periodBoundary, Math.max(newlineBoundary, bracketBoundary)));
        return reportMd.substring(safeStart, clusterStart);
    }

    public List<ContextClaim> extractClaimsFromContext(String context) {
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

    public List<Double> extractEvidenceNumbers(AgentEvidence ev) {
        List<Double> nums = new ArrayList<>();
        if (ev.value() != null) nums.addAll(extractNumbersFromText(ev.value()));
        if (ev.previousValue() != null) nums.addAll(extractNumbersFromText(ev.previousValue()));
        if (nums.size() >= 2) {
            double diff = Math.abs(nums.get(0) - nums.get(1));
            nums.add(diff);
            if (Math.abs(nums.get(1)) > 1e-4) {
                nums.add((diff / Math.abs(nums.get(1))) * 100.0);
            }
        }
        return nums;
    }

    public List<Double> extractNumbersFromText(String text) {
        List<Double> numbers = new ArrayList<>();
        String lowerContext = text.toLowerCase();
        Matcher matcher = NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {
            parseAndCollectNumber(matcher.group(1), matcher.start(), matcher.end(), lowerContext, numbers);
        }
        return numbers;
    }

    private int findClusterStart(String reportMd, int matchStart) {
        int lookbackStart = Math.max(0, matchStart - 120);
        String prefix = reportMd.substring(lookbackStart, matchStart);
        Matcher matcher = PRECEDING_CLUSTER_PATTERN.matcher(prefix);
        return matcher.find() ? lookbackStart + matcher.start() : matchStart;
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
}
