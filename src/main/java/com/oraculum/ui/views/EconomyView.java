package com.oraculum.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.*;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.chart.builder.AnimationsBuilder;
import com.github.appreciated.apexcharts.config.chart.builder.ToolbarBuilder;
import com.github.appreciated.apexcharts.config.chart.builder.ZoomBuilder;
import com.github.appreciated.apexcharts.config.chart.toolbar.AutoSelected;
import com.github.appreciated.apexcharts.config.stroke.Curve;
import com.github.appreciated.apexcharts.config.xaxis.XAxisType;
import com.github.appreciated.apexcharts.config.yaxis.builder.LabelsBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Route(value = "economy", layout = com.oraculum.ui.MainLayout.class)
@PageTitle("Economy | Oraculum")
@PermitAll
public class EconomyView extends VerticalLayout {

    private final EconomyDataApi economyDataApi;
    private final Div chartContainer;
    private Div selectedCard;

    public EconomyView(EconomyDataApi economyDataApi) {
        this.economyDataApi = economyDataApi;

        setWidthFull();
        setPadding(false);
        setSpacing(true);
        getStyle().set("padding-top", "2rem");
        getStyle().set("padding-bottom", "2rem");

        H3 title = new H3("Macroeconomic Indicators");
        title.addClassNames(LumoUtility.Margin.Top.LARGE, LumoUtility.Margin.Bottom.MEDIUM);
        title.getStyle().set("margin-bottom", "1rem");
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
        // Remove fixed height so contents don't overflow the border padding
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
            selectedCard = (Div) dashboardGrid.getChildren().findFirst().orElse(null);
            if (selectedCard != null) {
                selectedCard.addClassName("selected-card");
            }
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

        H4 name = new H4(summary.indicatorTitle() != null ? summary.indicatorTitle() : summary.indicator().name());
        name.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

        Div latestLayout = new Div();
        latestLayout.getStyle().set("display", "flex");
        latestLayout.getStyle().set("justify-content", "space-between");
        latestLayout.getStyle().set("align-items", "baseline");
        latestLayout.getStyle().set("margin-top", "0.5rem");

        Span val = new Span(summary.latestValue() != null ? String.format(java.util.Locale.US, "%.2f", summary.latestValue()) + " " + summary.indicator().getUnit() : "N/A");
        val.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        Span yoy = new Span(summary.yoyChangePct() != null ? String.format(java.util.Locale.US, "%+.2f%%", summary.yoyChangePct()) : "");
        yoy.getStyle().set("font-weight", "bold");
        yoy.getStyle().set("color", "var(--lumo-body-text-color)"); // Default to white in dark mode

        if (summary.yoyChangePct() != null && summary.yoyChangePct() != 0) {
            MacroIndicator.SentimentDirection dir = summary.indicator().getSentimentDirection();
            if (dir == MacroIndicator.SentimentDirection.POSITIVE_IS_GOOD) {
                if (summary.yoyChangePct() > 0) {
                    yoy.getStyle().set("color", "var(--lumo-success-text-color)");
                } else if (summary.yoyChangePct() < 0) {
                    yoy.getStyle().set("color", "var(--lumo-error-text-color)");
                }
            } else if (dir == MacroIndicator.SentimentDirection.NEGATIVE_IS_GOOD) {
                if (summary.yoyChangePct() < 0) {
                    yoy.getStyle().set("color", "var(--lumo-success-text-color)");
                } else if (summary.yoyChangePct() > 0) {
                    yoy.getStyle().set("color", "var(--lumo-error-text-color)");
                }
            }
        }

        latestLayout.add(val, yoy);
        card.add(name, latestLayout);

        card.addClickListener(_ -> {
            if (selectedCard != null) {
                selectedCard.removeClassName("selected-card");
            }
            selectedCard = card;
            selectedCard.addClassName("selected-card");
            loadChart(summary);
        });

        return card;
    }

    private void loadChart(MacroSummaryDto summary) {
        chartContainer.removeAll();
        chartContainer.getStyle().set("display", "block");

        List<MacroObservationDto> history = economyDataApi.getHistoricalData(summary.indicator());

        if (history == null || history.isEmpty()) {
            Span placeholder = new Span("No historical data available.");
            placeholder.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE);
            chartContainer.add(placeholder);
            chartContainer.getStyle().set("display", "flex");
            return;
        }

        // ApexCharts requires data to be strictly sorted and unique by date for line charts
        List<MacroObservationDto> uniqueSortedHistory = new java.util.ArrayList<>(
                history.stream()
                        .filter(obs -> obs.value() != null) // Filter out missing values (holidays in daily series) which break the line into dots and ruin performance
                        .collect(java.util.stream.Collectors.toMap(
                                MacroObservationDto::observationDate,
                                obs -> obs,
                                (_, replacement) -> replacement, // keep the latest if duplicates exist
                                java.util.TreeMap::new // guarantees sorted by date
                        ))
                        .values()
        );

