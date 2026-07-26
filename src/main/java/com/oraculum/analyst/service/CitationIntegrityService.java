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
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:,\\d{3})*(?:\\.\\d+)?");
    private final ObjectMapper objectMapper;

    public String verifyCitations(String reportMd, Map<Integer, Object> citations) {
        return verifyCitations(reportMd, citations, true);
    }

    public String verifyCitations(String reportMd, Map<Integer, Object> citations, boolean appendFootnote) {
        if (reportMd == null || citations == null || citations.isEmpty()) {
            return reportMd;
        }

        StringBuilder correctedReport = new StringBuilder();
        Matcher m = CITATION_PATTERN.matcher(reportMd);
        int lastIndex = 0;
        boolean hasUnverified = false;

        while (m.find()) {
            int matchStart = m.start();
            String context = extractContext(reportMd, matchStart);
            List<Double> contextNumbers = extractNumbersFromText(context);

            String[] ids = m.group(1).split(",");
            List<String> validatedIds = validateCitationIds(ids, contextNumbers, citations);
            if (validatedIds.stream().anyMatch(id -> id.endsWith("?"))) {
                hasUnverified = true;
            }

            correctedReport.append(reportMd, lastIndex, matchStart);
            correctedReport.append("[").append(String.join(", ", validatedIds)).append("]");
            lastIndex = m.end();
        }

        correctedReport.append(reportMd.substring(lastIndex));

        if (hasUnverified && appendFootnote) {
            correctedReport.append("\n\n*Note: Citations marked with [?] contain extrapolated metrics or claims that could not be strictly verified against the raw numerical data.*");
        }

        return correctedReport.toString();
    }

    public <T> T verifyRecordCitations(T record, Map<Integer, Object> citations) {
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
                    args[i] = verifyCitations(str, citations, false);
                } else if (value instanceof List<?> list) {
                    List<Object> newList = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof String strItem) {
                            newList.add(verifyCitations(strItem, citations, false));
                        } else if (item != null && item.getClass().isRecord()) {
                            newList.add(verifyRecordCitations(item, citations));
                        } else {
                            newList.add(item);
                        }
                    }
                    args[i] = newList;
                } else if (value != null && value.getClass().isRecord()) {
                    args[i] = verifyRecordCitations(value, citations);
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

    private List<String> validateCitationIds(String[] ids, List<Double> contextNumbers, Map<Integer, Object> citations) {
        List<String> validatedIds = new ArrayList<>();
        for (String idStr : ids) {
            try {
                int id = Integer.parseInt(idStr.trim());
                if (isCitationValid(id, contextNumbers, citations)) {
                    validatedIds.add(String.valueOf(id));
                } else {
                    validatedIds.add(id + " ?");
                }
            } catch (NumberFormatException e) {
                validatedIds.add(idStr.trim());
            }
        }
        return validatedIds;
    }

    private boolean isCitationValid(int id, List<Double> contextNumbers, Map<Integer, Object> citations) {
        if (!citations.containsKey(id)) {
            return false;
        }
        if (contextNumbers.isEmpty()) {
            return true;
        }

        Object payload = citations.get(id);
        JsonNode root = objectMapper.valueToTree(payload);
        List<Double> jsonNumbers = new ArrayList<>();
        extractNumbersFromJson(root, jsonNumbers);

        for (double cNum : contextNumbers) {
            for (double jNum : jsonNumbers) {
                if (isMatch(cNum, jNum)) {
                    return true;
                }
            }
        }

        log.debug("Citation mismatch detected: ID {} did not contain any of {}", id, contextNumbers);
        return false;
    }

    private List<Double> extractNumbersFromText(String text) {
        List<Double> numbers = new ArrayList<>();
        Matcher m = NUMBER_PATTERN.matcher(text);
        while (m.find()) {
            try {
                String numStr = m.group().replace(",", "");
                double val = Double.parseDouble(numStr);

                if (val >= 1900 && val <= 2100 && numStr.indexOf('.') == -1) continue; // Ignore years
                if (val <= 4 && numStr.indexOf('.') == -1) continue; // Ignore small integers

                numbers.add(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return numbers;
    }

    private void extractNumbersFromJson(JsonNode node, List<Double> numbers) {
        if (node.isObject()) {
            node.properties().forEach(entry -> extractNumbersFromJson(entry.getValue(), numbers));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractNumbersFromJson(child, numbers);
            }
        } else if (node.isNumber()) {
            numbers.add(node.asDouble());
        } else if (node.isString()) {
            try {
                double val = Double.parseDouble(node.asString());
                numbers.add(val);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean isMatch(double cNum, double jNum) {
        if (Math.abs(cNum) < 1e-6 && Math.abs(jNum) < 1e-6) return true;
        if (Math.abs(cNum) < 1e-6 || Math.abs(jNum) < 1e-6) return false;

        double ratio = Math.abs(cNum / jNum);
        double log10 = Math.log10(ratio);
        double roundedLog10 = Math.round(log10);

        double normalizedRatio = ratio / Math.pow(10, roundedLog10);
        return Math.abs(normalizedRatio - 1.0) <= 0.03; // 3% tolerance
    }
}
