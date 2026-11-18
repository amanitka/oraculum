package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenarioCase;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.RendererUtil;
import tools.jackson.databind.JsonNode;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Collects referenced citations from an {@link AnalysisResult} and formats the PDF sources appendix.
 */
final class AnalysisPdfCitationHelper {

    private static final int MAX_PREVIEW_FIELDS = 5;

    record CitationEntry(String id, String source, String details) {
    }

    private AnalysisPdfCitationHelper() {
    }

    static List<CitationEntry> collectReferencedCitations(AnalysisResult result, JsonNode citationsNode) {
        if (result == null || citationsNode == null || !citationsNode.isObject()) {
            return List.of();
        }
        Set<String> orderedIds = new LinkedHashSet<>();
        for (String text : collectCitableTexts(result)) {
            orderedIds.addAll(MarkdownRenderer.extractCitationIds(text, citationsNode));
        }
        return orderedIds.stream()
                .map(id -> toCitationEntry(id, citationsNode.get(id)))
                .toList();
    }

    static String formatPdfCitation(String id, String suffix) {
        boolean isUnverifiable = suffix.contains("?");
        String classAttr = isUnverifiable ? "cite cite-unverifiable" : "cite";
        String suffixPart = isUnverifiable ? " ?" : "";
        String safeId = PdfHtmlUtil.escape(id);
        return "<sup class=\"" + classAttr + "\"><a class=\"cite-link\" href=\"#cite-"
                + safeId + "\">" + safeId + suffixPart + "</a></sup>";
    }

    static String renderCitationsAppendix(List<CitationEntry> entries) {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class=\"section-title\">Referenced Sources</h2>");
        sb.append("<table class=\"data-table\"><thead><tr>");
        sb.append("<th style=\"width:42pt;\">Ref</th>");
        sb.append("<th style=\"width:130pt;\">Source</th>");
        sb.append("<th>Data Summary</th>");
        sb.append("</tr></thead><tbody>");
        for (CitationEntry entry : entries) {
            appendCitationRow(sb, entry);
        }
        return sb.append("</tbody></table>").toString();
    }

    private static void appendCitationRow(StringBuilder sb, CitationEntry entry) {
        String safeId = PdfHtmlUtil.escape(entry.id());
        sb.append("<tr><td><a id=\"cite-").append(safeId).append("\" class=\"cite-anchor\"><strong>[")
                .append(safeId).append("]</strong></a></td>")
                .append("<td>").append(PdfHtmlUtil.escape(entry.source())).append("</td>")
                .append("<td class=\"citation-summary\">").append(PdfHtmlUtil.escape(entry.details())).append("</td></tr>");
    }

    private static List<String> collectCitableTexts(AnalysisResult r) {
        List<String> texts = new ArrayList<>();
        addIfPresent(texts, r.thesis());
        addAllIfPresent(texts, r.topBullPoints());
        addAllIfPresent(texts, r.topBearPoints());
        addIfPresent(texts, r.whatWouldChangeThis());
        addIfPresent(texts, r.valuationOneLiner());
        addScenarioTexts(texts, r.scenarios());
        addAllIfPresent(texts, r.thesisBreakers());
        addIfPresent(texts, r.executiveSummary());
        addIfPresent(texts, r.recommendationReasoning());
        return texts;
    }

    private static void addScenarioTexts(List<String> texts, ScenariosResult s) {
        if (s == null) {
            return;
        }
        addScenarioCaseTexts(texts, s.bull());
        addScenarioCaseTexts(texts, s.base());
        addScenarioCaseTexts(texts, s.bear());
    }

    private static void addScenarioCaseTexts(List<String> texts, ScenarioCase c) {
        if (c == null) {
            return;
        }
        addIfPresent(texts, c.description());
        addIfPresent(texts, c.keyAssumption());
        addIfPresent(texts, c.impliedUpside());
        addIfPresent(texts, c.impliedMove());
        addIfPresent(texts, c.impliedDownside());
    }

    private static void addIfPresent(List<String> texts, String value) {
        if (!PdfHtmlUtil.isBlank(value)) {
            texts.add(value);
        }
    }

    private static void addAllIfPresent(List<String> texts, List<String> values) {
        if (values != null) {
            values.forEach(v -> addIfPresent(texts, v));
        }
    }

    private static CitationEntry toCitationEntry(String id, JsonNode data) {
        String source = formatSourceName(data);
        String details = formatDataSummary(data);
        return new CitationEntry(id, source, details);
    }

    private static String formatSourceName(JsonNode data) {
        if (data == null || !data.isObject() || !data.has("_source")) {
            return "Underlying Data";
        }
        String source = data.get("_source").asString();
        if (data.has("_variant")) {
            return source + " (" + data.get("_variant").asString() + ")";
        }
        return source;
    }

    private static String formatDataSummary(JsonNode data) {
        if (data == null || !data.isObject()) {
            return data != null ? data.asString() : "";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : data.properties()) {
            if (entry.getKey().startsWith("_")) {
                continue;
            }
            parts.add(RendererUtil.formatKeyTitle(entry.getKey()) + ": " + formatJsonValue(entry.getValue()));
            if (parts.size() >= MAX_PREVIEW_FIELDS) {
                break;
            }
        }
        return String.join(" | ", parts);
    }

    private static String formatJsonValue(JsonNode val) {
        if (val.isNumber()) {
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMaximumFractionDigits(4);
            return nf.format(val.asDouble());
        }
        String text = val.isObject() || val.isArray() ? val.toString() : val.asString();
        return text.length() > 90 ? text.substring(0, 87) + "..." : text;
    }
}
