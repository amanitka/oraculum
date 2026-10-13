package com.oraculum.ui.components;

import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.ValuationZone;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReverseDcfSensitivityMatrixComponent extends VerticalLayout {

    public ReverseDcfSensitivityMatrixComponent(ReverseDcfDto dcfDto) {
        if (dcfDto == null || dcfDto.sensitivityMatrix() == null) {
            return;
        }

        setPadding(false);
        setSpacing(false);
        setWidthFull();

        Div card = new Div();
        card.addClassName("metric-card");
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("padding", "1rem 1.15rem")
                .set("width", "100%");

        card.add(createHeader(dcfDto));
        card.add(createMatrixGrid(dcfDto));
        card.add(createLegend());

        add(card);
    }

    private Component createHeader(ReverseDcfDto dcfDto) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.BASELINE);
        headerLayout.getStyle().set("margin-bottom", "0.5rem");

        H4 title = new H4("Reverse DCF Sensitivity Matrix");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "0.95rem")
                .set("font-weight", "600")
                .set("white-space", "nowrap");

        Span subtitle = new Span("Implied 10Y FCF CAGR (r vs. g)");
        subtitle.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap");

        headerLayout.add(title, subtitle);
        return headerLayout;
    }

    private Component createMatrixGrid(ReverseDcfDto dcfDto) {
        ReverseDcfSensitivityMatrixDto matrix = dcfDto.sensitivityMatrix();
        List<Double> discountRates = matrix.discountRatesPct();
        List<Double> terminalRates = matrix.terminalRatesPct();

        Map<String, ReverseDcfMatrixCellDto> cellMap = matrix.cells().stream()
                .collect(Collectors.toMap(
                        c -> formatRateKey(c.discountRatePct(), c.terminalGrowthRatePct()),
                        c -> c,
                        (c1, _) -> c1
                ));

        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "96px repeat(" + terminalRates.size() + ", minmax(84px, 1fr))")
                .set("gap", "5px")
                .set("align-items", "stretch");

        addGridHeaderRows(grid, terminalRates);
        addGridDataRows(grid, discountRates, terminalRates, cellMap);

        return grid;
    }

    private void addGridHeaderRows(Div grid, List<Double> terminalRates) {
        // Corner cell showing axes direction
        Div corner = new Div(new Span("r \\ g"));
        corner.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "6px 8px");
        grid.add(corner);

        // Column headers for each terminal rate (g)
        for (Double g : terminalRates) {
            boolean isBase = Math.abs(g - 3.0) < 0.001;
            String text = String.format(Locale.US, "%.1f%%%s", g, isBase ? " (Base)" : "");
            Div colHeader = new Div(new Span(text));
            colHeader.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("color", isBase ? "var(--lumo-primary-text-color)" : "var(--lumo-secondary-text-color)")
                    .set("font-weight", isBase ? "bold" : "500")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("padding", "6px 4px");
            grid.add(colHeader);
        }
    }

    private void addGridDataRows(Div grid, List<Double> discountRates, List<Double> terminalRates, Map<String, ReverseDcfMatrixCellDto> cellMap) {
        for (Double r : discountRates) {
            boolean isBaseDiscount = Math.abs(r - 10.0) < 0.001;
            String rText = String.format(Locale.US, "%.1f%%%s", r, isBaseDiscount ? " (Base)" : "");

            Div rowHeader = new Div(new Span(rText));
            rowHeader.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "flex-start")
                    .set("font-weight", isBaseDiscount ? "bold" : "500")
                    .set("color", isBaseDiscount ? "var(--lumo-primary-text-color)" : "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("padding", "6px 8px");
            grid.add(rowHeader);

            for (Double g : terminalRates) {
                ReverseDcfMatrixCellDto cellDto = cellMap.get(formatRateKey(r, g));
                grid.add(renderMatrixCell(cellDto, r, g));
            }
        }
    }

    private Component renderMatrixCell(ReverseDcfMatrixCellDto cellDto, double r, double g) {
        Div cell = new Div();
        if (cellDto == null) {
            cell.setText("-");
            return cell;
        }

        String valText = String.format(Locale.US, "%+.1f%%", cellDto.impliedFcfGrowthPct());
        Span span = new Span(valText);
        cell.add(span);

        applyZoneStyle(cell, span, cellDto.valuationZone(), cellDto.isBaseCase());

        String tooltip = String.format(Locale.US,
                "Discount Rate: %.1f%% | Terminal Growth: %.1f%%\nImplied 10Y FCF CAGR: %+.1f%%\nZone: %s%s",
                r, g, cellDto.impliedFcfGrowthPct(),
                cellDto.valuationZone().displayName(),
                cellDto.isBaseCase() ? " (Baseline Case)" : "");
        cell.getElement().setAttribute("title", tooltip);

        return cell;
    }

    private void applyZoneStyle(Div cell, Span span, ValuationZone zone, boolean isBaseCase) {
        cell.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "4px 2px")
                .set("border-radius", "3px")
                .set("font-size", "0.82rem")
                .set("cursor", "default")
                .set("user-select", "none");

        span.getStyle().set("font-weight", isBaseCase ? "bold" : "600");

        if (zone == ValuationZone.DEEP_DISCOUNT) {
            cell.getStyle().set("background-color", "rgba(34, 197, 94, 0.12)");
            span.getStyle().set("color", "#4ade80");
        } else if (zone == ValuationZone.FAIR) {
            cell.getStyle().set("background-color", "rgba(59, 130, 246, 0.12)");
            span.getStyle().set("color", "#60a5fa");
        } else if (zone == ValuationZone.AGGRESSIVE) {
            cell.getStyle().set("background-color", "rgba(249, 115, 22, 0.12)");
            span.getStyle().set("color", "#fb923c");
        } else {
            cell.getStyle().set("background-color", "rgba(239, 68, 68, 0.14)");
            span.getStyle().set("color", "#f87171");
        }

        cell.getStyle().set("border", "1px solid rgba(255, 255, 255, 0.04)");
    }

    private Component createLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setWidthFull();
        legend.setSpacing(false);
        legend.getStyle()
                .set("margin-top", "0.5rem")
                .set("padding-top", "0.4rem")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("gap", "0.35rem")
                .set("flex-wrap", "nowrap")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        legend.add(createLegendItem("≤ 6% Deep Disc.", "rgba(34, 197, 94, 0.15)", "#4ade80"));
        legend.add(createLegendItem("6-12% Fair", "rgba(59, 130, 246, 0.15)", "#60a5fa"));
        legend.add(createLegendItem("12-20% Aggr.", "rgba(249, 115, 22, 0.15)", "#fb923c"));
        legend.add(createLegendItem("> 20% Spec.", "rgba(239, 68, 68, 0.15)", "#f87171"));

        return legend;
    }

    private Component createLegendItem(String label, String bgColor, String textColor) {
        Span item = new Span(label);
        item.getStyle()
                .set("background-color", bgColor)
                .set("color", textColor)
                .set("padding", "1px 5px")
                .set("border-radius", "3px")
                .set("font-size", "0.7rem")
                .set("white-space", "nowrap")
                .set("font-weight", "500");
        return item;
    }

    private static String formatRateKey(double r, double g) {
        return String.format(Locale.US, "%.3f_%.3f", r, g);
    }
}
