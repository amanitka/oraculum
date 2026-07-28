package com.oraculum.analyst.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitationIntegrityService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\d,\\s]+)\\]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:,\\d{3})*(?:\\.\\d+)?)");
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

            if (validatedIds.stream().anyMatch(id -> id.endsWith("?"))) {
                hasUnverified = true;
            }
            if (validatedIds.stream().anyMatch(id -> id.endsWith("!"))) {
                hasBroken = true;
            }

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
        if (!clazz.isRecord()) {
            return record;
        }
        try {
            RecordComponent[] components = clazz.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                java.lang.reflect.RecordComponent component = components[i];
                paramTypes[i] = component.getType();
                java.lang.reflect.Method accessor = component.getAccessor();
                Object value = accessor.invoke(record);
                if (value instanceof String str) {
                    args[i] = verifyCitations(str, citations, false, metrics);
                } else if (value instanceof List<?> list) {
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
                    args[i] = newList;
                } else if (value != null && value.getClass().isRecord()) {
                    args[i] = verifyRecordCitations(value, citations, metrics);
                } else {
                    args[i] = value;
                }
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            log.error("Failed to verify record citations for {}", clazz.getName(), e);
            return record;
        }
    }

    private String extractContext(String reportMd, int matchStart) {
        int contextStart = Math.max(0, matchStart - 150);
        int periodIdx = reportMd.lastIndexOf(". ", matchStart - 1);
        if (periodIdx != -1) periodIdx++; // point to the space after the period
        int newlineIdx = reportMd.lastIndexOf('\n', matchStart - 1);
        int safeStart = Math.max(contextStart, Math.max(periodIdx + 1, newlineIdx + 1));
        return reportMd.substring(safeStart, matchStart);
    }

    private List<String> validateCitationIds(String[] ids, String context, List<Double> contextNumbers, Map<Integer, Object> citations, CitationMetrics metrics) {
        List<String> validatedIds = new ArrayList<>();

        // 1. Positional Pairing (if N citations match N numbers in context)
        if (ids.length == contextNumbers.size() && ids.length > 0) {
            for (int i = 0; i < ids.length; i++) {
                String idStr = ids[i].trim();
                try {
                    int id = Integer.parseInt(idStr);
                    validatedIds.add(checkCitation(id, List.of(contextNumbers.get(i)), context, citations, metrics));
                } catch (NumberFormatException e) {
                    validatedIds.add(idStr);
                }
            }
            return validatedIds;
        }

        // 2. Fallback Any-Match Pairing
        for (String idStr : ids) {
            try {
                int id = Integer.parseInt(idStr.trim());
                validatedIds.add(checkCitation(id, contextNumbers, context, citations, metrics));
            } catch (NumberFormatException e) {
                validatedIds.add(idStr.trim());
            }
        }
        return validatedIds;
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

    private String checkCitation(int id, List<Double> contextNumbers, String context, Map<Integer, Object> citations, CitationMetrics metrics) {
        if (!citations.containsKey(id)) {
            if (metrics != null) metrics.incrementBroken();
            return id + " !";
        }
        if (contextNumbers.isEmpty()) {
            if (metrics != null) metrics.incrementValid();
            return String.valueOf(id);
        }

        Object payload = citations.get(id);
        JsonNode root = objectMapper.valueToTree(payload);
        List<Double> jsonNumbers = new ArrayList<>();

        List<String> targetFields = getTargetFields(context);
        extractNumbersFromJson(root, jsonNumbers, targetFields);

        // Fallback to searching all fields if target fields yielded no numbers
        if (jsonNumbers.isEmpty() && !targetFields.isEmpty()) {
            extractNumbersFromJson(root, jsonNumbers, List.of());
        }

        for (double cNum : contextNumbers) {
            for (double jNum : jsonNumbers) {
                if (isMatch(cNum, jNum)) {
                    if (metrics != null) metrics.incrementValid();
                    return String.valueOf(id);
                }
            }
        }

        if (metrics != null) metrics.incrementUnverified();
        log.debug("Citation mismatch detected: ID {} did not contain any of {}", id, contextNumbers);
        return id + " ?";
    }

    private List<Double> extractNumbersFromText(String text) {
        List<Double> numbers = new ArrayList<>();
        String lowerContext = text.toLowerCase();
        Matcher m = NUMBER_PATTERN.matcher(text);
        while (m.find()) {
            try {
                String numStr = m.group(1).replace(",", "");
                double val = Double.parseDouble(numStr);

                if (val >= 1900 && val <= 2100 && numStr.indexOf('.') == -1) continue; // Ignore years
                if (val >= 0 && val <= 4 && numStr.indexOf('.') == -1) continue; // Ignore small positive integers

                if (val > 0) {
                    int start = Math.max(0, m.start() - 25);
                    String prefix = lowerContext.substring(start, m.start());
                    if (prefix.matches(".*\\b(fell|dropped|declined|decreased|down|loss)\\b.*")) {
                        val = -val;
                    }
                }

                numbers.add(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return numbers;
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
                } else if (entry.getValue().isString()) {
                    String text = entry.getValue().asString().trim();
                    if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
                        try {
                            JsonNode nested = objectMapper.readTree(text);
                            extractNumbersFromJson(nested, numbers, targetFields);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractNumbersFromJson(child, numbers, targetFields);
            }
        } else if (node.isString()) {
            String text = node.asString().trim();
            if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
                try {
                    JsonNode nested = objectMapper.readTree(text);
                    extractNumbersFromJson(nested, numbers, targetFields);
                } catch (Exception ignored) {
                }
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
            String text = node.asString().trim();
            if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
                try {
                    JsonNode nested = objectMapper.readTree(text);
                    extractAllNumbers(nested, numbers);
                    return;
                } catch (Exception ignored) {
                }
            }
            try {
                double val = Double.parseDouble(text);
                numbers.add(val);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean isMatch(double cNum, double jNum) {
        if (Math.abs(cNum) < 1e-6 && Math.abs(jNum) < 1e-6) return true;
        if (Math.abs(cNum) < 1e-6 || Math.abs(jNum) < 1e-6) return false;

        if (Math.signum(cNum) != Math.signum(jNum)) return false;

        double ratio = Math.abs(cNum / jNum);
        double log10 = Math.log10(ratio);
        double roundedLog10 = Math.round(log10);

        double normalizedRatio = ratio / Math.pow(10, roundedLog10);
        return Math.abs(normalizedRatio - 1.0) <= 0.03; // 3% tolerance
    }
}
