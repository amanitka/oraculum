package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CashFlowAgentService implements AgentService<CashFlowAgentOutput> {

    private static final double DIRECTION_EPSILON = 1e-9;
    private static final Set<String> MISSING_NUMERIC_VALUES = new HashSet<>(Arrays.asList("",
            "-",
            "--",
            "na",
            "n/a",
            "nan",
            "none",
            "null"));
    private static final Map<String, List<String>> METRIC_ALIASES = Map.of("net_cash_from_operating_activities",
            Arrays.asList("net_cash_from_operating_activities",
                    "net_cash_from_operating_activities_continuing_operations"),
            "free_cash_flow",
            Arrays.asList("free_cash_flow"),
            "capital_expenditure",
            Arrays.asList("capital_expenditure", "capex"));
    private static final List<String> YEAR_ALIASES = Arrays.asList("fiscal_year", "year");

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.CASH_FLOW;
    }

    @Override
    public Class<CashFlowAgentOutput> getOutputModel() {
        return CashFlowAgentOutput.class;
    }

    @Override
    public AgentOutput<CashFlowAgentOutput> run(AgentContext ctx) {
        FactSheetAgentOutput factSheetOutput = (FactSheetAgentOutput) ctx.priorOutputs().get(AgentType.FACT_SHEET);
        CompanyFactSheetData factSheet = factSheetOutput.factSheet();
        Map<String, Object> quantitativeGuardrails = buildQuantitativeGuardrails(factSheet.cashFlowHistory());

        Map<String, Object> promptData = Map.of("cash_flow_history",
                factSheet.cashFlowHistory(),
                "derived_metrics",
                factSheet.derivedMetrics(),
                "quantitative_guardrails",
                quantitativeGuardrails);

        String promptDataJson;
        try {
            promptDataJson = objectMapper.writeValueAsString(promptData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.CASH_FLOW).replace("{{ fact_sheet_json }}", promptDataJson);

        String userPrompt = String.format(
                "Analyze cash flow for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.requestDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<CashFlowAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                CashFlowAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }

    private String normalizeKey(String value) {
        return value.strip().toLowerCase().replace(" ", "_");
    }

    private Double parseNumeric(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return null;
        }

        if (value instanceof Integer || value instanceof Double) {
            return ((Number) value).doubleValue();
        }

        String text = String.valueOf(value).strip();
        if (text.isEmpty()) {
            return null;
        }

        String normalized = text.toLowerCase().replace(",", "");
        if (MISSING_NUMERIC_VALUES.contains(normalized)) {
            return null;
        }

        Pattern pattern = Pattern.compile("-?\\d+(?:\\.\\d+)?");
        Matcher matcher = pattern.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        double numericValue = Double.parseDouble(matcher.group());
        if (normalized.contains("trillion")) {
            return numericValue * 1_000_000;
        }
        if (normalized.contains("billion")) {
            return numericValue * 1_000;
        }
        return numericValue;
    }

    private Map<String, Object> parsePayload(String payloadRaw) {
        String raw = payloadRaw.strip();
        if (raw.isEmpty()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (JacksonException e) {
            // Not a valid JSON, ignore
        }

        return Map.of();
    }

    private List<Map<String, String>> parseMarkdownTable(String markdownTable) {
        List<String> lines = Arrays.stream(markdownTable.split("\n"))
                .map(String::strip)
                .filter(line -> line.startsWith("|"))
                .collect(Collectors.toList());

        if (lines.size() < 2) {
            return new ArrayList<>();
        }

        List<String> headers = Arrays.stream(lines.get(0).substring(1, lines.get(0).length() - 1).split("\\|"))
                .map(String::strip)
                .collect(Collectors.toList());

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            List<String> cells = Arrays.stream(lines.get(i).substring(1, lines.get(i).length() - 1).split("\\|"))
                    .map(String::strip)
                    .collect(Collectors.toList());
            if (cells.size() != headers.size()) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), cells.get(j));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<Map.Entry<Integer, Double>> deduplicateSeries(List<Map.Entry<Integer, Double>> points) {
        Map<Integer, Double> uniqueByYear = new HashMap<>();
        for (Map.Entry<Integer, Double> point : points) {
            if (!uniqueByYear.containsKey(point.getKey())) {
                uniqueByYear.put(point.getKey(), point.getValue());
            }
        }
        return uniqueByYear.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
    }

    private Integer resolveYear(Map<String, Object> payload, Map<String, String> row) {
        for (String yearKey : YEAR_ALIASES) {
            Double numericYear = parseNumeric(payload.getOrDefault(yearKey, row.get(yearKey)));
            if (numericYear != null) {
                return numericYear.intValue();
            }
        }
        return null;
    }

    private Double resolveMetricValue(List<String> aliases, Map<String, Object> payload, Map<String, String> row) {
        for (String alias : aliases) {
            if (payload.containsKey(alias)) {
                Double payloadValue = parseNumeric(payload.get(alias));
                if (payloadValue != null) {
                    return payloadValue;
                }
            }

            if (row.containsKey(alias)) {
                Double rowValue = parseNumeric(row.get(alias));
                if (rowValue != null) {
                    return rowValue;
                }
            }
        }
        return null;
    }

    private String resolveTrend(List<Map.Entry<Integer, Double>> points) {
        if (points.size() < 2) {
            return "insufficient_data";
        }

        double firstValue = points.get(0).getValue();
        double lastValue = points.get(points.size() - 1).getValue();
        if (lastValue > firstValue + DIRECTION_EPSILON) {
            return "increasing";
        }
        if (lastValue < firstValue - DIRECTION_EPSILON) {
            return "decreasing";
        }
        return "flat";
    }

    private Map<String, Object> buildMetricGuardrails(Map<String, List<Map.Entry<Integer, Double>>> seriesByMetric) {
        Map<String, Object> metrics = new HashMap<>();
        for (Map.Entry<String, List<Map.Entry<Integer, Double>>> entry : seriesByMetric.entrySet()) {
            String metricName = entry.getKey();
            List<Map.Entry<Integer, Double>> points = entry.getValue();
            List<Map.Entry<Integer, Double>> deduplicatedSeries = deduplicateSeries(points);
            if (deduplicatedSeries.isEmpty()) {
                continue;
            }

            Map<String, Object> metricData = new HashMap<>();
            metricData.put("trend", resolveTrend(deduplicatedSeries));
            metricData.put("series_millions",
                    deduplicatedSeries.stream()
                            .map(p -> Map.of("fiscal_year",
                                    p.getKey(),
                                    "value_millions",
                                    Math.round(p.getValue() * 1000.0) / 1000.0))
                            .collect(Collectors.toList()));
            metrics.put(metricName, metricData);
        }
        return metrics;
    }

    private Map<String, Object> buildQuantitativeGuardrails(String cashFlowHistory) {
        Map<String, List<Map.Entry<Integer, Double>>> seriesByMetric = new HashMap<>();
        for (String metric : METRIC_ALIASES.keySet()) {
            seriesByMetric.put(metric, new ArrayList<>());
        }

        for (Map<String, String> markdownRow : parseMarkdownTable(cashFlowHistory)) {
            Map<String, String> normalizedRow = markdownRow.entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> normalizeKey(e.getKey()), Map.Entry::getValue));
            String payloadRaw = normalizedRow.getOrDefault("payload", "");
            Map<String, Object> payload = parsePayload(payloadRaw).entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> normalizeKey(String.valueOf(e.getKey())), Map.Entry::getValue));

            Integer year = resolveYear(payload, normalizedRow);
            if (year == null) {
                continue;
            }

            for (Map.Entry<String, List<String>> metricEntry : METRIC_ALIASES.entrySet()) {
                String metricName = metricEntry.getKey();
                List<String> aliases = metricEntry.getValue();
                Double metricValue = resolveMetricValue(aliases, payload, normalizedRow);
                if (metricValue != null) {
                    seriesByMetric.get(metricName).add(Map.entry(year, metricValue));
                }
            }
        }

        Map<String, Object> metrics = buildMetricGuardrails(seriesByMetric);

        if (metrics.isEmpty()) {
            return Map.of();
        }

        return Map.of("unit_convention",
                "Treat raw cash-flow values as millions of reporting currency unless explicitly labeled otherwise.",
                "metrics",
                metrics);
    }
}