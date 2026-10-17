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
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:,\\d{3})*(?:\\.\\d+)?)");
    private static final double[] VALID_SCALES = {
            1.0, 100.0, 1_000.0, 1_000_000.0, 1_000_000_000.0, 1_000_000_000_000.0
    };
    private static final double MATCH_TOLERANCE = 0.03;

    private static final Map<String, List<String>> TRIGGER_WORDS = Map.of(
            "p/e", List.of("peRatio", "priceEarningsRatio", "forwardPE", "trailingPE"),
            "margin", List.of("grossMargin", "operatingMargin", "netMargin", "profitMargin", "ebitdaMargin"),
            "roe", List.of("returnOnEquity", "roe"),
            "roa", List.of("returnOnAssets", "roa"),
            "revenue", List.of("revenue", "totalRevenue", "sales"),
            "eps", List.of("eps", "earningsPerShare", "dilutedEps", "basicEps")
    );

    private final ObjectMapper objectMapper;

    public String verifyCitations(String reportMd, Map<Integer, Object> citations) {
        return verifyCitations(reportMd, citations, true, null);
    }

    public String verifyCitations(String reportMd, Map<Integer, Object> citations, boolean appendFootnote, CitationMetrics metrics) {
        if (reportMd == null || citations == null || citations.isEmpty()) {
            return reportMd;
        }
        StringBuilder correctedReport = new StringBuilder();
        Matcher m = CITATION_PATTERN.matcher(reportMd);
        int lastIndex = 0;
        boolean hasUnverified = false;
        boolean hasBroken = false;

        while (m.find()) {
            int matchStart = m.start();
            String context = extractContext(reportMd, matchStart);
            List<Double> contextNumbers = extractNumbersFromText(context);

            String[] ids = m.group(1).split(",");
            List<String> validatedIds = validateCitationIds(ids, context, contextNumbers, citations, metrics);

            hasUnverified |= validatedIds.stream().anyMatch(id -> id.endsWith("?"));
            hasBroken |= validatedIds.stream().anyMatch(id -> id.endsWith("!"));

            correctedReport.append(reportMd, lastIndex, matchStart);
            correctedReport.append("[").append(String.join(", ", validatedIds)).append("]");
            lastIndex = m.end();
        }
        correctedReport.append(reportMd.substring(lastIndex));

        if ((hasUnverified || hasBroken) && appendFootnote) {
            correctedReport.append("\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified. Citations marked with [!] reference missing or hallucinated sources.*");
        }
        return correctedReport.toString();
    }

    public <T> T verifyRecordCitations(T record, Map<Integer, Object> citations, CitationMetrics metrics) {
        if (record == null) return null;
        Class<?> clazz = record.getClass();
        if (!clazz.isRecord()) return record;

        try {
            RecordComponent[] components = clazz.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] paramTypes = new Class<?>[components.length];

            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
                Method accessor = components[i].getAccessor();
                Object value = accessor.invoke(record);
                args[i] = verifyComponentValue(value, citations, metrics);
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            log.error("Failed to verify record citations for {}", clazz.getName(), e);
            return record;
        }
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
        List<Object> newList = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String strItem) {
                newList.add(verifyCitations(strItem, citations, false, metrics));
            } else if (item != null && item.getClass().isRecord()) {
                newList.add(verifyRecordCitations(item, citations, metrics));
            } else {
                newList.add(item);
            }
        }
        return newList;
    }

    private String extractContext(String reportMd, int matchStart) {
        int contextStart = Math.max(0, matchStart - 150);
        int periodIdx = reportMd.lastIndexOf(". ", matchStart - 1);
        if (periodIdx != -1) periodIdx++;
        int newlineIdx = reportMd.lastIndexOf('\n', matchStart - 1);
        int safeStart = Math.max(contextStart, Math.max(periodIdx + 1, newlineIdx + 1));
        return reportMd.substring(safeStart, matchStart);
    }

    private List<String> validateCitationIds(String[] ids, String context, List<Double> contextNumbers,
                                             Map<Integer, Object> citations, CitationMetrics metrics) {
        List<String> validatedIds = new ArrayList<>();
        if (ids.length == contextNumbers.size() && ids.length > 0) {
            for (int i = 0; i < ids.length; i++) {
                validatedIds.add(validateSingleCitation(ids[i].trim(), List.of(contextNumbers.get(i)), context, citations, metrics));
            }
            return validatedIds;
        }
        for (String idStr : ids) {
            validatedIds.add(validateSingleCitation(idStr.trim(), contextNumbers, context, citations, metrics));
        }
        return validatedIds;
    }

    private String validateSingleCitation(String idStr, List<Double> contextNumbers, String context,
                                          Map<Integer, Object> citations, CitationMetrics metrics) {
        try {
            int id = Integer.parseInt(idStr);
            return checkCitation(id, contextNumbers, context, citations, metrics);
        } catch (NumberFormatException e) {
            return idStr;
        }
    }

    private String checkCitation(int id, List<Double> contextNumbers, String context,
                                 Map<Integer, Object> citations, CitationMetrics metrics) {
        if (!citations.containsKey(id)) {
            if (metrics != null) metrics.incrementBroken();
            return id + " !";
        }
        if (contextNumbers.isEmpty()) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }

        List<Double> jsonNumbers = extractNumbersForCitation(citations.get(id), context);
        boolean allVerified = contextNumbers.stream()
                .allMatch(cNum -> jsonNumbers.stream().anyMatch(jNum -> isMatch(cNum, jNum)));

        if (allVerified) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }
        if (metrics != null) metrics.incrementUnverified();
        log.debug("Citation mismatch detected: ID {} payload did not verify all context numbers {}", id, contextNumbers);
        return id + " ?";
    }

    private List<Double> extractNumbersForCitation(Object payload, String context) {
        JsonNode root = objectMapper.valueToTree(payload);
        List<Double> jsonNumbers = new ArrayList<>();
        List<String> targetFields = getTargetFields(context);
        extractNumbersFromJson(root, jsonNumbers, targetFields);

        if (jsonNumbers.isEmpty() && !targetFields.isEmpty()) {
            extractNumbersFromJson(root, jsonNumbers, List.of());
        }
        return jsonNumbers;
    }

    private List<String> getTargetFields(String context) {
        String lowerContext = context.toLowerCase();
        List<String> fields = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : TRIGGER_WORDS.entrySet()) {
            if (lowerContext.contains(entry.getKey())) {
                fields.addAll(entry.getValue());
            }
        }
        return fields;
    }

    private List<Double> extractNumbersFromText(String text) {
        List<Double> numbers = new ArrayList<>();
        String lowerContext = text.toLowerCase();
        Matcher m = NUMBER_PATTERN.matcher(text);

        while (m.find()) {
            parseAndCollectNumber(m.group(1), m.start(), lowerContext, numbers);
        }
        return numbers;
    }

    private void parseAndCollectNumber(String numStr, int matchStart, String lowerContext, List<Double> numbers) {
        try {
            double val = Double.parseDouble(numStr.replace(",", ""));
            if (val >= 1900 && val <= 2100 && numStr.indexOf('.') == -1) return;
            if (val >= 0 && val <= 4 && numStr.indexOf('.') == -1) return;

            if (val > 0) {
                int start = Math.max(0, matchStart - 25);
                String prefix = lowerContext.substring(start, matchStart);
                if (prefix.matches(".*\\b(fell|dropped|declined|decreased|down|loss)\\b.*")) {
                    val = -val;
                }
            }
            numbers.add(val);
        } catch (NumberFormatException ignored) {
        }
    }

    private void extractNumbersFromJson(JsonNode node, List<Double> numbers, List<String> targetFields) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                boolean isTarget = targetFields.isEmpty() || targetFields.stream()
                        .anyMatch(t -> entry.getKey().toLowerCase().contains(t.toLowerCase()));
                if (isTarget) {
                    extractAllNumbers(entry.getValue(), numbers);
                } else if (entry.getValue().isObject() || entry.getValue().isArray()) {
                    extractNumbersFromJson(entry.getValue(), numbers, targetFields);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractNumbersFromJson(child, numbers, targetFields);
            }
        } else if (targetFields.isEmpty()) {
            extractAllNumbers(node, numbers);
        }
    }

    private void extractAllNumbers(JsonNode node, List<Double> numbers) {
        if (node.isObject()) {
            node.properties().forEach(entry -> extractAllNumbers(entry.getValue(), numbers));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractAllNumbers(child, numbers);
            }
        } else if (node.isNumber()) {
            numbers.add(node.asDouble());
        } else if (node.isString()) {
            tryParseStringNumber(node.asString().trim(), numbers);
        }
    }

    private void tryParseStringNumber(String text, List<Double> numbers) {
        if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
            try {
                JsonNode nested = objectMapper.readTree(text);
                extractAllNumbers(nested, numbers);
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            numbers.add(Double.parseDouble(text));
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean isMatch(double cNum, double jNum) {
        if (Math.abs(cNum) < 1e-6 && Math.abs(jNum) < 1e-6) return true;
        if (Math.abs(cNum) < 1e-6 || Math.abs(jNum) < 1e-6) return false;
        if (Math.signum(cNum) != Math.signum(jNum)) return false;

        double absC = Math.abs(cNum);
        double absJ = Math.abs(jNum);
        double ratio = absC > absJ ? absC / absJ : absJ / absC;

        for (double scale : VALID_SCALES) {
            double normalized = ratio / scale;
            if (Math.abs(normalized - 1.0) <= MATCH_TOLERANCE) {
                return true;
            }
        }
        return false;
    }
}
