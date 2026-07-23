package com.oraculum.ui.views;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.TooltipBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.chart.builder.EventsBuilder;
import com.github.appreciated.apexcharts.config.plotoptions.builder.TreemapBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.company.api.*;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.ui.service.AnalysisRequestService;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.components.CompanyOverviewComponent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import tools.jackson.databind.ObjectMapper;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "market-map", layout = MainLayout.class)
@PageTitle("Market Map")
@PermitAll
public class MarketMapView extends VerticalLayout {

    private final CompanyScreenerApi companyScreenerApi;
    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final CompanyValuationApi companyValuationApi;
    private final AnalysisRequestService analysisRequestService;
    private final ObjectMapper objectMapper;

    public MarketMapView(CompanyScreenerApi companyScreenerApi,
                         CompanyMetadataApi companyMetadataApi,
                         CompanyFinancialDataApi companyFinancialDataApi,
                         CompanySharePriceApi companySharePriceApi,
                         CompanyNewsApi companyNewsApi,
                         CompanyInsiderTransactionApi companyInsiderTransactionApi,
                         CompanyValuationApi companyValuationApi,
                         AnalysisRequestService analysisRequestService,
                         ObjectMapper objectMapper) {
        this.companyScreenerApi = companyScreenerApi;
        this.companyMetadataApi = companyMetadataApi;
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.companyValuationApi = companyValuationApi;
        this.analysisRequestService = analysisRequestService;
        this.objectMapper = objectMapper;

        setPadding(true);
        setSpacing(false);
        setSizeFull();

        initLayout();
    }

    private void initLayout() {
        H3 title = new H3("Market Map");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem");
        
        List<CompanyOverviewDto> companies = companyScreenerApi.getCompanyOverview();
        LocalDate maxTradeDate = companies != null ? companies.stream()
                .map(CompanyOverviewDto::tradeDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null) : null;
                
        String asOfText = maxTradeDate != null ? " (Data as of " + maxTradeDate + ")" : "";
        Paragraph description = new Paragraph("Interactive treemap and sortable table view visualizing market capitalization, sector division, growth trends, moving averages, and volume velocity." + asOfText);
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        Tab treemapTab = new Tab("Treemap View");
        Tab tableTab = new Tab("Table View");
        Tabs tabs = new Tabs(treemapTab, tableTab);
        tabs.setWidthFull();

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);

        tabs.addSelectedChangeListener(event -> {
            contentArea.removeAll();
            if (event.getSelectedTab().equals(treemapTab)) {
                ApexCharts treemap = createTreemap(companies);
                contentArea.add(Objects.requireNonNullElseGet(treemap, () -> new Paragraph("Not enough data to display the market map.")));
            } else {
                Grid<CompanyOverviewDto> grid = createMarketGrid(companies != null ? companies : List.of());
                
                com.vaadin.flow.component.orderedlayout.HorizontalLayout toolbar = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
                toolbar.setWidthFull();
                toolbar.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
                toolbar.addClassNames(LumoUtility.Padding.Bottom.SMALL);

                Button runAnalysisBtn = new Button("Run Analysis", com.vaadin.flow.component.icon.VaadinIcon.PLAY.create());
                runAnalysisBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                toolbar.add(runAnalysisBtn);
                toolbar.getStyle().set("margin-top", "1rem");
                toolbar.getStyle().set("margin-bottom", "0.5rem");
                
                runAnalysisBtn.addClickListener(_ -> triggerAnalysis(grid.getSelectedItems(), grid));
                
                contentArea.add(toolbar, grid);
            }
        });

        ApexCharts treemap = createTreemap(companies);
        contentArea.add(Objects.requireNonNullElseGet(treemap, () -> new Paragraph("Not enough data to display the market map.")));

        add(title, description, tabs, ViewHelper.wrapInCard(contentArea));
        setFlexGrow(1, contentArea);

