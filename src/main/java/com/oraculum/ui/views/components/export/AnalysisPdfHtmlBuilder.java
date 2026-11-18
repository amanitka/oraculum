package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.ui.views.components.export.AnalysisPdfCitationHelper.CitationEntry;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.VerdictStanceResolver;
import com.oraculum.ui.views.components.renderer.VerdictStanceResolver.VerdictStance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Assembles the complete XHTML investment memo document for a completed company analysis.
 */
@Component
@RequiredArgsConstructor
public class AnalysisPdfHtmlBuilder {

    private final MarkdownRenderer markdownRenderer;
    private final AnalysisPdfSnapshotSectionBuilder snapshotSectionBuilder;

    public String buildXhtml(CompanyAnalysisViewDto analysis, String css) {
        AnalysisResult result = analysis.getAnalysisResult();
        JsonNode citationsNode = markdownRenderer.extractCitationsNode(analysis.getAnalysisData());
        String displayTitle = resolveCompanyTitle(analysis);

        StringBuilder body = new StringBuilder();
        body.append(buildRunningHeaders(displayTitle, analysis.getAnalysisDate()));
        body.append(buildDocumentHeader(analysis, displayTitle));
        body.append(buildVerdictHero(result, citationsNode));
        body.append(snapshotSectionBuilder.buildSnapshotSections(result, citationsNode));
        body.append(buildNarrativeSection("Executive Summary", result.executiveSummary(), citationsNode));
        body.append(buildNarrativeSection("Valuation Justification", result.recommendationReasoning(), citationsNode));

        List<CitationEntry> citations = AnalysisPdfCitationHelper.collectReferencedCitations(result, citationsNode);
        body.append(AnalysisPdfCitationHelper.renderCitationsAppendix(citations));

        return PdfHtmlUtil.wrapXhtmlDocument(displayTitle + " — Financial Analysis", css, body.toString());
    }

    private String resolveCompanyTitle(CompanyAnalysisViewDto analysis) {
        String ticker = !PdfHtmlUtil.isBlank(analysis.getTicker()) ? analysis.getTicker() : "Company";
        if (PdfHtmlUtil.isBlank(analysis.getCompanyName())) {
            return ticker;
        }
        return analysis.getCompanyName() + " (" + ticker + ")";
    }

    private String buildRunningHeaders(String displayTitle, LocalDate analysisDate) {
        String dateText = analysisDate != null ? analysisDate.toString() : "";
        return "<div class=\"running-header-left\">" + PdfHtmlUtil.escape(displayTitle) + "</div>"
                + "<div class=\"running-header-right\">Analysis Date: " + PdfHtmlUtil.escape(dateText) + "</div>";
    }

    private String buildDocumentHeader(CompanyAnalysisViewDto analysis, String displayTitle) {
        String meta = joinMetadata(analysis);
        String dateText = analysis.getAnalysisDate() != null ? analysis.getAnalysisDate().toString() : "-";
        return "<div class=\"doc-header\"><table class=\"doc-header-table\"><tr>"
                + "<td class=\"doc-header-left\">"
                + "<div class=\"doc-brand\">Oraculum Investment Research</div>"
                + "<h1 class=\"doc-title\">" + PdfHtmlUtil.escape(displayTitle) + "</h1>"
                + (meta.isEmpty() ? "" : "<div class=\"doc-meta\">" + PdfHtmlUtil.escape(meta) + "</div>")
                + "</td>"
                + "<td class=\"doc-header-right\">"
                + "<div class=\"doc-date-label\">Analysis Date</div>"
                + "<div class=\"doc-date-val\">" + PdfHtmlUtil.escape(dateText) + "</div>"
                + "</td></tr></table></div>";
    }

    private String joinMetadata(CompanyAnalysisViewDto analysis) {
        List<String> parts = new ArrayList<>();
        if (!PdfHtmlUtil.isBlank(analysis.getMarket())) {
            parts.add("Market: " + analysis.getMarket());
        }
        if (!PdfHtmlUtil.isBlank(analysis.getSectorName())) {
            parts.add("Sector: " + analysis.getSectorName());
        }
        if (!PdfHtmlUtil.isBlank(analysis.getIndustryName())) {
            parts.add("Industry: " + analysis.getIndustryName());
        }
        return String.join("  |  ", parts);
    }

