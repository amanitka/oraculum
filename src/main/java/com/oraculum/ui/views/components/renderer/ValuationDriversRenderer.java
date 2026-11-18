package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Locale;

/**
 * Renders the Valuation Context, Methodology Reconciliation, Decision Drivers,
 * and Conviction Audit cards for the Valuation &amp; Drivers tab.
 */
final class ValuationDriversRenderer {

    private record DriverPalette(String accent, String bgTint, String borderTint) {
    }

    Component renderValuationAndDriversTab(AnalysisResult snapshot, CompanyAnalysisDto analysis, MarkdownRenderer markdownRenderer) {
        if (snapshot == null) {
            return null;
        }
        Div root = new Div();
        root.setWidthFull();

        addIfNotNull(root, renderValuationContextRow(snapshot, analysis, markdownRenderer));
        addIfNotNull(root, renderDecisionDriversGrid(snapshot.decisionDrivers()));
        addIfNotNull(root, renderConvictionAuditCard(snapshot.conviction(), snapshot.convictionReasons()));
        return root;
    }

    Component renderDecisionDriversGrid(List<DecisionDriver> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            return null;
        }
        Div section = new Div();
        section.setWidthFull();
        section.getStyle().set("margin-bottom", "20px");

        H5 heading = new H5("Decision Drivers & Monitoring Triggers");
        heading.getStyle().set("margin", "0 0 12px 0").set("font-size", "0.95rem").set("color", "var(--lumo-header-text-color)");

