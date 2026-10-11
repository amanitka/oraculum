package com.oraculum.ui.components;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.*;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.plotoptions.bar.Ranges;
import com.github.appreciated.apexcharts.config.plotoptions.bar.builder.ColorsBuilder;
import com.github.appreciated.apexcharts.config.plotoptions.builder.BarBuilder;
import com.github.appreciated.apexcharts.config.tooltip.builder.YBuilder;
import com.github.appreciated.apexcharts.config.xaxis.builder.LabelsBuilder;
import com.github.appreciated.apexcharts.config.yaxis.labels.builder.StyleBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.dto.SectorInstitutionalFlowTrendDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class InstitutionalSectorRotationComponent extends VerticalLayout {

    private final CompanyInstitutionalFlowApi institutionalFlowApi;
    private final Div chartContainer = new Div();
    private final HorizontalLayout kpiContainer = new HorizontalLayout();
    private final Grid<SectorInstitutionalFlowTrendDto> table = new Grid<>(SectorInstitutionalFlowTrendDto.class, false);

    private LocalDate currentPeriod;

    public InstitutionalSectorRotationComponent(CompanyInstitutionalFlowApi institutionalFlowApi) {
        this.institutionalFlowApi = institutionalFlowApi;

        setWidthFull();
        setPadding(false);
        setSpacing(true);

        initLayout();
    }

    private void initLayout() {
        chartContainer.setWidthFull();
        chartContainer.getStyle().set("min-height", "420px");

        kpiContainer.setWidthFull();
        kpiContainer.getStyle()
                .set("gap", "1rem")
                .set("flex-wrap", "wrap")
                .set("margin-top", "0.25rem");

        initTable();

        add(createTitleHeader(), kpiContainer, chartContainer, table);
    }

    private Component createTitleHeader() {
        H4 title = new H4("Institutional Sector Rotation (% Net Flow vs Sector Portfolio)");
        title.getStyle().set("margin-top", "0.25rem").set("margin-bottom", "0.25rem");
        return title;
    }

    private void initTable() {
        table.setWidthFull();
        table.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        table.setAllRowsVisible(true);

        table.addColumn(SectorInstitutionalFlowTrendDto::sectorName)
                .setHeader("Sector").setAutoWidth(true).setSortable(true);
        table.addColumn(new ComponentRenderer<>(s -> ViewHelper.deltaSpan(s.flowPct())))
                .setHeader("Flow (%)").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true);
        table.addColumn(new ComponentRenderer<>(s -> ViewHelper.deltaCurrencySpan(s.netValueChangeUsd())))
                .setHeader("Net Flow ($)").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true);
        table.addColumn(s -> s.totalInstValueUsd() != null ? ViewHelper.formatCompact(s.totalInstValueUsd().floatValue()) : "-")
                .setHeader("Portfolio ($)").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true);
        table.addColumn(new ComponentRenderer<>(s -> ViewHelper.deltaNumberSpan(s.netSharesChange())))
                .setHeader("Net Shares Δ").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true);
        table.addColumn(s -> String.valueOf(s.companiesCount()))
                .setHeader("Companies").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true);
    }

    public void updatePeriod(LocalDate period) {
        this.currentPeriod = period;
        refresh();
    }

    public void refresh() {
        List<SectorInstitutionalFlowTrendDto> rawSectors =
                institutionalFlowApi.getSectorInstitutionalFlowByPeriod(currentPeriod);

        chartContainer.removeAll();
        kpiContainer.removeAll();

        if (rawSectors == null || rawSectors.isEmpty()) {
            chartContainer.add(createEmptyState());
            table.setItems(List.of());
            return;
        }

        List<SectorInstitutionalFlowTrendDto> sectors = rawSectors.stream()
                .sorted(Comparator.comparing(SectorInstitutionalFlowTrendDto::flowPct).reversed())
                .toList();

        renderKpiCards(sectors);
        renderBarChart(sectors);
        table.setItems(sectors);
    }

    private Component createEmptyState() {
        VerticalLayout empty = new VerticalLayout();
        empty.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        empty.setPadding(true);
        Span msg = new Span("No sector institutional flow data available for this quarter.");
        msg.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-style", "italic");
        empty.add(msg);
        return empty;
    }

    private void renderKpiCards(List<SectorInstitutionalFlowTrendDto> sectors) {
        SectorInstitutionalFlowTrendDto topInflow = sectors.stream()
                .filter(s -> s.flowPct() > 0)
                .findFirst()
                .orElse(null);

        SectorInstitutionalFlowTrendDto topOutflow = sectors.stream()
                .filter(s -> s.flowPct() < 0)
                .min(Comparator.comparing(SectorInstitutionalFlowTrendDto::flowPct))
                .orElse(null);

        long totalNetFlow = sectors.stream()
                .mapToLong(s -> s.netValueChangeUsd() != null ? s.netValueChangeUsd() : 0L)
                .sum();

        long buyingCount = sectors.stream().filter(s -> s.flowPct() > 0).count();
        long sellingCount = sectors.stream().filter(s -> s.flowPct() < 0).count();

        if (topInflow != null) {
            String val = String.format(java.util.Locale.US, "+%.1f%% (%s)",
                    topInflow.flowPct(), ViewHelper.formatCompact(topInflow.netValueChangeUsd().floatValue()));
            kpiContainer.add(createKpiCard("Leading Accumulation", topInflow.sectorName(), val, true));
        }
        if (topOutflow != null) {
            String val = String.format(java.util.Locale.US, "%.1f%% (-%s)",
                    topOutflow.flowPct(), ViewHelper.formatCompact(Math.abs(topOutflow.netValueChangeUsd().floatValue())));
            kpiContainer.add(createKpiCard("Leading Liquidation", topOutflow.sectorName(), val, false));
        }
        String totalSign = totalNetFlow > 0 ? "+" : "";
        kpiContainer.add(createKpiCard("Net Market Flow", totalSign + ViewHelper.formatCompact((float) totalNetFlow), null, null));
        kpiContainer.add(createKpiCard("Sector Breadth", buyingCount + " Buying / " + sellingCount + " Selling", null, null));
    }

    private Component createKpiCard(String label, String mainText, String deltaText, Boolean isPositive) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("metric-card");
        card.setWidth(null);
        card.getStyle().set("flex", "1 1 180px").set("max-width", "300px");
        card.setPadding(true);
        card.setSpacing(false);
        card.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span lblSpan = new Span(label);
        lblSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase");

        HorizontalLayout valLayout = new HorizontalLayout();
        valLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        valLayout.setSpacing(true);

        Span mainSpan = new Span(mainText);
        mainSpan.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-m)");
        valLayout.add(mainSpan);

        if (deltaText != null && isPositive != null) {
            Icon icon = isPositive ? VaadinIcon.ARROW_UP.create() : VaadinIcon.ARROW_DOWN.create();
            icon.setSize("12px");
            icon.setColor(isPositive ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
            Span deltaSpan = new Span(deltaText);
            deltaSpan.getStyle().set("color", isPositive ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)")
                    .set("font-weight", "500").set("font-size", "var(--lumo-font-size-s)");
            valLayout.add(icon, deltaSpan);
        }

        card.add(lblSpan, valLayout);
        return card;
    }

    private void renderBarChart(List<SectorInstitutionalFlowTrendDto> sectors) {
        String[] sectorNames = sectors.stream().map(SectorInstitutionalFlowTrendDto::sectorName).toArray(String[]::new);
        Double[] flowPcts = sectors.stream()
                .map(s -> (double) Math.round(s.flowPct() * 100.0) / 100.0)
                .toArray(Double[]::new);

        Ranges posRange = new Ranges();
        posRange.setFrom(0.0);
        posRange.setTo(10000.0);
        posRange.setColor("#16a34a");

        Ranges negRange = new Ranges();
        negRange.setFrom(-10000.0);
        negRange.setTo(-0.0001);
        negRange.setColor("#dc2626");

        ApexCharts chart = ApexChartsBuilder.get()
                .withChart(ChartBuilder.get()
                        .withId("sector-rotation-bar-chart")
                        .withType(Type.BAR)
                        .withHeight("420px")
                        .withForeColor("#cbd5e1")
                        .withBackground("transparent")
                        .build())
                .withPlotOptions(PlotOptionsBuilder.get()
                        .withBar(BarBuilder.get()
                                .withHorizontal(true)
                                .withBarHeight("65%")
                                .withColors(ColorsBuilder.get().withRanges(posRange, negRange).build())
                                .build())
                        .build())
                .withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
                .withSeries(new Series<>("Relative Flow (% of Portfolio)", flowPcts))
                .withXaxis(XAxisBuilder.get()
                        .withCategories(sectorNames)
                        .withLabels(LabelsBuilder.get()
                                .withStyle(com.github.appreciated.apexcharts.config.xaxis.labels.builder.StyleBuilder.get()
                                        .withColors(List.of("#e2e8f0"))
                                        .withFontSize("12px")
                                        .build())
                                .withFormatter("function(val) { return (val > 0 ? '+' : '') + val.toFixed(1) + '%'; }")
                                .build())
                        .build())
                .withYaxis(YAxisBuilder.get()
                        .withLabels(com.github.appreciated.apexcharts.config.yaxis.builder.LabelsBuilder.get()
                                .withStyle(StyleBuilder.get()
                                        .withColors(List.of("#f8fafc"))
                                        .withFontSize("13px")
                                        .build())
                                .build())
                        .build())
                .withTooltip(TooltipBuilder.get()
                        .withTheme("dark")
                        .withY(YBuilder.get()
                                .withFormatter("function(val) { return (val > 0 ? '+' : '') + val.toFixed(2) + '%'; }")
                                .build())
                        .build())
                .build();

        chart.setWidth("100%");
        chart.setHeight("420px");
        chartContainer.add(chart);
    }
}
