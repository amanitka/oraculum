package com.oraculum.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.MarketDto;
import com.oraculum.company.api.dto.SharePriceDto;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route(value = "company", layout = MainLayout.class)
@PageTitle("Company Details")
@NpmPackage(value = "@webcomponents/shadycss", version = "1.11.2")
public class CompanyView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final Div chartPlaceholder;
    private ComboBox<CompanyDto> companyComboBox;
    private ComboBox<String> timeframeComboBox;
    private Div chartCard;

    public CompanyView(CompanyApi companyApi) {
        this.companyApi = companyApi;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Header
        HorizontalLayout header = createHeader();
        add(header);

        // Main Dashboard Area
        VerticalLayout mainDashboard = createMainDashboard();
        add(mainDashboard);
        setFlexGrow(1, mainDashboard);

        this.chartPlaceholder = createChartPlaceholder();
        chartCard.add(chartPlaceholder);
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE, LumoUtility.BoxShadow.XSMALL);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        companyComboBox = new ComboBox<>("Search Company or Ticker...");
        companyComboBox.setWidth("600px");

        List<CompanyDto> allCompanies = new ArrayList<>();
        for (MarketDto market : companyApi.getAllMarkets()) {
            allCompanies.addAll(companyApi.getCompaniesByMarket(market.marketId()));
        }
        companyComboBox.setItems(allCompanies);
        companyComboBox.setItemLabelGenerator(c -> String.format("%s - %s", c.ticker(), c.companyName()));

        companyComboBox.addValueChangeListener(_ -> loadChartData());

        header.add(companyComboBox);
        return header;
    }

    private VerticalLayout createMainDashboard() {
        VerticalLayout mainDashboard = new VerticalLayout();
        mainDashboard.setSizeFull();
        mainDashboard.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.CONTRAST_5);

        chartCard = new Div();
        chartCard.setWidthFull();
        chartCard.setMaxWidth("1200px");
        chartCard.setMinHeight("400px");
        chartCard.addClassNames(LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN);
        chartCard.getStyle().set("overflow", "hidden");

        HorizontalLayout chartHeader = new HorizontalLayout();
        chartHeader.setWidthFull();
        chartHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        chartHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        H3 title = new H3("Price History");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.NONE);

        timeframeComboBox = new ComboBox<>("Timeframe");
        timeframeComboBox.setItems("1M", "3M", "6M", "1Y", "5Y", "All");
        timeframeComboBox.setValue("3M");
        timeframeComboBox.addValueChangeListener(_ -> loadChartData());

        chartHeader.add(title, timeframeComboBox);
        chartCard.add(chartHeader);

        mainDashboard.add(chartCard);
        mainDashboard.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        return mainDashboard;
    }

    private Div createChartPlaceholder() {
        Div placeholder = new Div();
        placeholder.setWidthFull();
        placeholder.addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM);
        placeholder.getStyle().set("flex-grow", "1");
        placeholder.getStyle().set("margin-top", "var(--lumo-space-m)");

        Span text = new Span("Chart component will render here");
        text.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);
        placeholder.add(text);

        return placeholder;
    }

    private void loadChartData() {
        CompanyDto selectedCompany = companyComboBox.getValue();
        if (selectedCompany == null) {
            chartPlaceholder.removeAll();
            Span text = new Span("Select a company to view price history");
            text.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);
            chartPlaceholder.add(text);
            return;
        }

        String timeframe = timeframeComboBox.getValue();
        LocalDate after = switch (timeframe) {
            case "1M" -> LocalDate.now().minusMonths(1);
            case "3M" -> LocalDate.now().minusMonths(3);
            case "6M" -> LocalDate.now().minusMonths(6);
            case "1Y" -> LocalDate.now().minusYears(1);
            case "5Y" -> LocalDate.now().minusYears(5);
            case "All" -> LocalDate.of(1900, 1, 1);
            default -> LocalDate.now().minusMonths(3);
        };

        // Fetch data using the API
        List<SharePriceDto> sharePrices = companyApi.getSharePricesByCompanyId(selectedCompany.id(), after);

        if (sharePrices.isEmpty()) {
            chartPlaceholder.removeAll();
            Span text = new Span("No price history available for the selected timeframe.");
            text.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);
            chartPlaceholder.add(text);
            return;
        }

        // Sort by date ascending to ensure the chart plots left to right correctly
        List<SharePriceDto> sortedPrices = new ArrayList<>(sharePrices);
        sortedPrices.sort(java.util.Comparator.comparing(SharePriceDto::tradeDate));

        Object[] prices = sortedPrices.stream().map(SharePriceDto::close).toArray();
        String[] dates = sortedPrices.stream().map(sp -> sp.tradeDate().toString()).toArray(String[]::new);

        ApexCharts lineChart = ApexChartsBuilder.get()
                .withChart(com.github.appreciated.apexcharts.config.builder.ChartBuilder.get()
                        .withType(com.github.appreciated.apexcharts.config.chart.Type.LINE)
                        .withBackground("transparent")
                        .withZoom(com.github.appreciated.apexcharts.config.chart.builder.ZoomBuilder.get().withEnabled(true).build())
                        .build())
                .withStroke(com.github.appreciated.apexcharts.config.builder.StrokeBuilder.get()
                        .withCurve(com.github.appreciated.apexcharts.config.stroke.Curve.STRAIGHT)
                        .withWidth(2.0)
                        .build())
                .withDataLabels(com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder.get().withEnabled(false).build())
                .withSeries(new com.github.appreciated.apexcharts.helper.Series<>("Close Price", prices))
                .withXaxis(com.github.appreciated.apexcharts.config.builder.XAxisBuilder.get()
                        .withType(com.github.appreciated.apexcharts.config.xaxis.XAxisType.DATETIME)
                        .withCategories(dates)
                        .build())
                .withYaxis(com.github.appreciated.apexcharts.config.builder.YAxisBuilder.get()
                        .withLabels(com.github.appreciated.apexcharts.config.yaxis.builder.LabelsBuilder.get()
                                .withFormatter("function (value) { return value.toFixed(2); }")
                                .build())
                        .build())
                .withTheme(com.github.appreciated.apexcharts.config.builder.ThemeBuilder.get()
                        .withMode(com.github.appreciated.apexcharts.config.theme.Mode.DARK)
                        .build())
                .withColors("#1676f3")
                .build();

        lineChart.setWidth("100%");
        lineChart.setHeight("400px");

        chartPlaceholder.removeAll();
        chartPlaceholder.add(lineChart);
    }
}
