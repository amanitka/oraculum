package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.analyst.api.dto.ExecutiveSummaryAgentOutput;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentSnapshotRenderer {

    private final JsonMapper jsonMapper;

    public Component renderInvestmentSnapshot(String snapshotJson, CompanyAnalysisDto analysis) {
        try {
            ExecutiveSummaryAgentOutput snapshot = jsonMapper.readValue(snapshotJson, ExecutiveSummaryAgentOutput.class);
            if (snapshot == null) return null;

            Div container = new Div();
            container.addClassNames(LumoUtility.Width.FULL);

            container.add(createSnapshotHeader(snapshot, analysis));

            if (snapshot.thesis() != null && !snapshot.thesis().isBlank()) {
                container.add(createSnapshotThesis(snapshot.thesis()));
            }

            Component points = createSnapshotPoints(snapshot);
            if (points != null) {
                container.add(points);
            }

            Component footer = createSnapshotFooter(snapshot);
            if (footer != null) {
                container.add(footer);
            }

            return container;
        } catch (Exception e) {
            return null;
        }
    }

    private Component createSnapshotHeader(ExecutiveSummaryAgentOutput snapshot, CompanyAnalysisDto analysis) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "16px");

        H3 title = new H3("Investment Snapshot");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.25rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.5px");

        String verdictText = snapshot.verdict() != null && !snapshot.verdict().isBlank()
                ? snapshot.verdict()
                : (analysis.getRecommendation() != null ? analysis.getRecommendation().name() : "");

        verdictText = RendererUtil.formatKeyTitle(verdictText);

        Span verdictBadge = new Span(verdictText);
        verdictBadge.getElement().getThemeList().add("badge");
        if ("Buy".equalsIgnoreCase(verdictText) || "Bullish".equalsIgnoreCase(verdictText) || verdictText.toUpperCase().contains("BUY")) {
            verdictBadge.getElement().getThemeList().add("success");
        } else if ("Sell".equalsIgnoreCase(verdictText) || "Bearish".equalsIgnoreCase(verdictText) || verdictText.toUpperCase().contains("SELL")) {
            verdictBadge.getElement().getThemeList().add("error");
        } else {
            verdictBadge.getElement().getThemeList().add("contrast");
        }
        verdictBadge.getStyle()
                .set("font-size", "0.95rem")
                .set("padding", "6px 14px")
                .set("font-weight", "bold")
                .set("letter-spacing", "1px");

        HorizontalLayout titleBadge = new HorizontalLayout(title, verdictBadge);
        if (analysis.getOutlook() != null) {
            titleBadge.add(com.oraculum.ui.ViewHelper.outlookBadge(analysis.getOutlook()));
        }
        titleBadge.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBadge.getStyle().set("gap", "12px");

        HorizontalLayout convictionLayout = new HorizontalLayout();
        convictionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        convictionLayout.getStyle().set("gap", "10px");

        int conviction = snapshot.conviction();
        HorizontalLayout meter = new HorizontalLayout();
        meter.setSpacing(false);
        meter.getStyle().set("gap", "4px").set("align-items", "center");

        for (int i = 1; i <= 5; i++) {
            Span bar = new Span();
            bar.getStyle()
                    .set("width", "12px")
                    .set("height", "16px")
                    .set("border-radius", "3px");
            if (i <= conviction) {
                bar.getStyle().set("background", "var(--lumo-primary-color)");
            } else {
                bar.getStyle().set("background", "rgba(255, 255, 255, 0.15)");
            }
            meter.add(bar);
        }

        Span convictionText = new Span("Conviction " + conviction + "/5");
        convictionText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        convictionText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        convictionLayout.add(meter, convictionText);
        header.add(titleBadge, convictionLayout);
        return header;
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

    private Component createSnapshotPoints(ExecutiveSummaryAgentOutput snapshot) {
        HorizontalLayout pointsLayout = new HorizontalLayout();
        pointsLayout.setWidthFull();
        pointsLayout.setSpacing(true);
        pointsLayout.getStyle().set("margin-bottom", "16px");

        if (snapshot.topBullPoints() != null && !snapshot.topBullPoints().isEmpty()) {
            pointsLayout.add(createPointsCard("Key Bull Drivers", snapshot.topBullPoints(), true));
        }
        if (snapshot.topBearPoints() != null && !snapshot.topBearPoints().isEmpty()) {
            pointsLayout.add(createPointsCard("Key Risks & Headwinds", snapshot.topBearPoints(), false));
        }

        return pointsLayout.getComponentCount() > 0 ? pointsLayout : null;
    }

    private Component createPointsCard(String titleText, List<String> items, boolean isBull) {
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

        for (String item : items) {
            ListItem li = new ListItem(item);
            li.getStyle().set("margin-bottom", "6px");
            list.add(li);
        }
        card.add(list);
        return card;
    }

    private Component createSnapshotFooter(ExecutiveSummaryAgentOutput snapshot) {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setSpacing(true);

        boolean hasValuation = snapshot.valuationOneLiner() != null && !snapshot.valuationOneLiner().isBlank();
        boolean hasWatch = snapshot.whatWouldChangeThis() != null && !snapshot.whatWouldChangeThis().isBlank();

        if (!hasValuation && !hasWatch) return null;

        if (hasValuation) {
            footer.add(createFooterPill("Valuation Context", snapshot.valuationOneLiner(), "🏷️", "rgba(255, 255, 255, 0.03)", "rgba(255, 255, 255, 0.1)"));
        }
        if (hasWatch) {
            footer.add(createFooterPill("Watch Trigger", snapshot.whatWouldChangeThis(), "⚡", "rgba(241, 196, 15, 0.05)", "rgba(241, 196, 15, 0.2)"));
        }

        return footer;
    }

    private Component createFooterPill(String title, String content, String iconStr, String bg, String borderColor) {
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

        Span contentSpan = new Span(content);
        contentSpan.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        box.add(iconSpan, titleSpan, contentSpan);
        return box;
    }
}
