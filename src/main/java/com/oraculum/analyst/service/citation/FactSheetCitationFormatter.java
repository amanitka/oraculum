package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class FactSheetCitationFormatter {

    private static final Set<String> METADATA_KEYS = Set.of(
            "Report Date", "Fiscal Year", "Fiscal Period", "Currency", "Ticker",
            "Publish Date", "Restated Date", "Shares (Basic)", "Shares (Diluted)"
    );

    private final JsonMapper jsonMapper;
    private final CitationRegistry citationRegistry;

    public String wrapWithCitation(Class<?> clazz, Object uniqueId, Object dto) {
        if (dto == null) return "{}";
        String citationId = citationRegistry.getOrAssignCitationId(clazz, uniqueId, dto);
        try {
            JsonNode root = jsonMapper.valueToTree(dto);
            if (!root.isObject()) return JsonUtils.toJson(jsonMapper, dto, "{}");
            ObjectNode newNode = (ObjectNode) root;
            newNode.put("citation_id", citationId);
            return jsonMapper.writeValueAsString(newNode);
        } catch (Exception e) {
            return JsonUtils.toJson(jsonMapper, dto, "{}");
        }
    }

    public <T> String wrapListWithCitations(Class<T> clazz, Collection<T> items, Function<T, Object> idExtractor) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        return "[" + items.stream()
                .map(dto -> wrapWithCitation(clazz, idExtractor.apply(dto), dto))
                .collect(Collectors.joining(",")) + "]";
    }

    public <T> String formatStatementHistory(Class<T> clazz,
                                             List<T> stmts,
                                             int limit,
                                             Function<T, Object> idExtractor,
                                             Function<T, String> statementDataExtractor,
                                             String sourceName,
                                             StatementVariant variant) {
        if (stmts == null || stmts.isEmpty()) {
            return "[]";
        }
        return "[" + stmts.stream()
                .limit(limit)
                .map(dto -> {
                    String stmtData = statementDataExtractor.apply(dto);
                    Object payload = getCitationPayload(stmtData, dto, sourceName, variant);
                    String citationId = citationRegistry.getOrAssignCitationId(clazz, idExtractor.apply(dto), payload);
                    return namespaceJsonMetrics(stmtData, variant, citationId);
                })
                .collect(Collectors.joining(",")) + "]";
    }

    public Object getCitationPayloadFromDto(Object dto, String sourceName, StatementVariant variant) {
        try {
            JsonNode node = jsonMapper.valueToTree(dto);
            if (node.isObject()) {
                ObjectNode objNode = (ObjectNode) node;
                if (sourceName != null) {
                    objNode.put("_source", sourceName);
                }
                if (variant != null) {
                    objNode.put("_variant", variant.name());
                }
                return objNode;
            }
            return dto;
        } catch (Exception e) {
            log.warn("Failed to convert dto to json tree for citation payload. Source: {}", sourceName, e);
            return dto;
        }
    }

    private Object getCitationPayload(String jsonStr, Object fallbackDto, String sourceName, StatementVariant variant) {
        if (jsonStr == null || jsonStr.isBlank()) return fallbackDto;
        try {
            JsonNode node = jsonMapper.readTree(jsonStr);
            if (node.isObject()) {
                ObjectNode objNode = (ObjectNode) node;
                if (sourceName != null) {
                    objNode.put("_source", sourceName);
                }
                if (variant != null) {
                    objNode.put("_variant", variant.name());
                }
            }
            return node;
        } catch (Exception e) {
            log.warn("Failed to parse json string for citation payload. Source: {}", sourceName, e);
            return fallbackDto;
        }
    }

    private String namespaceJsonMetrics(String jsonStr, StatementVariant variant, String citationId) {
        if (jsonStr == null || jsonStr.isBlank()) return "{}";
        try {
            JsonNode root = jsonMapper.readTree(jsonStr);
            if (!root.isObject()) return jsonStr;

            ObjectNode newNode = jsonMapper.createObjectNode();
            if (citationId != null) {
                newNode.put("citation_id", citationId);
            }
            processJsonFields((ObjectNode) root, newNode, getVariantSuffix(variant));

            return jsonMapper.writeValueAsString(newNode);
        } catch (Exception e) {
            log.warn("Failed to namespace json metrics. Variant: {}, Citation ID: {}", variant, citationId, e);
            return jsonStr;
        }
    }

    private void processJsonFields(ObjectNode source, ObjectNode target, String suffix) {
        addPeriodEndLabel(source, target);
        for (Map.Entry<String, JsonNode> field : source.properties()) {
            String key = field.getKey();
            if (METADATA_KEYS.contains(key)) {
                target.set(key, field.getValue());
            } else if (!isVendorDerivedRatio(key)) {
                target.set(formatKey(key, suffix), field.getValue());
            }
        }
    }

    private void addPeriodEndLabel(ObjectNode source, ObjectNode target) {
        String reportDate = source.path("Report Date").asString("");
        if (reportDate.isBlank()) {
            return;
        }
        String fiscalPeriod = source.path("Fiscal Period").asString("");
        String label = fiscalPeriod.isBlank()
                ? "ended " + reportDate
                : fiscalPeriod + " (ended " + reportDate + ")";
        target.put("Period End Label", label);
    }

    private boolean isVendorDerivedRatio(String key) {
        String lower = key.toLowerCase();
        return lower.contains("margin") || lower.contains("ratio") ||
                lower.contains("growth") || lower.contains("return") ||
                lower.contains("yield");
    }

    private String formatKey(String key, String suffix) {
        String snakeCase = key.toLowerCase()
                .replaceAll("[\\s\\-()]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return snakeCase + suffix;
    }

    private String getVariantSuffix(StatementVariant variant) {
        return switch (variant) {
            case QUARTERLY -> "_q";
            case TTM -> "_ttm";
            case ANNUAL -> "_a";
        };
    }
}
