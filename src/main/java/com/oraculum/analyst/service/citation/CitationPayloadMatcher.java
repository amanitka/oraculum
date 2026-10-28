package com.oraculum.analyst.service.citation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CitationPayloadMatcher {

    private static final double[] VALID_SCALES = {
            1.0, 100.0, 1_000.0, 10_000.0, 1_000_000.0, 1_000_000_000.0, 1_000_000_000_000.0
    };
    private static final double MATCH_TOLERANCE = 0.03;

    private final CitationClaimExtractor claimExtractor;

    public boolean isQualitativeFiling(JsonNode root) {
        if (!root.has("documentSubtype") && !root.has("documentType")) {
            return false;
        }
        return !root.has("peRatio") && !root.has("grossMargin")
                && !root.has("daysSalesOutstanding") && !root.has("totalRevenue");
    }

    public boolean isEvidenceSupportedByPayload(JsonNode root, List<Double> evidenceNumbers) {
        if (evidenceNumbers.isEmpty()) {
            return false;
        }
        List<Double> allJsonNumbers = new ArrayList<>();
        extractNumbersFromJson(root, allJsonNumbers, List.of());
        return evidenceNumbers.stream()
                .anyMatch(eNum -> allJsonNumbers.stream().anyMatch(jNum -> isMatch(eNum, jNum)));
    }

    public boolean isQuantitativePayloadVerified(JsonNode root, List<ContextClaim> claims, boolean isSingleCitation) {
        List<Double> allJsonNumbers = new ArrayList<>();
        extractNumbersFromJson(root, allJsonNumbers, List.of());

        boolean verified = claims.stream()
                .anyMatch(claim -> isClaimVerified(claim, root, allJsonNumbers));

        return verified && (!isSingleCitation || !hasContradictedClaim(claims, root));
    }

    private boolean hasContradictedClaim(List<ContextClaim> claims, JsonNode root) {
        Map<String, List<ContextClaim>> claimsByTarget = groupClaimsByTarget(claims);
        for (Map.Entry<String, List<ContextClaim>> entry : claimsByTarget.entrySet()) {
            if (isTargetGroupContradicted(entry.getKey(), entry.getValue(), root)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<ContextClaim>> groupClaimsByTarget(List<ContextClaim> claims) {
        Map<String, List<ContextClaim>> claimsByTarget = new HashMap<>();
        for (ContextClaim claim : claims) {
            for (String target : claim.targetFields()) {
                claimsByTarget.computeIfAbsent(target, _ -> new ArrayList<>()).add(claim);
            }
        }
        return claimsByTarget;
    }

    private boolean isTargetGroupContradicted(String target, List<ContextClaim> groupClaims, JsonNode root) {
        List<String> targetList = List.of(target);
        if (!isIndicatorPayloadApplicable(root, targetList)) {
            return false;
        }
        List<Double> targetedNumbers = new ArrayList<>();
        extractNumbersFromJson(root, targetedNumbers, targetList);
        if (targetedNumbers.isEmpty()) {
            return false;
        }
        return groupClaims.stream()
                .noneMatch(c -> targetedNumbers.stream().anyMatch(tNum -> isMatch(c.value(), tNum)));
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
        return targetFields.stream().anyMatch(t -> lowerKey.contains(t.toLowerCase()));
    }

    private void extractFieldNumbers(String key, JsonNode value, List<Double> numbers) {
        if (value.isNumber()) {
            double num = value.asDouble();
            numbers.add(num);
            if (isDirectionalCashFlowProperty(key)) {
                numbers.add(Math.abs(num));
            }
        } else if (value.isString()) {
            numbers.addAll(claimExtractor.extractNumbersFromText(value.asString()));
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
}
