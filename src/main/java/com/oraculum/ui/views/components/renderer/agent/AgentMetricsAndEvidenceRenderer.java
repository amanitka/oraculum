package com.oraculum.ui.views.components.renderer.agent;

import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.SnapshotUiSupport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentMetricsAndEvidenceRenderer {

    private final MarkdownRenderer markdownRenderer;

    public void addEvidenceIfPresent(VerticalLayout layout, JsonNode agentData, String jsonData) {
        if (agentData.has("evidence")) {
            layout.add(renderEvidence(agentData.get("evidence"), jsonData));
        }
    }

    public void addMetricsIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("factor_scores")) {
            layout.add(renderFactorScoresSection(agentData.get("factor_scores")));
        } else if (agentData.has("metrics")) {
            layout.add(renderMetrics(agentData.get("metrics"), "Key Metrics"));
        }
    }

    public void addValuationDimensionsIfPresent(VerticalLayout layout, JsonNode agentData) {
        if (agentData.has("valuation_dimensions")) {
            layout.add(renderMetrics(agentData.get("valuation_dimensions"), "Valuation Dimensions"));
        }
    }

    private Component renderEvidence(JsonNode value, String jsonData) {
        if (value == null || !value.isArray() || value.isEmpty()) {
            return new Span();
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("margin-bottom", "20px");
        layout.add(SnapshotUiSupport.createSectionHeader("Evidence & Data"));
        layout.add(buildEvidenceGrid(value, jsonData));
        return layout;
    }

    private Grid<JsonNode> buildEvidenceGrid(JsonNode value, String jsonData) {
        Grid<JsonNode> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.addClassName("screener-grid");
        grid.setWidthFull();
        grid.getStyle().set("font-size", "0.88rem");

        addCoreEvidenceColumns(grid);
        addDetailEvidenceColumns(grid, jsonData);

        List<JsonNode> itemList = new ArrayList<>();
        value.forEach(itemList::add);
        grid.setItems(itemList);
        grid.setAllRowsVisible(true);
        return grid;
    }

    private void addCoreEvidenceColumns(Grid<JsonNode> grid) {
        grid.addColumn(item -> item.path("metric").asString(""))
                .setHeader("Metric").setWidth("220px").setFlexGrow(1);
        grid.addComponentColumn(this::renderCurrentValueCell)
                .setHeader("Current").setWidth("130px").setFlexGrow(0);
        grid.addComponentColumn(this::renderPreviousValueCell)
                .setHeader("Previous").setWidth("130px").setFlexGrow(0);
        grid.addComponentColumn(item -> renderTrendIcon(item.path("trend").asString("")))
                .setHeader("Trend").setWidth("70px").setFlexGrow(0);
    }

    private void addDetailEvidenceColumns(Grid<JsonNode> grid, String jsonData) {
        grid.addComponentColumn(this::renderSourceTypeCell)
                .setHeader("Type").setWidth("110px").setFlexGrow(0);
        grid.addComponentColumn(item -> renderSignificanceCell(item, jsonData))
                .setHeader("Why It Matters").setFlexGrow(4);
    }

    private Span renderCurrentValueCell(JsonNode item) {
        Span currentSpan = new Span(item.path("value").asString(""));
        currentSpan.getStyle().set("font-weight", "bold").set("color", "var(--lumo-header-text-color)");
        return currentSpan;
    }

    private Span renderPreviousValueCell(JsonNode item) {
        String prev = item.path("previous_value").asString("");
        Span prevSpan = new Span(prev.isBlank() ? "-" : prev);
        prevSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return prevSpan;
    }

    private Component renderSourceTypeCell(JsonNode item) {
        String sourceType = item.path("source_type").asString("");
        if (sourceType.isBlank()) {
            return new Span("-");
        }
        String formatted = SnapshotUiSupport.formatDimensionLabel(sourceType);
        String theme = switch (sourceType.toUpperCase(Locale.ROOT)) {
            case "REPORTED" -> "primary";
            case "DERIVED" -> "warning";
            case "ESTIMATED" -> "success";
            default -> "contrast";
        };
        return SnapshotUiSupport.createStatusBadge(formatted, theme);
    }

    private Component renderSignificanceCell(JsonNode item, String jsonData) {
        String sig = item.path("significance").asString("");
        if (sig.isBlank()) {
            return new Span("-");
        }
        Component sigComp = markdownRenderer.renderMarkdownWithCitations(sig, jsonData);
        sigComp.getStyle().set("font-size", "0.85rem").set("line-height", "1.45");
        return sigComp;
    }

    private Component renderTrendIcon(String trend) {
        String upper = trend != null ? trend.toUpperCase(Locale.ROOT) : "";
        if (upper.contains("IMPROV") || upper.contains("UP") || upper.contains("BULL")) {
            return createStyledTrendIcon(VaadinIcon.ARROW_UP, "var(--lumo-success-color)", "Improving (Favorable implication for the thesis)");
        }
        if (upper.contains("DETERIORAT") || upper.contains("DOWN") || upper.contains("DECLIN") || upper.contains("BEAR")) {
            return createStyledTrendIcon(VaadinIcon.ARROW_DOWN, "var(--lumo-error-color)", "Deteriorating (Adverse / higher risk implication)");
        }
        return createStyledTrendIcon(VaadinIcon.ARROW_RIGHT, "#8c9197", "Stable / Neutral");
    }

    private Icon createStyledTrendIcon(VaadinIcon vaadinIcon, String color, String tooltip) {
        Icon icon = vaadinIcon.create();
        icon.setColor(color);
        icon.getStyle().set("color", color);
        icon.setSize("14px");
        icon.getElement().setAttribute("title", tooltip);
        return icon;
    }

    private Component renderFactorScoresSection(JsonNode factorScores) {
        if (factorScores == null || factorScores.isEmpty()) {
            return new Span();
        }
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        section.getStyle().set("margin-bottom", "20px");
        section.add(SnapshotUiSupport.createSectionHeader("Factor Scores"));
        section.add(buildFactorScoresGrid(factorScores));
        return section;
    }

    private Div buildFactorScoresGrid(JsonNode factorScores) {
        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "12px");

        for (Map.Entry<String, JsonNode> entry : factorScores.properties()) {
            double score = entry.getValue().asDouble(0.0);
            if (score > 10.0) {
                score = score / 10.0;
            }
            String title = formatFactorScoreTitle(entry.getKey());
            String badgeText = SnapshotUiSupport.resolveScoreBadgeLabel(score);
            String badgeTheme = SnapshotUiSupport.resolveScoreBadgeTheme(score);
            grid.add(SnapshotUiSupport.createScoreCard(title, score, badgeText, badgeTheme));
        }
        return grid;
    }

    private String formatFactorScoreTitle(String key) {
        if (key == null || key.isBlank()) return "Score";
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "fundamental_health", "fundamental" -> "Fundamental Health";
            case "valuation" -> "Valuation Hurdle";
            case "growth_prospects", "growth" -> "Growth Prospects";
            case "risk_profile", "risk" -> "Risk & Market Setup";
            default -> SnapshotUiSupport.formatDimensionLabel(key);
        };
    }

    private Component renderMetrics(JsonNode value, String title) {
        if (value == null || value.isEmpty()) {
            return new Span();
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().set("margin-bottom", "20px");
        layout.add(SnapshotUiSupport.createSectionHeader(title));

        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(180px, 1fr))")
                .set("grid-auto-flow", "dense")
                .set("gap", "12px");

        for (Map.Entry<String, JsonNode> entry : value.properties()) {
            grid.add(buildMetricCard(entry.getKey(), entry.getValue()));
        }
        layout.add(grid);
        return layout;
    }

    private Div buildMetricCard(String rawKey, JsonNode valNode) {
        Div card = SnapshotUiSupport.createNeutralCard();
        card.getStyle()
                .set("padding", "10px 14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("gap", "6px");

        String valStr = extractMetricString(valNode);
        if (valStr.length() > 65) {
            card.getStyle().set("grid-column", "span 2");
        }

        Span keySpan = new Span(SnapshotUiSupport.formatMetricLabel(rawKey));
        keySpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.74rem")
                .set("font-weight", "600")
                .set("letter-spacing", "0.02em")
                .set("line-height", "1.25");

        card.add(keySpan, buildMetricValueComponent(rawKey, valStr));
        return card;
    }

    private Component buildMetricValueComponent(String rawKey, String valStr) {
        if (isStatusOrEnum(rawKey, valStr)) {
            String theme = SnapshotUiSupport.resolveDimensionTheme(valStr);
            Span badge = SnapshotUiSupport.createStatusBadge(SnapshotUiSupport.formatDimensionLabel(valStr), theme);
            badge.getStyle().set("align-self", "flex-start");
            return badge;
        }
        int colonIdx = valStr.indexOf(':');
        if (colonIdx > 0 && colonIdx <= 25 && valStr.length() > 30) {
            String prefix = valStr.substring(0, colonIdx).trim();
            if (isSentimentPrefix(prefix)) {
                return buildPrefixedDescriptiveBlock(prefix, valStr.substring(colonIdx + 1).trim());
            }
        }
        return valStr.length() > 25 ? buildDescriptiveText(valStr) : buildStandardValueSpan(rawKey, valStr);
    }

    private Component buildPrefixedDescriptiveBlock(String prefix, String body) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("gap", "4px");

        Span badge = SnapshotUiSupport.createStatusBadge(SnapshotUiSupport.formatDimensionLabel(prefix), resolvePrefixTheme(prefix));
        badge.getStyle().set("align-self", "flex-start");

        Span textSpan = new Span(body);
        textSpan.getStyle()
                .set("font-size", "0.80rem")
                .set("font-weight", "500")
                .set("line-height", "1.35")
                .set("color", "var(--lumo-body-text-color)");

        block.add(badge, textSpan);
        return block;
    }

    private Span buildDescriptiveText(String text) {
        Span span = new Span(text);
        span.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "500")
                .set("line-height", "1.35")
                .set("color", "var(--lumo-header-text-color)");
        return span;
    }

    private Span buildStandardValueSpan(String rawKey, String valStr) {
        String displayVal = formatShortMetricValue(rawKey, valStr);
        Span span = new Span(!displayVal.isBlank() ? displayVal : "-");
        span.getStyle()
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)")
                .set("font-size", "1.05rem")
                .set("line-height", "1.2");
        return span;
    }

    private String formatShortMetricValue(String rawKey, String valStr) {
        if (valStr == null || valStr.isBlank()) return "-";
        try {
            double d = Double.parseDouble(valStr);
            String kLower = rawKey.toLowerCase(Locale.ROOT);
            if (kLower.contains("eps") && !kLower.contains("pct") && !kLower.contains("revision") && !kLower.contains("spread")) {
                return String.format(Locale.US, "$%.2f", d);
            }
        } catch (NumberFormatException ignored) {
            // Not a pure number
        }
        return valStr;
    }

    private String extractMetricString(JsonNode valNode) {
        if (valNode == null || valNode.isNull()) return "";
        String s = valNode.isString() ? valNode.asString("") : valNode.asString();
        return s.replace("null down", "0 down").replace("null up", "0 up");
    }

    private boolean isStatusOrEnum(String key, String val) {
        if (val == null || val.isBlank()) return false;
        if (isCategoricalZone(key, val)) return true;
        String kLower = key.toLowerCase(Locale.ROOT);
        if (kLower.contains("status") || kLower.contains("flag") || kLower.contains("divergence")) {
            return true;
        }
        return val.trim().matches("^[A-Z][A-Z0-9_]{2,}$");
    }

    private boolean isCategoricalZone(String key, String val) {
        if (key.toLowerCase(Locale.ROOT).contains("zone")) return true;
        String upper = val.toUpperCase(Locale.ROOT);
        return upper.equals("AGGRESSIVE") || upper.equals("SPECULATIVE") || upper.equals("REALISTIC")
                || upper.equals("CONSERVATIVE") || upper.equals("HIGH") || upper.equals("MODERATE");
    }

    private boolean isSentimentPrefix(String prefix) {
        String p = prefix.trim().toLowerCase(Locale.ROOT);
        return p.contains("mixed") || p.contains("negative") || p.contains("positive")
                || p.contains("bullish") || p.contains("bearish") || p.contains("neutral")
                || p.contains("warning") || p.contains("caution");
    }

    private String resolvePrefixTheme(String prefix) {
        String p = prefix.trim().toLowerCase(Locale.ROOT);
        if (p.contains("bullish") || (p.contains("positive") && !p.contains("mixed"))) {
            return "success";
        }
        if (p.contains("bearish") || (p.contains("negative") && !p.contains("mixed"))) {
            return "error";
        }
        if (p.contains("mixed") || p.contains("warning") || p.contains("caution")) {
            return "warning";
        }
        return "contrast";
    }
}
