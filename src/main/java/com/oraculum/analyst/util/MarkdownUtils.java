package com.oraculum.analyst.util;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MarkdownUtils {

    private MarkdownUtils() {
        // Prevent instantiation
    }

    /**
     * Converts a list of DTOs into a cleanly formatted Markdown table.
     */
    public static String toMarkdownTable(List<?> items, String title, ObjectMapper objectMapper) {
        if (items == null || items.isEmpty()) {
            return "No data available.";
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dictItems = items.stream()
                    .map(item -> (Map<String, Object>) objectMapper.convertValue(item, Map.class))
                    .collect(Collectors.toList());

            List<String> headers = dictItems.getFirst()
                    .keySet()
                    .stream()
                    .filter(h -> !h.equals("template") && !h.equals("variant"))
                    .collect(Collectors.toList());

            String headerLine = "| " + String.join(" | ", headers) + " |";
            String separator = "| " + headers.stream().map(_ -> "---").collect(Collectors.joining(" | ")) + " |";

            List<String> rows = dictItems.stream().map(item -> {
                List<String> rowValues = headers.stream()
                        .map(h -> String.valueOf(item.get(h) != null ? item.get(h) : ""))
                        .collect(Collectors.toList());
                return "| " + String.join(" | ", rowValues) + " |";
            }).collect(Collectors.toList());

            return "### " + title + "\n" + headerLine + "\n" + separator + "\n" + String.join("\n", rows);
        } catch (Exception e) {
            return "Error formatting table: " + e.getMessage();
        }
    }
}
