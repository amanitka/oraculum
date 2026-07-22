package com.oraculum.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.TooltipBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.plotoptions.builder.TreemapBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.company.api.CompanyScreenerApi;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "market-map", layout = MainLayout.class)
@PageTitle("Market Map")
@PermitAll
public class MarketMapView extends VerticalLayout {

    private final CompanyScreenerApi companyScreenerApi;

    public MarketMapView(CompanyScreenerApi companyScreenerApi) {
        this.companyScreenerApi = companyScreenerApi;
        setPadding(true);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3("Market Map");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem");
        Paragraph description = new Paragraph("Finviz-style treemap visualizing market capitalization and 1-day price changes.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        add(title, description);

        VerticalLayout chartContainer = new VerticalLayout();
        chartContainer.setSizeFull();
        chartContainer.setPadding(false);

        ApexCharts treemap = createTreemap();
        chartContainer.add(Objects.requireNonNullElseGet(treemap, () -> new Paragraph("Not enough data to display the market map.")));

        add(ViewHelper.wrapInCard(chartContainer));
        setFlexGrow(1, chartContainer);
    }

    private ApexCharts createTreemap() {
        List<CompanyOverviewDto> companies = companyScreenerApi.getCompanyOverview();
        if (companies == null || companies.isEmpty()) {
            return null;
        }

        // Group by Sector
        Map<String, List<CompanyOverviewDto>> groupedBySector = companies.stream()
                .filter(c -> c.sectorName() != null && c.marketCapitalization() != null && c.marketCapitalization() > 0)
                .collect(Collectors.groupingBy(CompanyOverviewDto::sectorName));

        List<Series<CustomData>> seriesList = new ArrayList<>();

        for (Map.Entry<String, List<CompanyOverviewDto>> entry : groupedBySector.entrySet()) {
            String sector = entry.getKey();

            // Limit to top 20 companies per sector by market cap to avoid clutter
            List<CompanyOverviewDto> topCompanies = entry.getValue().stream()
                    .sorted((a, b) -> Float.compare(b.marketCapitalization(), a.marketCapitalization()))
                    .limit(20)
                    .toList();

            List<CustomData> dataPoints = new ArrayList<>();
            for (CompanyOverviewDto company : topCompanies) {
                Float priceChange = company.priceChange1d() != null ? company.priceChange1d() : 0f;
                String color = getColorForPriceChange(priceChange);

                CustomData data = new CustomData(company.ticker(), company.marketCapitalization(), color);
                dataPoints.add(data);
            }
            seriesList.add(new Series<>(sector, dataPoints.toArray(new CustomData[0])));
        }

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get()
                        .withType(Type.TREEMAP)
                        .withHeight("800px")
                        .build())
                .withSeries(seriesList.toArray(new Series[0]))
                .withPlotOptions(PlotOptionsBuilder.get()
                        .withTreemap(TreemapBuilder.get()
                                .withEnableShades(true)
                                .withShadeIntensity(0.5)
                                .withDistributed(false)
                                .build())
                        .build())
                .withDataLabels(DataLabelsBuilder.get()
                        .withEnabled(true)
                        .build())
                .withTooltip(TooltipBuilder.get()
                        .withY(com.github.appreciated.apexcharts.config.tooltip.builder.YBuilder.get()
                                .withFormatter("function(value, { series, seriesIndex, dataPointIndex, w }) { return 'Market Cap: $' + (value / 1000000000).toFixed(2) + 'B'; }")
                                .build())
                        .build())
                .build();
    }

    private String getColorForPriceChange(Float priceChange) {
        if (priceChange >= 3.0f) return "#00FF00"; // Bright green
        if (priceChange >= 1.0f) return "#00CC00"; // Green
        if (priceChange > 0.0f) return "#009900"; // Dark green
        if (priceChange <= -3.0f) return "#FF0000"; // Bright red
        if (priceChange <= -1.0f) return "#CC0000"; // Red
        if (priceChange < 0.0f) return "#990000"; // Dark red
        return "#666666"; // Gray
    }

    // A custom Data class to hold fillColor for ApexCharts
    public static class CustomData {
        public String x;
        public Float y;
        public String fillColor;

        public CustomData(String x, Float y, String fillColor) {
            this.x = x;
            this.y = y;
            this.fillColor = fillColor;
        }
    }
}
