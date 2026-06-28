package com.oraculum.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Route(value = "economy", layout = com.oraculum.ui.MainLayout.class)
@PageTitle("Economy | Oraculum")
public class EconomyView extends VerticalLayout {

    private static final String CHART_CSS = """
            <style>
            .apexcharts-tooltip {
                background: var(--lumo-base-color) !important;
                border: 1px solid var(--lumo-contrast-10pct) !important;
                box-shadow: var(--lumo-box-shadow-s) !important;
                color: var(--lumo-body-text-color) !important;
            }
            .apexcharts-tooltip-title {
                background: var(--lumo-contrast-5pct) !important;
                border-bottom: 1px solid var(--lumo-contrast-10pct) !important;
                font-family: var(--lumo-font-family) !important;
                font-weight: 600 !important;
            }
            .apexcharts-tooltip-text {
                color: var(--lumo-body-text-color) !important;
            }
            </style>
            """;

    private final EconomyDataApi economyDataApi;
    private final Div chartContainer;

    public EconomyView(EconomyDataApi economyDataApi) {
        this.economyDataApi = economyDataApi;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        add(new com.vaadin.flow.component.Html(CHART_CSS));

        H2 title = new H2("Macroeconomic Indicators");
        title.addClassNames(LumoUtility.Margin.Top.LARGE, LumoUtility.Margin.Bottom.MEDIUM);
        title.getStyle().set("margin-bottom", "1rem");
        title.getStyle().set("margin-top", "2rem");
        add(title);

        Div dashboardGrid = new Div();
        dashboardGrid.setWidthFull();
        dashboardGrid.getStyle().set("display", "grid");
        dashboardGrid.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))");
        dashboardGrid.getStyle().set("gap", "1rem");

        List<MacroSummaryDto> summaries = economyDataApi.getMacroeconomicSummary();
        // Sort summaries by indicator name to be deterministic
        summaries.sort(Comparator.comparing(s -> s.indicatorTitle() != null ? s.indicatorTitle() : s.indicator().name()));

        for (MacroSummaryDto summary : summaries) {
            dashboardGrid.add(createSummaryCard(summary));
        }
        add(dashboardGrid);

        chartContainer = new Div();
        chartContainer.setWidthFull();
        chartContainer.setHeight("450px");
        chartContainer.getStyle().set("margin-top", "2rem");
        chartContainer.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        chartContainer.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        chartContainer.getStyle().set("padding", "var(--lumo-space-l)");
        chartContainer.getStyle().set("box-sizing", "border-box");
        chartContainer.getStyle().set("background-color", "var(--lumo-base-color)");

        Span placeholder = new Span("Select an indicator above to view its historical trend.");
        placeholder.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE);
        chartContainer.add(placeholder);
        chartContainer.getStyle().set("display", "flex");
        chartContainer.getStyle().set("align-items", "center");
        chartContainer.getStyle().set("justify-content", "center");

        add(chartContainer);

        if (!summaries.isEmpty()) {
            loadChart(summaries.getFirst());
        }
    }

    private Div createSummaryCard(MacroSummaryDto summary) {
        Div card = new Div();
        card.getStyle().set("padding", "var(--lumo-space-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("transition", "box-shadow 0.2s, transform 0.2s");
        card.getElement().addEventListener("mouseover", _ -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
            card.getStyle().set("transform", "translateY(-2px)");
        });
        card.getElement().addEventListener("mouseout", _ -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
            card.getStyle().set("transform", "none");
        });

        H3 name = new H3(summary.indicatorTitle() != null ? summary.indicatorTitle() : summary.indicator().name());
        name.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

        Div latestLayout = new Div();
        latestLayout.getStyle().set("display", "flex");
        latestLayout.getStyle().set("justify-content", "space-between");
        latestLayout.getStyle().set("align-items", "baseline");
        latestLayout.getStyle().set("margin-top", "0.5rem");

        Span val = new Span(summary.latestValue() != null ? String.format(java.util.Locale.US, "%.2f", summary.latestValue()) + " " + summary.indicator().getUnit() : "N/A");
        val.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        Span yoy = new Span(summary.yoyChangePct() != null ? String.format(java.util.Locale.US, "%+.2f%%", summary.yoyChangePct()) : "");
        if (summary.yoyChangePct() != null) {
            if (summary.yoyChangePct() > 0) {
                yoy.addClassNames(LumoUtility.TextColor.SUCCESS);
            } else if (summary.yoyChangePct() < 0) {
                yoy.addClassNames(LumoUtility.TextColor.ERROR);
            } else {
                yoy.addClassNames(LumoUtility.TextColor.SECONDARY);
            }
        }

        latestLayout.add(val, yoy);
        card.add(name, latestLayout);

        card.addClickListener(_ -> loadChart(summary));

        return card;
    }

    private void loadChart(MacroSummaryDto summary) {
        chartContainer.removeAll();
        chartContainer.getStyle().set("display", "block");

        List<MacroObservationDto> history = economyDataApi.getHistoricalData(summary.indicator());

        if (history.isEmpty()) {
            Span placeholder = new Span("No historical data available.");
            placeholder.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE);
            chartContainer.add(placeholder);
            chartContainer.getStyle().set("display", "flex");
            return;
        }

        // ApexCharts requires data to be strictly sorted and unique by date for line charts
        List<MacroObservationDto> uniqueSortedHistory = new java.util.ArrayList<>(
                history.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                MacroObservationDto::observationDate,
                                obs -> obs,
                                (_, replacement) -> replacement,
                                java.util.TreeMap::new
                        ))
                        .values()
        );

        Object[] values = uniqueSortedHistory.stream().map(MacroObservationDto::value).toArray();
        String[] dates = uniqueSortedHistory.stream().map(obs -> obs.observationDate().toString()).toArray(String[]::new);

        // Zoom to last 10 years
        long maxDate = uniqueSortedHistory.getLast().observationDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long minDate10YearsAgo = uniqueSortedHistory.getLast().observationDate().minusYears(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long actualMinDate = uniqueSortedHistory.getFirst().observationDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long initialMinX = Math.max(actualMinDate, minDate10YearsAgo);

        ApexCharts chart = ApexChartsBuilder.get()
                .withChart(com.github.appreciated.apexcharts.config.builder.ChartBuilder.get()
                        .withType(com.github.appreciated.apexcharts.config.chart.Type.LINE)
                        .withWidth("100%")
                        .withHeight("400px")
                        .withZoom(com.github.appreciated.apexcharts.config.chart.builder.ZoomBuilder.get()
                                .withEnabled(true)
                                .withAutoScaleYaxis(true)
                                .build())
                        .withForeColor("var(--lumo-body-text-color)")
                        .withBackground("transparent")
                        .build())
                .withStroke(com.github.appreciated.apexcharts.config.builder.StrokeBuilder.get()
                        .withCurve(com.github.appreciated.apexcharts.config.stroke.Curve.STRAIGHT)
                        .withWidth(2.0)
                        .build())
                .withDataLabels(com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder.get().withEnabled(false).build())
                .withSeries(new com.github.appreciated.apexcharts.helper.Series<>((summary.indicatorTitle() != null ? summary.indicatorTitle() : summary.indicator().name()) + " (" + summary.indicator().getUnit() + ")", values))
                .withXaxis(com.github.appreciated.apexcharts.config.builder.XAxisBuilder.get()
                        .withType(com.github.appreciated.apexcharts.config.xaxis.XAxisType.DATETIME)
                        .withCategories(dates)
                        .withMin((double) initialMinX)
                        .withMax((double) maxDate)
                        .build())
                .withYaxis(com.github.appreciated.apexcharts.config.builder.YAxisBuilder.get()
                        .withLabels(com.github.appreciated.apexcharts.config.yaxis.builder.LabelsBuilder.get()
                                .withFormatter("function (value) { return value.toFixed(2); }")
                                .build())
                        .build())
                .withColors("var(--lumo-primary-color)")
                .build();

        chart.setWidth("100%");
        chart.setHeight("400px");
        chartContainer.add(chart);
    }
}
