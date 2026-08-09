package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisOverviewRenderer {

    private final MarkdownRenderer markdownRenderer;

    public Component renderAnalysisOverview(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        if (snapshot == null) return null;

        Div container = new Div();
        container.addClassNames(LumoUtility.Width.FULL);

        container.add(createSnapshotHeader(snapshot, analysis));

        if (snapshot.thesis() != null && !snapshot.thesis().isBlank()) {
            container.add(createSnapshotThesis(snapshot.thesis()));
        }

        if (snapshot.factorScores() != null && !snapshot.factorScores().isEmpty()) {
            container.add(createFactorScoresCard(snapshot.factorScores()));
        }

        Component points = createSnapshotPoints(snapshot, analysis);
        if (points != null) {
            container.add(points);
        }

        Component footer = createSnapshotFooter(snapshot, analysis);
        if (footer != null) {
            container.add(footer);
        }

        return container;
    }

    private Component createSnapshotHeader(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();

        H4 headerTitle = new H4("Overview");
        headerTitle.getStyle()
                .set("margin-top", "0")
                .set("margin-bottom", "20px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "10px")
                .set("font-size", "1.15rem")
                .set("color", "var(--lumo-header-text-color)");

        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.getStyle().set("margin-top", "6px").set("margin-bottom", "16px");

        HorizontalLayout badges = new HorizontalLayout();
        badges.setAlignItems(FlexComponent.Alignment.CENTER);
        badges.getStyle().set("gap", "12px");

        ValuationAssessment valuation = snapshot.valuation();
        String verdictText = valuation != null ? valuation.getDisplayLabel() : "Pending";

        Span verdictBadge = new Span(verdictText);
        verdictBadge.getElement().getThemeList().add("badge");
        if (valuation == ValuationAssessment.UNDERVALUED) {
            verdictBadge.getElement().getThemeList().add("success");
        } else if (valuation == ValuationAssessment.OVERVALUED) {
            verdictBadge.getElement().getThemeList().add("error");
        } else {
            verdictBadge.getElement().getThemeList().add("contrast");
        }
        verdictBadge.getStyle()
                .set("font-size", "0.95rem")
                .set("padding", "6px 14px")
                .set("font-weight", "bold")
                .set("letter-spacing", "1px");

        badges.add(verdictBadge);
        if (snapshot.outlook() != null) {
            badges.add(ViewHelper.outlookBadge(snapshot.outlook()));
        }

        topRow.add(badges, buildConvictionMeter(snapshot.conviction()));
        container.add(headerTitle, topRow);
        return container;
    }

    private HorizontalLayout buildConvictionMeter(int conviction) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("gap", "10px");

        HorizontalLayout meter = new HorizontalLayout();
        meter.setSpacing(false);
        meter.getStyle().set("gap", "4px").set("align-items", "center");

        for (int i = 1; i <= 5; i++) {
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "12px")
                    .set("height", "16px")
                    .set("border-radius", "3px")
                    .set("background", i <= conviction ? "var(--lumo-primary-color)" : "rgba(255, 255, 255, 0.15)");
            meter.add(bar);
        }

        Span convictionText = new Span("Conviction " + conviction + "/5");
        convictionText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        convictionText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        layout.add(meter, convictionText);
        return layout;
    }

    private Component createSnapshotThesis(String thesis) {
        Div thesisCard = new Div();
        thesisCard.setWidthFull();
        thesisCard.getStyle()
                .set("background", "rgba(var(--lumo-primary-color-rgb), 0.05)")
                .set("border-left", "3px solid var(--lumo-primary-color)")
                .set("border-radius", "0 8px 8px 0")
                .set("padding", "12px 16px")
                .set("margin-bottom", "16px");

        Paragraph p = new Paragraph(thesis);
        p.getStyle()
                .set("margin", "0")
                .set("font-size", "0.95rem")
                .set("line-height", "1.6")
                .set("color", "var(--lumo-header-text-color)");

        thesisCard.add(p);
        return thesisCard;
    }

    private Component createFactorScoresCard(Map<String, Double> factorScores) {
        HorizontalLayout scoresLayout = new HorizontalLayout();
        scoresLayout.setWidthFull();
        scoresLayout.getStyle()
                .set("gap", "12px")
                .set("margin-bottom", "16px")
                .set("flex-wrap", "wrap");

        factorScores.forEach((factor, score) -> {
            Div tile = new Div();
            tile.getStyle()
                    .set("flex", "1 1 180px")
                    .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "8px")
                    .set("padding", "10px 14px");

            Span titleSpan = new Span(RendererUtil.formatKeyTitle(factor));
            titleSpan.getStyle()
                    .set("font-size", "0.8rem")
                    .set("font-weight", "600")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("display", "block")
                    .set("margin-bottom", "6px");

            tile.add(titleSpan, renderScoreBarMeter(score));
            scoresLayout.add(tile);
        });

        return scoresLayout;
    }

    private HorizontalLayout renderScoreBarMeter(double rawScore) {
        double score = rawScore > 10.0 ? rawScore / 10.0 : rawScore;
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("gap", "8px");

        HorizontalLayout bars = new HorizontalLayout();
        bars.setSpacing(false);
        bars.getStyle().set("gap", "3px").set("align-items", "center");

        int fullBlocks = (int) Math.round(score);
        for (int i = 1; i <= 10; i++) {
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "8px")
                    .set("height", "13px")
                    .set("border-radius", "2px");

            if (i <= fullBlocks) {
                if (i <= 3) {
                    bar.getStyle().set("background", "var(--lumo-error-color)");
                } else if (i <= 7) {
                    bar.getStyle().set("background", "var(--lumo-warning-color)");
                } else {
                    bar.getStyle().set("background", "var(--lumo-success-color)");
                }
            } else {
                bar.getStyle().set("background", "rgba(255, 255, 255, 0.12)");
            }
            bars.add(bar);
        }

        Span scoreText = new Span(String.format(Locale.US, "%.1f", score));
        scoreText.getStyle()
                .set("font-size", "0.85rem")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-header-text-color)");

        layout.add(bars, scoreText);
        return layout;
    }

    private Component createSnapshotPoints(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        HorizontalLayout pointsLayout = new HorizontalLayout();
        pointsLayout.setWidthFull();
        pointsLayout.setSpacing(true);
        pointsLayout.getStyle().set("margin-bottom", "16px");

        if (snapshot.topBullPoints() != null && !snapshot.topBullPoints().isEmpty()) {
            pointsLayout.add(createPointsCard("Key Bull Drivers", snapshot.topBullPoints(), true, analysis));
        }
        if (snapshot.topBearPoints() != null && !snapshot.topBearPoints().isEmpty()) {
            pointsLayout.add(createPointsCard("Key Risks & Headwinds", snapshot.topBearPoints(), false, analysis));
        }

        return pointsLayout.getComponentCount() > 0 ? pointsLayout : null;
    }

    private Component createPointsCard(String titleText, List<String> items, boolean isBull, CompanyAnalysisDto analysis) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        String bg = isBull ? "rgba(46, 204, 113, 0.05)" : "rgba(231, 76, 60, 0.05)";
        String borderColor = isBull ? "rgba(46, 204, 113, 0.2)" : "rgba(231, 76, 60, 0.2)";
        String headerColor = isBull ? "#2ecc71" : "#e74c3c";
        String icon = isBull ? "▲ " : "▼ ";

        card.getStyle()
                .set("background", bg)
                .set("border", "1px solid " + borderColor)
                .set("border-radius", "8px")
                .set("padding", "14px 16px");

        H5 header = new H5(icon + titleText);
        header.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", headerColor)
                .set("font-size", "0.85rem")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");
        card.add(header);

        UnorderedList list = new UnorderedList();
        list.getStyle()
                .set("margin", "0")
                .set("padding-left", "1.2rem")
                .set("font-size", "0.88rem")
                .set("line-height", "1.55");

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        for (String item : items) {
            ListItem li = new ListItem();
            li.getStyle().set("margin-bottom", "6px");
            li.add(markdownRenderer.renderMarkdownWithCitations(item, jsonData));
            list.add(li);
        }
        card.add(list);
        return card;
    }

    private Component createSnapshotFooter(AnalysisResult snapshot, CompanyAnalysisDto analysis) {
        boolean hasValuation = snapshot.valuationOneLiner() != null && !snapshot.valuationOneLiner().isBlank();
        boolean hasWatch = snapshot.whatWouldChangeThis() != null && !snapshot.whatWouldChangeThis().isBlank();

        if (!hasValuation && !hasWatch) return null;

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setSpacing(true);

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        if (hasValuation) {
            footer.add(createFooterPill("Valuation Context", snapshot.valuationOneLiner(),
                    "🏷️", "rgba(255, 255, 255, 0.03)", "rgba(255, 255, 255, 0.1)", jsonData));
        }
        if (hasWatch) {
            footer.add(createFooterPill("Watch Trigger", snapshot.whatWouldChangeThis(),
                    "⚡", "rgba(241, 196, 15, 0.05)", "rgba(241, 196, 15, 0.2)", jsonData));
        }

        return footer;
    }

    private Component createFooterPill(String title, String content, String iconStr, String bg, String borderColor, String jsonData) {
        Div box = new Div();
        box.setWidthFull();
        box.getStyle()
                .set("background", bg)
                .set("border", "1px solid " + borderColor)
                .set("border-radius", "8px")
                .set("padding", "10px 14px");

        Span iconSpan = new Span(iconStr + " ");
        Span titleSpan = new Span(title + ": ");
        titleSpan.getStyle().set("font-weight", "700").set("font-size", "0.85rem");

        Component contentComponent = markdownRenderer.renderMarkdownWithCitations(content, jsonData);
        contentComponent.getStyle().set("display", "inline").set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        box.add(iconSpan, titleSpan, contentComponent);
        return box;
    }
}
