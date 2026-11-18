package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.ui.views.components.renderer.AnalysisOverviewRenderer;
import com.oraculum.ui.views.components.renderer.DecisionSnapshotRenderer;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.SnapshotUiSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders structured executive snapshot sections (pillars, expectations, bull/bear,
 * valuation drivers, conviction audit, scenarios, and thesis breakers) as PDF-ready HTML.
 */
@Component
@RequiredArgsConstructor
public class AnalysisPdfSnapshotSectionBuilder {

    private final MarkdownRenderer markdownRenderer;

    public String buildSnapshotSections(AnalysisResult result, JsonNode citationsNode) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildPillarsSection(result));
        sb.append(buildImpliedExpectationsSection(result.decisionSummary()));
        sb.append(buildBullBearSection(result, citationsNode));
        sb.append(buildWatchTriggerBar(result.whatWouldChangeThis(), citationsNode));
        sb.append(buildValuationAndDriversSection(result, citationsNode));
        sb.append(buildScenariosSection(result.scenarios(), citationsNode));
        sb.append(buildThesisBreakersSection(result.thesisBreakers(), citationsNode));
        return sb.toString();
    }

    private String buildPillarsSection(AnalysisResult result) {
        DecisionSummary s = result.decisionSummary();
        Map<String, Double> scores = AnalysisOverviewRenderer.normalizeFactorScores(result.factorScores());
        if (s == null && scores.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"avoid-break\"><table class=\"grid-table\"><tr>");
        if (s == null) {
            appendLegacyPillarCells(sb, scores);
            return sb.append("</tr></table></div>").toString();
        }
        appendUnifiedPillarCells(sb, scores, s);
        return sb.append("</tr></table></div>").toString();
    }

    private void appendUnifiedPillarCells(StringBuilder sb, Map<String, Double> scores, DecisionSummary s) {
        double fund = scores.getOrDefault("fundamental_health", 0.0);
        double val = scores.getOrDefault("valuation", 0.0);
        double growth = scores.getOrDefault("growth_prospects", 0.0);
        double risk = scores.getOrDefault("risk_profile", 0.0);
        sb.append(buildPillarCell("Fundamental Health", fund, s.businessQuality(), SnapshotUiSupport.resolveDimensionTheme(s.businessQuality())));
        sb.append(buildPillarCell("Valuation Attractiveness", val, s.valuationVsExpectations(), SnapshotUiSupport.resolveDimensionTheme(s.valuationVsExpectations())));
        sb.append(buildPillarCell("Growth Prospects", growth, DecisionSnapshotRenderer.resolveGrowthLabel(growth, s), DecisionSnapshotRenderer.resolveGrowthTheme(growth)));
        sb.append(buildPillarCell("Risk Resilience & Setup", risk, DecisionSnapshotRenderer.resolveRiskAndSetupLabel(risk, s.marketSetup()), SnapshotUiSupport.resolveScoreBadgeTheme(risk)));
    }

    private void appendLegacyPillarCells(StringBuilder sb, Map<String, Double> scores) {
        scores.forEach((factor, score) -> {
            String title = SnapshotUiSupport.formatDimensionLabel(factor);
            String badge = SnapshotUiSupport.resolveScoreBadgeLabel(factor, score);
            sb.append(buildPillarCell(title, score, badge, SnapshotUiSupport.resolveScoreBadgeTheme(score)));
        });
    }

    private String buildPillarCell(String title, double score, String badgeText, String badgeTheme) {
        double pct = Math.clamp(score * 10.0, 0.0, 100.0);
        String fill = score >= 7.5 ? "#16a34a" : (score >= 4.5 ? "#d97706" : "#dc2626");
        String badgeHtml = PdfHtmlUtil.isBlank(badgeText) ? ""
                : "<span class=\"" + PdfHtmlUtil.badgeCssClass(badgeTheme) + "\">"
                + PdfHtmlUtil.escape(SnapshotUiSupport.formatDimensionLabel(badgeText)) + "</span>";
        return "<td class=\"pillar-cell\">"
                + "<div class=\"pillar-title\">" + PdfHtmlUtil.escape(title) + "</div>"
                + "<div class=\"pillar-score\">" + String.format(Locale.US, "%.1f", score) + "</div>"
                + "<div class=\"score-track\"><div class=\"score-fill\" style=\"width:"
                + String.format(Locale.US, "%.0f%%", pct) + ";background-color:" + fill + ";\"></div></div>"
                + badgeHtml + "</td>";
    }

    private String buildImpliedExpectationsSection(DecisionSummary s) {
        if (s == null) {
            return "";
        }
        boolean hasDcf = s.requiredFcfCagrPct() != null;
        boolean hasEps = s.impliedEpsByMultiple() != null && !s.impliedEpsByMultiple().isEmpty();
        if (!hasDcf && !hasEps) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"card\">");
        sb.append("<div class=\"sub-heading\" style=\"margin-top:0;\">What Today&#39;s Price Requires</div>");
        if (hasDcf) {
            sb.append(buildDcfSummaryLine(s));
        }
        if (hasEps) {
            sb.append(buildEpsTable(s.targetFiscalYear(), s.impliedEpsByMultiple()));
        }
        return sb.append("</div>").toString();
    }

    private String buildDcfSummaryLine(DecisionSummary s) {
        StringBuilder extra = new StringBuilder();
        if (s.ttmFcfGrowthPct() != null) {
            extra.append(String.format(Locale.US, " | Recent TTM: %.1f%%", s.ttmFcfGrowthPct()));
        }
        if (s.historicalFcfCagrPct() != null) {
            extra.append(String.format(Locale.US, " | 5Y CAGR: %.1f%%", s.historicalFcfCagrPct()));
        }
        return "<div style=\"margin-bottom:5pt;\"><strong>10Y Reverse DCF Hurdle:</strong> "
                + String.format(Locale.US, "%.1f%% FCF CAGR", s.requiredFcfCagrPct())
                + "<span style=\"color:#475569;font-size:8pt;\">" + extra + "</span></div>";
    }

    private String buildEpsTable(String targetFy, List<ImpliedEpsSnapshot> entries) {
        String fyLabel = !PdfHtmlUtil.isBlank(targetFy) ? targetFy : "Target FY";
        Float consensus = entries.getFirst().consensusEpsAvg();
        String consHeader = consensus != null ? String.format(Locale.US, " (Consensus: $%.2f)", consensus) : "";
        StringBuilder sb = new StringBuilder("<table class=\"data-table\"><thead><tr>");
        sb.append("<th>Multiple Anchor (").append(PdfHtmlUtil.escape(fyLabel)).append(")</th>");
        sb.append("<th>Required EPS").append(PdfHtmlUtil.escape(consHeader)).append("</th>");
        sb.append("<th>Consensus Buffer</th></tr></thead><tbody>");
        for (ImpliedEpsSnapshot e : entries) {
            appendEpsRow(sb, e);
        }
        return sb.append("</tbody></table>").toString();
    }

    private void appendEpsRow(StringBuilder sb, ImpliedEpsSnapshot e) {
        String anchor = DecisionSnapshotRenderer.formatShortAnchorLabel(e);
        float req = e.requiredEpsForCurrentPrice() != null ? e.requiredEpsForCurrentPrice() : 0.0f;
        float buf = e.consensusVsRequiredPct() != null ? e.consensusVsRequiredPct() : 0.0f;
        String bufColor = buf >= 15.0f ? "#15803d" : (buf >= 0.0f ? "#475569" : "#b91c1c");
        sb.append("<tr><td><strong>").append(PdfHtmlUtil.escape(anchor)).append("</strong></td>")
                .append("<td>").append(String.format(Locale.US, "$%.2f", req)).append("</td>")
                .append("<td style=\"font-weight:700;color:").append(bufColor).append(";\">")
                .append(String.format(Locale.US, "%+.1f%%", buf)).append("</td></tr>");
    }

    private String buildBullBearSection(AnalysisResult result, JsonNode citationsNode) {
        boolean hasBull = result.topBullPoints() != null && !result.topBullPoints().isEmpty();
        boolean hasBear = result.topBearPoints() != null && !result.topBearPoints().isEmpty();
        if (!hasBull && !hasBear) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"avoid-break\"><table class=\"grid-table\"><tr>");
        if (hasBull) {
            sb.append(buildPointsCell("Key Bull Drivers", result.topBullPoints(), true, citationsNode));
        }
        if (hasBear) {
            sb.append(buildPointsCell("Key Risks &amp; Headwinds", result.topBearPoints(), false, citationsNode));
        }
        return sb.append("</tr></table></div>").toString();
    }

    private String buildPointsCell(String title, List<String> points, boolean isBull, JsonNode citationsNode) {
        String cardClass = isBull ? "card-cell card-bull" : "card-cell card-bear";
        String titleClass = isBull ? "card-title title-green" : "card-title title-red";
        String prefix = isBull ? "+ " : "- ";
        StringBuilder sb = new StringBuilder("<td class=\"" + cardClass + "\">");
        sb.append("<div class=\"").append(titleClass).append("\">").append(prefix).append(title).append("</div>");
        sb.append("<ul class=\"bullet-list\">");
        for (String point : points) {
            sb.append("<li>").append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, point, citationsNode)).append("</li>");
        }
        return sb.append("</ul></td>").toString();
    }

    private String buildWatchTriggerBar(String watchText, JsonNode citationsNode) {
        if (PdfHtmlUtil.isBlank(watchText)) {
            return "";
        }
        return "<div class=\"watch-bar\"><strong style=\"color:#b45309;\">Watch Trigger: </strong>"
                + PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, watchText, citationsNode) + "</div>";
    }

    private String buildValuationAndDriversSection(AnalysisResult result, JsonNode citationsNode) {
        String contextRow = buildValuationContextRow(result, citationsNode);
        String driversGrid = buildDecisionDriversGrid(result.decisionDrivers());
        String auditCard = buildConvictionAuditCard(result.conviction(), result.convictionReasons());
        if (contextRow.isEmpty() && driversGrid.isEmpty() && auditCard.isEmpty()) {
            return "";
        }
        return "<h2 class=\"section-title\">Valuation &amp; Decision Drivers</h2>" + contextRow + driversGrid + auditCard;
    }

    private String buildValuationContextRow(AnalysisResult result, JsonNode citationsNode) {
        boolean hasContext = !PdfHtmlUtil.isBlank(result.valuationOneLiner());
        DecisionSummary s = result.decisionSummary();
        boolean hasTension = s != null && !PdfHtmlUtil.isBlank(s.keyValuationTension());
        if (!hasContext && !hasTension) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"avoid-break\"><table class=\"grid-table\"><tr>");
        if (hasContext) {
            sb.append("<td class=\"card-cell card-info\"><div class=\"card-title title-blue\">Valuation Context</div>")
                    .append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, result.valuationOneLiner(), citationsNode))
                    .append("</td>");
        }
        if (hasTension) {
            sb.append(buildMethodologyCell(s));
        }
        return sb.append("</tr></table></div>").toString();
    }

    private String buildMethodologyCell(DecisionSummary s) {
        boolean aligned = "ALIGNED".equalsIgnoreCase(s.methodAgreement());
        String cardClass = aligned ? "card-cell card-bull" : "card-cell card-warn";
        String titleClass = aligned ? "card-title title-green" : "card-title title-amber";
        String badge = s.methodAgreement() == null ? ""
                : " <span class=\"" + PdfHtmlUtil.badgeCssClass(aligned ? "success" : "warning") + "\">"
                + PdfHtmlUtil.escape(SnapshotUiSupport.formatDimensionLabel(s.methodAgreement())) + "</span>";
        return "<td class=\"" + cardClass + "\"><div class=\"" + titleClass + "\">Methodology Reconciliation"
                + badge + "</div><div>" + PdfHtmlUtil.escape(s.keyValuationTension()) + "</div></td>";
    }

    private String buildDecisionDriversGrid(List<DecisionDriver> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"sub-heading\">Decision Drivers &amp; Monitoring Triggers</div>");
        sb.append("<table class=\"grid-table grid-table-rows\">");
        for (int i = 0; i < drivers.size(); i += 2) {
            sb.append("<tr>").append(buildDriverCell(drivers.get(i)));
            sb.append(i + 1 < drivers.size() ? buildDriverCell(drivers.get(i + 1)) : "<td></td>").append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private String buildDriverCell(DecisionDriver d) {
        String theme = SnapshotUiSupport.resolveDimensionTheme(d.status());
        String cardClass = "success".equals(theme) ? "card-cell card-bull"
                : ("error".equals(theme) ? "card-cell card-bear" : "card-cell card-warn");
        return "<td class=\"" + cardClass + "\"><div><strong>" + PdfHtmlUtil.escape(d.factor()) + "</strong> "
                + "<span class=\"" + PdfHtmlUtil.badgeCssClass(theme) + "\">"
                + PdfHtmlUtil.escape(SnapshotUiSupport.formatDimensionLabel(d.status())) + "</span></div>"
                + "<div class=\"driver-meta\"><strong>Current:</strong> " + PdfHtmlUtil.escape(orDash(d.currentValue()))
                + "<br/><strong>Benchmark / Hurdle:</strong> " + PdfHtmlUtil.escape(orDash(d.benchmarkOrRequired())) + "</div>"
                + "<div class=\"driver-trigger\"><strong>Trigger:</strong> "
                + PdfHtmlUtil.escape(orDash(d.whatChangesView())) + "</div></td>";
    }

    private String buildConvictionAuditCard(int conviction, List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        String suffix = conviction > 0 ? " (Final Conviction " + conviction + "/5)" : "";
        StringBuilder sb = new StringBuilder("<div class=\"card card-info\">");
        sb.append("<div class=\"card-title title-blue\">Conviction Audit &amp; Active Guardrails")
                .append(PdfHtmlUtil.escape(suffix)).append("</div><ul class=\"bullet-list\">");
        for (String reason : reasons) {
            sb.append("<li>").append(PdfHtmlUtil.escape(reason)).append("</li>");
        }
        return sb.append("</ul></div>").toString();
    }

    private String buildScenariosSection(ScenariosResult scenarios, JsonNode citationsNode) {
        if (scenarios == null || (scenarios.bull() == null && scenarios.base() == null && scenarios.bear() == null)) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<h2 class=\"section-title\">Scenarios &amp; Risk Framing</h2>");
        sb.append("<div class=\"avoid-break\"><table class=\"grid-table\"><tr>");
        if (scenarios.bull() != null) {
            sb.append(buildScenarioCell("Bull Case", scenarios.bull().description(), scenarios.bull().keyAssumption(),
                    scenarios.bull().impliedUpside(), "card-bull", "title-green", citationsNode));
        }
        if (scenarios.base() != null) {
            sb.append(buildScenarioCell("Base Case", scenarios.base().description(), scenarios.base().keyAssumption(),
                    scenarios.base().impliedMove(), "card-info", "title-blue", citationsNode));
        }
        if (scenarios.bear() != null) {
            sb.append(buildScenarioCell("Bear Case", scenarios.bear().description(), scenarios.bear().keyAssumption(),
                    scenarios.bear().impliedDownside(), "card-bear", "title-red", citationsNode));
        }
        return sb.append("</tr></table></div>").toString();
    }

    private String buildScenarioCell(String title, String desc, String assumption, String move,
                                     String cardVariant, String titleVariant, JsonNode citationsNode) {
        StringBuilder sb = new StringBuilder("<td class=\"card-cell " + cardVariant + "\">");
        sb.append("<div class=\"card-title ").append(titleVariant).append("\">").append(title).append("</div>");
        if (!PdfHtmlUtil.isBlank(desc)) {
            sb.append("<div>").append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, desc, citationsNode)).append("</div>");
        }
        if (!PdfHtmlUtil.isBlank(assumption)) {
            sb.append("<div class=\"scenario-label\">Key Assumption</div><div>")
                    .append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, assumption, citationsNode)).append("</div>");
        }
        if (!PdfHtmlUtil.isBlank(move)) {
            sb.append("<div class=\"scenario-move ").append(titleVariant).append("\">")
                    .append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, move, citationsNode)).append("</div>");
        }
        return sb.append("</td>").toString();
    }

    private String buildThesisBreakersSection(List<String> breakers, JsonNode citationsNode) {
        if (breakers == null || breakers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"card card-warn\">");
        sb.append("<div class=\"card-title title-amber\">Thesis Breakers &amp; Monitoring Triggers</div>");
        sb.append("<ul class=\"bullet-list\">");
        for (String breaker : breakers) {
            sb.append("<li>").append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, breaker, citationsNode)).append("</li>");
        }
        return sb.append("</ul></div>").toString();
    }

    private String orDash(String value) {
        return !PdfHtmlUtil.isBlank(value) ? value : "-";
    }
}