        com.vaadin.flow.component.orderedlayout.HorizontalLayout chartHeader = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        chartHeader.setWidthFull();
        chartHeader.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        chartHeader.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);

        H3 title = new H3((summary.indicatorTitle() != null ? summary.indicatorTitle() : summary.indicator().name()) + " (" + summary.indicator().getUnit() + ")");
        title.getStyle().set("margin-top", "0");

        com.vaadin.flow.component.combobox.ComboBox<String> timeframeComboBox = new com.vaadin.flow.component.combobox.ComboBox<>("Timeframe");
        timeframeComboBox.setItems("1Y", "5Y", "10Y", "MAX");
        timeframeComboBox.setValue("10Y");

        chartHeader.add(title, timeframeComboBox);
        chartContainer.add(chartHeader);

        Div actualChartDiv = new Div();
        actualChartDiv.setWidthFull();
        chartContainer.add(actualChartDiv);

        long actualMinDate = uniqueSortedHistory.getFirst().observationDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        Runnable renderChart = () -> {
            actualChartDiv.removeAll();

            String tf = timeframeComboBox.getValue();
            long initialMinX = switch (tf) {
                case "1Y" ->
                        Math.max(actualMinDate, uniqueSortedHistory.getLast().observationDate().minusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
                case "5Y" ->
                        Math.max(actualMinDate, uniqueSortedHistory.getLast().observationDate().minusYears(5).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
                case "MAX" -> actualMinDate;
                case null, default ->
                        Math.max(actualMinDate, uniqueSortedHistory.getLast().observationDate().minusYears(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
            };

            // Filter data instead of relying on XAxis min/max which causes SVG clipping bugs
            java.util.List<MacroObservationDto> filteredHistory = uniqueSortedHistory.stream()
                    .filter(obs -> obs.observationDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() >= initialMinX)
                    .toList();

            Object[] filteredValues = filteredHistory.stream().map(MacroObservationDto::value).toArray();
            String[] filteredDates = filteredHistory.stream().map(obs -> obs.observationDate().toString()).toArray(String[]::new);

            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (Object v : filteredValues) {
                if (v instanceof Number num) {
                    double val = num.doubleValue();
                    if (val < minY) minY = val;
                    if (val > maxY) maxY = val;
                }
            }
            if (minY == Double.MAX_VALUE) {
                minY = 0.0;
                maxY = 100.0;
            }
            double range = maxY - minY;
            if (range == 0) range = 1.0;
            double paddingY = range * 0.1;

            ApexCharts chart = ApexChartsBuilder.get()
                    .withChart(ChartBuilder.get()
                            .withType(Type.LINE)
                            .withWidth("100%")
                            .withHeight("400px")
                            .withForeColor("var(--lumo-body-text-color)")
                            .withBackground("transparent")
                            .withAnimations(AnimationsBuilder.get()
                                    .withEnabled(false) // Disable animations for massive performance boost on large datasets
                                    .build())
                            .withZoom(ZoomBuilder.get()
                                    .withEnabled(true)
                                    .withAutoScaleYaxis(true)
                                    .build())
                            .withToolbar(ToolbarBuilder.get()
                                    .withAutoSelected(AutoSelected.ZOOM)
                                    .build())
                            .build())
                    .withStroke(StrokeBuilder.get()
                            .withCurve(Curve.STRAIGHT)
                            .withWidth(2.0)
                            .build())
                    .withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
                    .withSeries(new Series<>((summary.indicatorTitle() != null ? summary.indicatorTitle() : summary.indicator().name()) + " (" + summary.indicator().getUnit() + ")", filteredValues))
                    .withXaxis(XAxisBuilder.get()
                            .withType(XAxisType.DATETIME)
                            .withCategories(filteredDates)
                            .withTickAmount(java.math.BigDecimal.valueOf(10.0)) // Force ticks to display so we don't lose the bottom axis labels
                            .build())
                    .withYaxis(YAxisBuilder.get()
                            .withMin(minY - paddingY)
                            .withMax(maxY + paddingY)
                            .withLabels(LabelsBuilder.get()
                                    .withFormatter("function (value) { return value.toFixed(2); }")
                                    .build())
                            .build())
                    .withColors("var(--lumo-primary-color)")
                    .build();
            actualChartDiv.add(chart);
        };

        timeframeComboBox.addValueChangeListener(_ -> renderChart.run());
        renderChart.run();
    }
}