        Div grid = new Div();
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, minmax(0, 1fr))")
                .set("gap", "16px");

        for (DecisionDriver driver : drivers) {
            grid.add(buildCleanDriverCard(driver));
        }
        section.add(heading, grid);
        return section;
    }

    Component renderConvictionAuditCard(int conviction, List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(52, 152, 219, 0.05)")
                .set("border", "1px solid rgba(52, 152, 219, 0.22)")
                .set("border-radius", "8px");

        card.add(buildAuditTitleRow(conviction));
        for (String reason : reasons) {
            card.add(buildAuditBulletRow(reason));
        }
        return card;
    }

    private Component renderValuationContextRow(AnalysisResult snapshot, CompanyAnalysisDto analysis, MarkdownRenderer markdownRenderer) {
        boolean hasContext = snapshot.valuationOneLiner() != null && !snapshot.valuationOneLiner().isBlank();
        DecisionSummary s = snapshot.decisionSummary();
        boolean hasTension = s != null && s.keyValuationTension() != null && !s.keyValuationTension().isBlank();
        if (!hasContext && !hasTension) {
            return null;
        }

        Div row = new Div();
        row.setWidthFull();
        int cols = (hasContext && hasTension) ? 2 : 1;
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(" + cols + ", minmax(0, 1fr))")
                .set("gap", "16px")
                .set("margin-bottom", "20px");

        if (hasContext) {
            row.add(buildValuationContextCard(snapshot.valuationOneLiner(), analysis, markdownRenderer));
        }
        if (hasTension) {
            row.add(buildMethodologyCard(s));
        }
        return row;
    }

    private Div buildValuationContextCard(String oneLiner, CompanyAnalysisDto analysis, MarkdownRenderer markdownRenderer) {
        Div card = SnapshotUiSupport.createTintedCard("rgba(52, 152, 219, 0.05)", "rgba(52, 152, 219, 0.2)");
        H5 title = new H5("Valuation Context");
        title.getStyle().set("margin", "0 0 10px 0").set("color", SnapshotUiSupport.COLOR_BLUE).set("font-weight", "700");

        String jsonData = analysis != null ? analysis.getAnalysisData() : null;
        Component body = markdownRenderer != null
                ? markdownRenderer.renderMarkdownWithCitations(oneLiner, jsonData)
                : new Span(oneLiner);
        body.getStyle().set("font-size", "0.88rem").set("line-height", "1.5");

        card.add(title, body);
        return card;
    }

    private Div buildMethodologyCard(DecisionSummary s) {
        boolean aligned = "ALIGNED".equalsIgnoreCase(s.methodAgreement());
        String accent = aligned ? SnapshotUiSupport.COLOR_GREEN : SnapshotUiSupport.COLOR_AMBER;
        String bg = aligned ? "rgba(46, 204, 113, 0.05)" : "rgba(255, 171, 0, 0.05)";
        String border = aligned ? "rgba(46, 204, 113, 0.2)" : "rgba(255, 171, 0, 0.22)";

        Div card = SnapshotUiSupport.createTintedCard(bg, border);
        Span body = new Span(s.keyValuationTension());
        body.getStyle().set("display", "block").set("font-size", "0.88rem").set("line-height", "1.5").set("color", "var(--lumo-body-text-color)");
        card.add(buildMethodologyHeader(s, aligned, accent), body);
        return card;
    }

    private HorizontalLayout buildMethodologyHeader(DecisionSummary s, boolean aligned, String accent) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "10px");

        H5 title = new H5("Methodology Reconciliation");
        title.getStyle().set("margin", "0").set("color", accent).set("font-weight", "700");
        header.add(title);

        if (s.methodAgreement() != null) {
            header.add(SnapshotUiSupport.createStatusBadge(
                    SnapshotUiSupport.formatDimensionLabel(s.methodAgreement()), aligned ? "success" : "warning"));
        }
        return header;
    }

    private Div buildCleanDriverCard(DecisionDriver d) {
        DriverPalette palette = resolveDriverPalette(d.status());
        Div card = SnapshotUiSupport.createTintedCard(palette.bgTint(), palette.borderTint());
        card.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);

        H5 factorTitle = new H5(d.factor());
        factorTitle.getStyle().set("margin", "0").set("color", palette.accent()).set("font-weight", "700");
        top.add(factorTitle, SnapshotUiSupport.createStatusBadge(
                SnapshotUiSupport.formatDimensionLabel(d.status()),
                SnapshotUiSupport.resolveDimensionTheme(d.status())));

        card.add(top, buildDriverMetricsRow(d), buildDriverTriggerBlock(d.whatChangesView()));
        return card;
    }

    private Div buildDriverMetricsRow(DecisionDriver d) {
        Div metrics = new Div();
        metrics.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, minmax(0, 1fr))")
                .set("gap", "12px");
        metrics.add(
                buildMetricPair("Current", d.currentValue()),
                buildMetricPair("Benchmark / Hurdle", d.benchmarkOrRequired())
        );
        return metrics;
    }

    private Div buildMetricPair(String label, String value) {
        Div box = new Div();
        Span lbl = new Span(label);
        lbl.getStyle().set("display", "block").set("font-size", "0.75rem").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        Span val = new Span(value != null ? value : "-");
        val.getStyle().set("display", "block").set("font-size", "0.85rem").set("color", "var(--lumo-header-text-color)").set("margin-top", "2px");
        box.add(lbl, val);
        return box;
    }

    private Div buildDriverTriggerBlock(String triggerText) {
        Div trig = new Div();
        trig.getStyle().set("padding-top", "8px").set("border-top", "1px solid var(--lumo-contrast-10pct)");
        Span lbl = new Span("Monitoring Trigger:");
        lbl.getStyle().set("display", "block").set("font-size", "0.76rem").set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        Span val = new Span(triggerText != null ? triggerText : "-");
        val.getStyle().set("display", "block").set("font-size", "0.82rem").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "2px");
        trig.add(lbl, val);
        return trig;
    }

    private DriverPalette resolveDriverPalette(String status) {
        if (status == null) {
            return new DriverPalette(SnapshotUiSupport.COLOR_BLUE, "rgba(52, 152, 219, 0.05)", "rgba(52, 152, 219, 0.2)");
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SUPPORTIVE" -> new DriverPalette(SnapshotUiSupport.COLOR_GREEN, "rgba(46, 204, 113, 0.05)", "rgba(46, 204, 113, 0.2)");
            case "RISK" -> new DriverPalette(SnapshotUiSupport.COLOR_RED, "rgba(231, 76, 60, 0.05)", "rgba(231, 76, 60, 0.2)");
            default -> new DriverPalette(SnapshotUiSupport.COLOR_AMBER, "rgba(255, 171, 0, 0.05)", "rgba(255, 171, 0, 0.22)");
        };
    }

    private HorizontalLayout buildAuditTitleRow(int conviction) {
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.getStyle().set("gap", "8px");

        Icon shield = VaadinIcon.SHIELD.create();
        shield.setColor(SnapshotUiSupport.COLOR_BLUE);
        shield.setSize("18px");

        String suffix = conviction > 0 ? " (Final Conviction " + conviction + "/5)" : "";
        H5 title = new H5("Conviction Audit & Active Guardrails" + suffix);
        title.getStyle().set("margin", "0").set("color", SnapshotUiSupport.COLOR_BLUE).set("font-weight", "bold");

        titleRow.add(shield, title);
        return titleRow;
    }

    private HorizontalLayout buildAuditBulletRow(String reason) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle().set("gap", "8px");

        Span dot = new Span("•");
        dot.getStyle().set("color", SnapshotUiSupport.COLOR_BLUE).set("font-weight", "bold");

        Span text = new Span(reason);
        text.getStyle().set("font-size", "0.88rem").set("color", "var(--lumo-body-text-color)");

        row.add(dot, text);
        return row;
    }

    private void addIfNotNull(Div parent, Component child) {
        if (child != null) {
            parent.add(child);
        }
    }
}