        attachClientClickListener();
    }

    private Grid<CompanyOverviewDto> createMarketGrid(List<CompanyOverviewDto> companies) {
        Grid<CompanyOverviewDto> grid = new Grid<>(CompanyOverviewDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof com.vaadin.flow.component.grid.GridMultiSelectionModel<CompanyOverviewDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }

        grid.addColumn(CompanyOverviewDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<CompanyOverviewDto> nameCol = grid.addColumn(CompanyOverviewDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(CompanyOverviewDto::description);

        grid.addColumn(CompanyOverviewDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(CompanyOverviewDto::industryName).setHeader("Industry").setAutoWidth(true).setKey("industry").setSortable(true);

        grid.addColumn(new NumberRenderer<>(c -> c.marketCapitalization() != null ? c.marketCapitalization() / 1_000_000_000f : null, "$%.2fB"))
                .setHeader("Market Cap").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::marketCapitalization));

        grid.addColumn(new NumberRenderer<>(CompanyOverviewDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("Price").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::sharePrice));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1d()))).setHeader("1D %").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1d));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1w()))).setHeader("1W %").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1w));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1m()))).setHeader("1M %").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1m));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange6m()))).setHeader("6M %").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange6m));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1y()))).setHeader("1Y %").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1y));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.pctFrom50dMa()))).setHeader("vs 50D MA").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::pctFrom50dMa));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.pctFrom200dMa()))).setHeader("vs 200D MA").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::pctFrom200dMa));

        grid.addColumn(item -> item.volumeVelocity() != null ? String.format(Locale.US, "%.2fx", item.volumeVelocity()) : "-")
                .setHeader("Vol Velocity").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::volumeVelocity));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::qualityScore));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::compositeSignal));

        grid.addComponentColumn(item -> createCompanyDetailsButton(item.companyId())).setHeader("Details").setAutoWidth(true);

        GridListDataView<CompanyOverviewDto> dataView = grid.setItems(companies);
        setupGridFilters(grid, dataView);

        return grid;
    }

    private void setupGridFilters(Grid<CompanyOverviewDto> grid, GridListDataView<CompanyOverviewDto> dataView) {
        HeaderRow filterRow = grid.appendHeaderRow();
        MarketMapFilter filter = new MarketMapFilter();
        dataView.setFilter(filter::test);

        ViewHelper.addFilter(grid, filterRow, "ticker", "Ticker", v -> {
            filter.ticker = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "companyName", "Company", v -> {
            filter.companyName = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "sector", "Sector", v -> {
            filter.sector = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "industry", "Industry", v -> {
            filter.industry = v;
            dataView.refreshAll();
        });
    }

    private Button createCompanyDetailsButton(int companyId) {
        return ViewHelper.createCompanyDetailsButton(
                companyMetadataApi,
                companyFinancialDataApi,
                companySharePriceApi,
                companyNewsApi,
                companyInsiderTransactionApi,
                companyValuationApi,
                objectMapper,
                companyId,
                true
        );
    }

    private void attachClientClickListener() {
        getElement().executeJs("""
            var el = $0;
            window.oraculumMarketMapClick = function(companyId) {
                if (companyId && el && el.$server) {
                    el.$server.onCompanySelected(companyId);
                }
            };
        """, getElement());
    }

    private void triggerAnalysis(Set<CompanyOverviewDto> selectedItems, Grid<CompanyOverviewDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (CompanyOverviewDto item : selectedItems) {
            CompanyAnalysisRequest requestDto = new CompanyAnalysisRequest(UUID.randomUUID(), item.companyId(), new TickerKeyDto(item.ticker(), item.market()), LocalDate.now(), null);
            analysisRequestService.requestAnalysis(requestDto);
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private boolean validateBatchSelection(int count) {
        if (count == 0) {
            ViewHelper.showError("Please select at least one company.");
            return false;
        }
        if (count > 10) {
            ViewHelper.showError("Maximum of 10 companies allowed per batch.");
            return false;
        }
        return true;
    }

    @ClientCallable
    public void onCompanySelected(int companyId) {
        CompanyDto company = companyMetadataApi.getCompanyById(companyId);
        if (company != null) {
            Dialog dialog = new Dialog();
            dialog.setWidth("90vw");
            dialog.setHeight("90vh");
            dialog.add(new CompanyOverviewComponent(
                    companyFinancialDataApi,
                    companySharePriceApi,
                    companyNewsApi,
                    companyInsiderTransactionApi,
                    companyValuationApi,
                    company,
                    objectMapper
            ));

            Button closeBtn = new Button("Close", _ -> dialog.close());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dialog.getFooter().add(closeBtn);

            dialog.addOpenedChangeListener(e -> {
                if (e.isOpened()) {
                    UI.getCurrent().getPage().executeJs("setTimeout(() => window.dispatchEvent(new Event('resize')), 250);");
                }
            });

            dialog.open();
        } else {
            ViewHelper.showError("Company details not found.");
        }
    }

    private ApexCharts createTreemap(List<CompanyOverviewDto> companies) {
        if (companies == null || companies.isEmpty()) {
            return null;
        }

        Map<String, List<CompanyOverviewDto>> groupedBySector = companies.stream()
                .filter(c -> c.sectorName() != null && c.marketCapitalization() != null && c.marketCapitalization() > 0)
                .collect(Collectors.groupingBy(CompanyOverviewDto::sectorName));

        List<Series<CustomData>> seriesList = new ArrayList<>();
        for (Map.Entry<String, List<CompanyOverviewDto>> entry : groupedBySector.entrySet()) {
            List<CompanyOverviewDto> topCompanies = entry.getValue().stream()
                    .sorted((a, b) -> Float.compare(b.marketCapitalization(), a.marketCapitalization()))
                    .limit(20)
                    .toList();

            List<CustomData> dataPoints = topCompanies.stream()
                    .map(this::toCustomData)
                    .toList();

            seriesList.add(new Series<>(entry.getKey(), dataPoints.toArray(new CustomData[0])));
        }

        return buildApexCharts(seriesList);
    }

    private CustomData toCustomData(CompanyOverviewDto c) {
        Float priceChange = c.priceChange1d() != null ? c.priceChange1d() : 0f;
        String color = getColorForPriceChange(priceChange);
        String name = c.companyName() != null ? c.companyName() : c.ticker();
        return new CustomData(
                c.ticker(),
                c.marketCapitalization(),
                color,
                name,
                c.companyId(),
                c.priceChange1d(),
                c.priceChange1w(),
                c.priceChange1m(),
                c.priceChange6m(),
                c.priceChange1y(),
                c.pctFrom50dMa(),
                c.pctFrom200dMa(),
                c.volumeVelocity(),
                c.tradeDate() != null ? c.tradeDate().toString() : "N/A"
        );
    }

    private ApexCharts buildApexCharts(List<Series<CustomData>> seriesList) {
        String tooltipCustomJs = """
            function({ series, seriesIndex, dataPointIndex, w }) {
                var item = w.config.series[seriesIndex].data[dataPointIndex];
                if (!item) return '';
            
                var name = item.companyName || item.x;
                var ticker = item.x;
                var cap = item.y ? '$' + (item.y / 1000000000).toFixed(2) + 'B' : 'N/A';
            
                var formatPct = function(val) {
                    if (val === null || val === undefined) return '<span style="color:#aaa;">N/A</span>';
                    var color = val >= 0 ? '#4caf50' : '#f44336';
                    var sign = val >= 0 ? '+' : '';
                    return '<span style="color:' + color + '; font-weight:600;">' + sign + val.toFixed(2) + '%</span>';
                };
            
                var formatVel = function(val) {
                    if (val === null || val === undefined) return '<span style="color:#aaa;">N/A</span>';
                    return '<span style="color:#60a5fa; font-weight:600;">' + val.toFixed(2) + 'x</span>';
                };
            
                return '<div style="padding: 12px 16px; background: rgba(18, 24, 38, 0.95); border: 1px solid rgba(255,255,255,0.12); border-radius: 8px; color: #fff; font-family: var(--lumo-font-family, sans-serif); box-shadow: 0 4px 16px rgba(0,0,0,0.4); min-width: 200px;">' +
                       '<div style="font-size: 14px; font-weight: 700; margin-bottom: 4px; color: #f0f4f8;">' + name + ' <span style="color:#8a99ad; font-size:12px; font-weight:400;">(' + ticker + ')</span></div>' +
                       '<div style="font-size: 12px; margin-bottom: 2px; color: #cbd5e1;">Market Cap: <strong style="color:#fff;">' + cap + '</strong></div>' +
                       '<div style="font-size: 12px; margin-bottom: 8px; color: #cbd5e1;">Last Trade: <strong style="color:#fff;">' + (item.tradeDate || 'N/A') + '</strong></div>' +
                       '<hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 6px 0;"/>' +
                       '<div style="font-size: 12px; display: grid; grid-template-columns: 90px 1fr; gap: 4px;">' +
                       '<span style="color:#94a3b8;">Prev Close:</span>' + formatPct(item.priceChange1d) +
                       '<span style="color:#94a3b8;">1-Week:</span>' + formatPct(item.priceChange1w) +
                       '<span style="color:#94a3b8;">1-Month:</span>' + formatPct(item.priceChange1m) +
                       '<span style="color:#94a3b8;">6-Month:</span>' + formatPct(item.priceChange6m) +
                       '<span style="color:#94a3b8;">1-Year:</span>' + formatPct(item.priceChange1y) +
                       '<span style="color:#94a3b8;">vs 50D MA:</span>' + formatPct(item.pctFrom50dMa) +
                       '<span style="color:#94a3b8;">vs 200D MA:</span>' + formatPct(item.pctFrom200dMa) +
                       '<span style="color:#94a3b8;">Vol Velocity:</span>' + formatVel(item.volumeVelocity) +
                       '</div></div>';
            }
            """;

        String clickEventJs = """
            function(event, chartContext, config) {
                if (config && config.w && config.seriesIndex !== undefined && config.dataPointIndex !== undefined) {
                    var item = config.w.config.series[config.seriesIndex].data[config.dataPointIndex];
                    if (item && item.companyId && window.oraculumMarketMapClick) {
                        window.oraculumMarketMapClick(item.companyId);
                    }
                }
            }
            """;

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get()
                        .withType(Type.TREEMAP)
                        .withHeight("800px")
                        .withEvents(EventsBuilder.get()
                                .withDataPointSelection(clickEventJs)
                                .build())
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
                        .withCustom(tooltipCustomJs)
                        .build())
                .build();
    }

    private String getColorForPriceChange(Float priceChange) {
        if (priceChange >= 3.0f) return "#16a34a";
        if (priceChange >= 1.0f) return "#15803d";
        if (priceChange > 0.0f) return "#166534";
        if (priceChange <= -3.0f) return "#dc2626";
        if (priceChange <= -1.0f) return "#b91c1c";
        if (priceChange < 0.0f) return "#991b1b";
        return "#334155";
    }

    private static class MarketMapFilter {
        String ticker = "", companyName = "", sector = "", industry = "";

        boolean test(CompanyOverviewDto item) {
            return ViewHelper.matches(item.ticker(), ticker)
                    && ViewHelper.matches(item.companyName(), companyName)
                    && ViewHelper.matches(item.sectorName(), sector)
                    && ViewHelper.matches(item.industryName(), industry);
        }
    }

    public static class CustomData {
        public String x;
        public Float y;
        public String fillColor;
        public String companyName;
        public int companyId;
        public Float priceChange1d;
        public Float priceChange1w;
        public Float priceChange1m;
        public Float priceChange6m;
        public Float priceChange1y;
        public Float pctFrom50dMa;
        public Float pctFrom200dMa;
        public Float volumeVelocity;
        public String tradeDate;

        public CustomData(String x, Float y, String fillColor, String companyName, int companyId,
                          Float priceChange1d, Float priceChange1w, Float priceChange1m,
                          Float priceChange6m, Float priceChange1y,
                          Float pctFrom50dMa, Float pctFrom200dMa, Float volumeVelocity, String tradeDate) {
            this.x = x;
            this.y = y;
            this.fillColor = fillColor;
            this.companyName = companyName;
            this.companyId = companyId;
            this.priceChange1d = priceChange1d;
            this.priceChange1w = priceChange1w;
            this.priceChange1m = priceChange1m;
            this.priceChange6m = priceChange6m;
            this.priceChange1y = priceChange1y;
            this.pctFrom50dMa = pctFrom50dMa;
            this.pctFrom200dMa = pctFrom200dMa;
            this.volumeVelocity = volumeVelocity;
            this.tradeDate = tradeDate;
        }
    }
}



