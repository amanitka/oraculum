package com.oraculum.ui.components;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.*;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.chart.builder.ZoomBuilder;
import com.github.appreciated.apexcharts.config.stroke.Curve;
import com.github.appreciated.apexcharts.config.xaxis.XAxisType;
import com.github.appreciated.apexcharts.config.yaxis.builder.LabelsBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompanyOverviewComponent extends VerticalLayout {

    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final CompanyDto company;
    private final ObjectMapper objectMapper;
    private final Div chartPlaceholder;
    private ComboBox<String> timeframeComboBox;

    public CompanyOverviewComponent(CompanyFinancialDataApi companyFinancialDataApi,
                                    CompanySharePriceApi companySharePriceApi,
                                    CompanyNewsApi companyNewsApi,
                                    CompanyInsiderTransactionApi companyInsiderTransactionApi,
                                    CompanyDto company, ObjectMapper objectMapper) {
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.company = company;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header
        add(createHeader());

        // Main TabSheet
        TabSheet mainTabSheet = new TabSheet();
        mainTabSheet.setSizeFull();
        mainTabSheet.addThemeName("bordered");
        mainTabSheet.getStyle().set("margin-top", "1rem");

        // 1. Overview Tab
        this.chartPlaceholder = new Div();
        chartPlaceholder.setWidthFull();
        chartPlaceholder.setHeight("400px");
        mainTabSheet.add("Overview", createOverviewLayout());

        // 2. Financial Ratios Tab
        mainTabSheet.add("Ratios", createRatiosLayout());

        // 3. Income Statement Tab
        mainTabSheet.add("Income Statement", createIncomeStatementLayout());

        // 4. Balance Sheet Tab
        mainTabSheet.add("Balance Sheet", createBalanceSheetLayout());

        // 5. Cash Flow Tab
        mainTabSheet.add("Cash Flow", createCashFlowLayout());

        // 6. News Tab
        mainTabSheet.add("News", createNewsLayout());

        // 7. Insider Tab
        mainTabSheet.add("Insider", createInsiderLayout());

        add(mainTabSheet);

        // Load chart data once component is attached to DOM to fix initial width rendering bug
        addAttachListener(_ -> loadChartData());
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.BASELINE);

        H3 name = new H3(company.companyName() + " (" + company.ticker() + ")");
        name.getStyle().set("margin", "0");

        Span industry = new Span(company.industryName() != null ? company.industryName() : company.industryId());
        industry.getElement().getThemeList().addAll(List.of("badge", "contrast"));

        Span sector = new Span(company.sectorName());
        sector.getElement().getThemeList().addAll(List.of("badge", "contrast"));

        header.add(name, industry, sector);
        return header;
    }

    private Component createOverviewLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(true);

        HorizontalLayout chartHeader = new HorizontalLayout();
        chartHeader.setWidthFull();
        chartHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        chartHeader.setAlignItems(FlexComponent.Alignment.BASELINE);

        H3 title = new H3("Price History");
        title.getStyle().set("margin-top", "0");

        timeframeComboBox = new ComboBox<>("Timeframe");
        timeframeComboBox.setItems("1M", "3M", "6M", "1Y", "5Y", "All");
        timeframeComboBox.setValue("1Y");
        timeframeComboBox.addValueChangeListener(_ -> loadChartData());

        chartHeader.add(title, timeframeComboBox);
        layout.add(chartHeader);

        chartPlaceholder.setWidthFull();
        chartPlaceholder.setHeight("400px");
        layout.add(chartPlaceholder);
        return layout;
    }

    private Component createRatiosLayout() {
        // Fetch data
        List<CompanyFinancialRatiosDto> data = companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(company.id(), LocalDate.now().minusYears(20));
        return createVariantTabSheet(data, this::createRatiosGrid);
    }

    private Component createIncomeStatementLayout() {
        List<IncomeStatementDto> data = companyFinancialDataApi.getIncomeStatementsByCompanyId(company.id(), LocalDate.now().minusYears(20));
        return createVariantTabSheet(data, this::createIncomeStatementGrid);
    }

    private Component createBalanceSheetLayout() {
        List<BalanceSheetDto> data = companyFinancialDataApi.getBalanceSheetsByCompanyId(company.id(), LocalDate.now().minusYears(20));
        return createVariantTabSheet(data, this::createBalanceSheetGrid);
    }

    private Component createCashFlowLayout() {
        List<CashFlowStatementDto> data = companyFinancialDataApi.getCashFlowStatementsByCompanyId(company.id(), LocalDate.now().minusYears(20));
        return createVariantTabSheet(data, this::createCashFlowGrid);
    }

    private Component createNewsLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(true);

        List<NewsTickerDto> news = companyNewsApi.getNewsByTicker(company.ticker(), LocalDate.now().minusDays(30));

        Grid<NewsTickerDto> grid = new Grid<>(NewsTickerDto.class, false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addColumn(item -> {
            if (item.timePublished() != null) {
                return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(item.timePublished());
            }
            return "-";
        }).setHeader("Date").setSortable(true).setAutoWidth(true).setComparator(Comparator.comparing(NewsTickerDto::timePublished, Comparator.nullsLast(Comparator.naturalOrder())));

        grid.addColumn(NewsTickerDto::title).setHeader("Title").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(NewsTickerDto::source).setHeader("Publisher").setAutoWidth(true);
        grid.addColumn(item -> item.relevanceScore() != null ? String.format(java.util.Locale.US, "%.0f%%", item.relevanceScore() * 100) : "-")
                .setHeader("Relevance").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.tickerSentimentLabel(), item.tickerSentimentScore())))
                .setHeader("Sentiment").setAutoWidth(true);

        // Sort newest first
        grid.setItems(news.stream().sorted(Comparator.comparing(NewsTickerDto::timePublished).reversed()).collect(Collectors.toList()));

        grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            VerticalLayout vl = new VerticalLayout();
            vl.setPadding(true);
            Span summary = new Span(item.summary());
            summary.getStyle().set("font-size", "0.9rem");
            vl.add(summary);
            return vl;
        }));
        grid.setDetailsVisibleOnClick(true);

        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        layout.add(grid);
        return layout;
    }

    private Component createInsiderLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(true);

        companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(company.ticker()).ifPresent(summary -> {
            HorizontalLayout summaryLayout = new HorizontalLayout();
            summaryLayout.setWidthFull();
            summaryLayout.setSpacing(true);
            summaryLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            Span clusterBuy = new Span("Cluster Buy: " + (Boolean.TRUE.equals(summary.hasClusterBuy()) ? "YES" : "NO"));
            clusterBuy.getElement().getThemeList().addAll(List.of("badge", Boolean.TRUE.equals(summary.hasClusterBuy()) ? "success" : "contrast"));

            Span buys3m = new Span(String.format("3M Buys: $%,.0f", summary.buysValue3m() != null ? summary.buysValue3m() : 0.0));
            Span sells3m = new Span(String.format("3M Sells: $%,.0f", summary.sellsValue3m() != null ? summary.sellsValue3m() : 0.0));

            summaryLayout.add(clusterBuy, buys3m, sells3m);
            layout.add(summaryLayout);
        });

        List<InsiderTransactionTickerDto> transactions = companyInsiderTransactionApi.getInsiderTransactionsByTicker(company.ticker(), LocalDate.now().minusYears(10));

        Grid<InsiderTransactionTickerDto> grid = new Grid<>(InsiderTransactionTickerDto.class, false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(InsiderTransactionTickerDto::tradeDate).setHeader("Date").setAutoWidth(true).setSortable(true);
        grid.addColumn(InsiderTransactionTickerDto::insiderName).setHeader("Insider Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(InsiderTransactionTickerDto::title).setHeader("Title").setAutoWidth(true).setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> {
                    Span typeSpan = new Span(item.tradeType());
                    String theme = item.tradeType() != null && item.tradeType().contains("Purchase") ? "success" : "error";
                    typeSpan.getElement().getThemeList().addAll(List.of("badge", theme));
                    return typeSpan;
                })).setHeader("Type").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::tradeType, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.addColumn(item -> item.price() != null ? String.format(java.util.Locale.US, "$%.2f", item.price()) : "-").setHeader("Price").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::price, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.addColumn(item -> item.qty() != null ? String.format(java.util.Locale.US, "%,d", item.qty().longValue()) : "-").setHeader("Qty").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::qty, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.addColumn(item -> item.owned() != null ? String.format(java.util.Locale.US, "%,d", item.owned().longValue()) : "-").setHeader("Owned").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::owned, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.addColumn(item -> item.deltaOwn() != null ? String.format(java.util.Locale.US, "%s%.1f%%", item.deltaOwn().signum() > 0 ? "+" : "", item.deltaOwn()) : "-").setHeader("Delta").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::deltaOwn, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.addColumn(item -> item.value() != null ? String.format(java.util.Locale.US, "$%,.0f", item.value()) : "-").setHeader("Value").setAutoWidth(true)
                .setComparator(Comparator.comparing(InsiderTransactionTickerDto::value, Comparator.nullsLast(Comparator.naturalOrder()))).setSortable(true);

        grid.setItems(transactions.stream().sorted(Comparator.comparing(InsiderTransactionTickerDto::tradeDate, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()).collect(Collectors.toList()));
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        layout.add(grid);
        return layout;
    }

    private String formatDynamicValue(String key, Object val) {
        if (val == null) return "-";

        if (val instanceof String str) {
            if (str.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
                try {
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(str.substring(0, 19));
                    if (dt.getHour() == 0 && dt.getMinute() == 0 && dt.getSecond() == 0) {
                        return dt.toLocalDate().toString();
                    } else {
                        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(dt);
                    }
                } catch (Exception _) {
                }
            }
        }

        if (val instanceof Number num) {
            double d = num.doubleValue();

            if (key != null && key.toLowerCase().contains("year")) {
                return String.valueOf(num.intValue());
            }
            if (key != null && key.toLowerCase().endsWith("id")) {
                return String.valueOf(num.longValue());
            }

            if (Math.abs(d) < 1000 && d != Math.floor(d)) {
                return String.format(java.util.Locale.US, "%,.2f", d);
            } else {
                return String.format(java.util.Locale.US, "%,.0f", d);
            }
        }
        return String.valueOf(val);
    }

    private <T> Grid<Map<String, Object>> createDynamicJsonGrid(List<T> items,
                                                                Function<T, String> jsonExtractor) {
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        List<Map<String, Object>> mapItems = new ArrayList<>();

        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();

        for (T item : items) {
            String json = jsonExtractor.apply(item);
            if (json != null && !json.isBlank()) {
                try {
                    Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
                    });
                    mapItems.add(map);
                    allKeys.addAll(map.keySet());
                } catch (Exception e) {
                    // Ignore bad JSON
                }
            }
        }

        String[] priorityKeys = {"Report Date", "Fiscal Year", "Fiscal Period", "Currency", "Ticker"};
        for (String pk : priorityKeys) {
            if (allKeys.contains(pk)) {
                grid.addColumn(m -> formatDynamicValue(pk, m.get(pk))).setHeader(pk).setSortable(true).setAutoWidth(true).setResizable(true);
                allKeys.remove(pk);
            }
        }

        for (String key : allKeys) {
            grid.addColumn(m -> formatDynamicValue(key, m.get(key))).setHeader(key).setSortable(true).setAutoWidth(true).setResizable(true);
        }

        grid.setItems(mapItems);
        return grid;
    }

    // --- Grid Creators ---

    private Grid<CompanyFinancialRatiosDto> createRatiosGrid(List<CompanyFinancialRatiosDto> items) {
        Grid<CompanyFinancialRatiosDto> grid = new Grid<>(CompanyFinancialRatiosDto.class, false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addColumn(CompanyFinancialRatiosDto::reportDate).setHeader("Report Date").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::financialTrendScore).setHeader("Trend Score").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::qualityScore).setHeader("Quality").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::returnOnEquity).setHeader("ROE").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::grossMargin).setHeader("Gross Margin").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::netMargin).setHeader("Net Margin").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::revenueYoyGrowth).setHeader("Rev Growth (YoY)").setSortable(true).setAutoWidth(true);
        grid.addColumn(CompanyFinancialRatiosDto::quickRatio).setHeader("Quick Ratio").setSortable(true).setAutoWidth(true);

        grid.setItems(items.stream().sorted(Comparator.comparing(CompanyFinancialRatiosDto::reportDate).reversed()).collect(Collectors.toList()));
        return grid;
    }

    private Grid<Map<String, Object>> createIncomeStatementGrid(List<IncomeStatementDto> items) {
        return createDynamicJsonGrid(items, IncomeStatementDto::statementData);
    }

    private Grid<Map<String, Object>> createBalanceSheetGrid(List<BalanceSheetDto> items) {
        return createDynamicJsonGrid(items, BalanceSheetDto::statementData);
    }

    private Grid<Map<String, Object>> createCashFlowGrid(List<CashFlowStatementDto> items) {
        return createDynamicJsonGrid(items, CashFlowStatementDto::statementData);
    }

    // --- Helper Methods ---
    private <T> Component createVariantTabSheet(List<T> allData, Function<List<T>, Component> gridCreator) {
        if (allData == null || allData.isEmpty()) {
            return new Span("No data available.");
        }
        TabSheet variantTabs = new TabSheet();
        variantTabs.setSizeFull();

        List<T> annual = new ArrayList<>();
        List<T> quarterly = new ArrayList<>();
        List<T> ttm = new ArrayList<>();

        for (T item : allData) {
            StatementVariant variant = null;
            if (item instanceof CompanyFinancialRatiosDto dto) variant = dto.variant();
            else if (item instanceof IncomeStatementDto dto) variant = dto.variant();
            else if (item instanceof BalanceSheetDto dto) variant = dto.variant();
            else if (item instanceof CashFlowStatementDto dto) variant = dto.variant();

            if (StatementVariant.ANNUAL.equals(variant)) annual.add(item);
            else if (StatementVariant.QUARTERLY.equals(variant)) quarterly.add(item);
            else if (StatementVariant.TTM.equals(variant)) ttm.add(item);
        }

        variantTabs.add("Annual", createGridContainer(annual, gridCreator));
        variantTabs.add("Quarterly", createGridContainer(quarterly, gridCreator));
        variantTabs.add("TTM", createGridContainer(ttm, gridCreator));

        return variantTabs;
    }

    private <T> Component createGridContainer(List<T> items, java.util.function.Function<List<T>, Component> gridCreator) {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull(); // Allow height to grow based on content
        layout.setPadding(true);
        if (items.isEmpty()) {
            layout.add(new Span("No records found for this timeframe."));
        } else {
            Component grid = gridCreator.apply(items);
            if (grid instanceof Grid) {
                ((Grid<?>) grid).setWidthFull();
                ((Grid<?>) grid).setAllRowsVisible(true); // Force grid to show all rows so it doesn't collapse in a scrolling layout
            }
            layout.add(grid);
        }
        return layout;
    }

    private void loadChartData() {
        if (timeframeComboBox == null) return;

        String timeframe = timeframeComboBox.getValue();
        LocalDate after = LocalDate.now();
        after = switch (timeframe) {
            case "1M" -> after.minusMonths(1);
            case "3M" -> after.minusMonths(3);
            case "6M" -> after.minusMonths(6);
            case "1Y" -> after.minusYears(1);
            case "5Y" -> after.minusYears(5);
            case null, default -> after.minusYears(100);
        };

        List<SharePriceDto> sharePrices = companySharePriceApi.getSharePricesByCompanyId(company.id(), after);

        if (sharePrices.isEmpty()) {
            chartPlaceholder.removeAll();
            chartPlaceholder.add(new Span("No price history available for the selected timeframe."));
            return;
        }

        List<SharePriceDto> sortedPrices = new ArrayList<>(sharePrices);
        sortedPrices.sort(Comparator.comparing(SharePriceDto::tradeDate));

        Object[] prices = sortedPrices.stream().map(SharePriceDto::close).toArray();
        String[] dates = sortedPrices.stream().map(sp -> sp.tradeDate().toString()).toArray(String[]::new);

        ApexCharts lineChart = ApexChartsBuilder.get()
                .withChart(ChartBuilder.get()
                        .withType(Type.LINE)
                        .withWidth("100%")
                        .withHeight("400px")
                        .withZoom(ZoomBuilder.get().withEnabled(true).build())
                        .withForeColor("var(--lumo-body-text-color)")
                        .withBackground("transparent")
                        .build())
                .withStroke(StrokeBuilder.get()
                        .withCurve(Curve.STRAIGHT)
                        .withWidth(2.0)
                        .build())
                .withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
                .withSeries(new Series<>("Close Price", prices))
                .withXaxis(XAxisBuilder.get()
                        .withType(XAxisType.DATETIME)
                        .withCategories(dates)
                        .build())
                .withYaxis(YAxisBuilder.get()
                        .withLabels(LabelsBuilder.get()
                                .withFormatter("function (value) { return value.toFixed(2); }")
                                .build())
                        .build())
                .withColors("#1676f3")
                .build();

        lineChart.setWidth("100%");
        lineChart.setHeight("400px");
        lineChart.getStyle().set("min-width", "100%");
        chartPlaceholder.getStyle().set("min-width", "100%");

        chartPlaceholder.removeAll();
        chartPlaceholder.add(lineChart);

        // Force the browser to trigger a resize event after the DOM layout settles.
        // Dialog animations can take ~300ms, so we stagger a few resize events to guarantee the chart snaps to 100%.
        chartPlaceholder.getUI().ifPresent(ui ->
                ui.getPage().executeJs("setTimeout(() => window.dispatchEvent(new Event('resize')), 100); " +
                        "setTimeout(() => window.dispatchEvent(new Event('resize')), 350); " +
                        "setTimeout(() => window.dispatchEvent(new Event('resize')), 600);")
        );
    }
}