    private String buildVerdictHero(AnalysisResult result, JsonNode citationsNode) {
        VerdictStance stance = VerdictStanceResolver.deriveStance(result.valuation(), result.outlook(), result.conviction());
        String heroStyle = "border-left-color:" + stance.accentColor() + ";";
        StringBuilder sb = new StringBuilder("<div class=\"hero-card\" style=\"" + heroStyle + "\">");
        sb.append("<table class=\"hero-top-table\"><tr>")
                .append(buildVerdictLeftCell(result, stance))
                .append(buildVerdictRightCell(result))
                .append("</tr></table>");
        if (!PdfHtmlUtil.isBlank(result.thesis())) {
            sb.append("<div class=\"hero-thesis\">")
                    .append(PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, result.thesis(), citationsNode))
                    .append("</div>");
        }
        sb.append(buildDecisiveStrip(result.decisionSummary()));
        return sb.append("</div>").toString();
    }

    private String buildVerdictLeftCell(AnalysisResult result, VerdictStance stance) {
        String badgeStyle = "color:" + stance.accentColor() + ";border-color:" + stance.accentColor() + ";background-color:#ffffff;";
        String outlookText = result.outlook() != null ? result.outlook().getDisplayName() : "Pending";
        String valText = result.valuation() != null ? result.valuation().getDisplayLabel() : "Pending";
        return "<td class=\"hero-top-left\">"
                + "<span class=\"stance-badge\" style=\"" + badgeStyle + "\">" + PdfHtmlUtil.escape(stance.label()) + "</span>"
                + "<div class=\"hero-why\">" + PdfHtmlUtil.escape(stance.summaryWhy()) + "</div>"
                + "<div class=\"hero-dims\">Business Outlook: <strong>" + PdfHtmlUtil.escape(outlookText)
                + "</strong>  |  Valuation Setup: <strong>" + PdfHtmlUtil.escape(valText) + "</strong></div>"
                + "</td>";
    }

    private String buildVerdictRightCell(AnalysisResult result) {
        DecisionSummary s = result.decisionSummary();
        StringBuilder sb = new StringBuilder("<td class=\"hero-top-right\">");
        if (s != null && s.currentSharePrice() != null) {
            sb.append("<div class=\"hero-price\">").append(String.format(Locale.US, "$%.2f", s.currentSharePrice())).append("</div>");
            String priceMeta = s.priceDate() != null ? "Ref. price: " + s.priceDate() : "Ref. share price";
            sb.append("<div class=\"hero-sub\">").append(PdfHtmlUtil.escape(priceMeta)).append("</div>");
        }
        sb.append("<div class=\"conviction-text\">Conviction: ").append(result.conviction()).append("/5</div>");
        sb.append("<div class=\"hero-sub\">").append(PdfHtmlUtil.escape(formatConvictionLevel(result))).append("</div>");
        return sb.append("</td>").toString();
    }

    private String formatConvictionLevel(AnalysisResult result) {
        int conviction = result.conviction();
        String level = conviction >= 4 ? "High confidence" : (conviction == 3 ? "Moderate" : "Low visibility");
        List<String> reasons = result.convictionReasons();
        long caps = reasons == null ? 0 : reasons.stream().filter(r -> r != null && r.startsWith("Capped")).count();
        return caps > 0 ? level + " (" + caps + (caps == 1 ? " guardrail)" : " guardrails)") : level;
    }

    private String buildDecisiveStrip(DecisionSummary s) {
        if (s == null || (PdfHtmlUtil.isBlank(s.decisiveQuestion()) && PdfHtmlUtil.isBlank(s.nextCatalyst()))) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"question-strip\">");
        if (!PdfHtmlUtil.isBlank(s.decisiveQuestion())) {
            sb.append("<div class=\"question-label\">Decisive Question</div>")
                    .append("<div class=\"question-val\">").append(PdfHtmlUtil.escape(s.decisiveQuestion())).append("</div>");
        }
        if (!PdfHtmlUtil.isBlank(s.nextCatalyst())) {
            String cleanCatalyst = s.nextCatalyst().replaceFirst("(?i)^Next Quarter End:\\s*", "Qtr End ");
            sb.append("<div class=\"hero-sub\" style=\"margin-top:3pt;\"><strong>Next Catalyst:</strong> ")
                    .append(PdfHtmlUtil.escape(cleanCatalyst)).append("</div>");
        }
        return sb.append("</div>").toString();
    }

    private String buildNarrativeSection(String title, String markdown, JsonNode citationsNode) {
        if (PdfHtmlUtil.isBlank(markdown)) {
            return "";
        }
        return "<h2 class=\"section-title\">" + PdfHtmlUtil.escape(title) + "</h2>"
                + "<div class=\"markdown-body\">"
                + PdfHtmlUtil.renderMarkdownXhtml(markdownRenderer, markdown, citationsNode)
                + "</div>";
    }
}
