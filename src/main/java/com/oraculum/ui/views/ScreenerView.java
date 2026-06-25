package com.oraculum.ui.views;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyScreenerApi;
import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.api.dto.*;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.service.AnalysisRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import tools.jackson.databind.ObjectMapper;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "screener", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Screener")
public class ScreenerView extends VerticalLayout {

    private final CompanyScreenerApi companyScreenerApi;
    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final AnalysisRequestService analysisRequestService;
    private final ObjectMapper objectMapper;
    private final VerticalLayout gridContainer;

    public ScreenerView(CompanyScreenerApi companyScreenerApi,
                        CompanyMetadataApi companyMetadataApi,
                        CompanyFinancialDataApi companyFinancialDataApi,
                        CompanySharePriceApi companySharePriceApi,
                        CompanyNewsApi companyNewsApi,
                        CompanyInsiderTransactionApi companyInsiderTransactionApi,
                        AnalysisRequestService analysisRequestService,
                        ObjectMapper objectMapper) {
        this.companyScreenerApi = companyScreenerApi;
        this.companyMetadataApi = companyMetadataApi;
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.analysisRequestService = analysisRequestService;
        this.objectMapper = objectMapper;
        setPadding(true);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3("Investment Screeners");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem"); // Pushes the text down significantly from the top menu
        Paragraph description = new Paragraph("Select a screening strategy below to view fully-materialized market data.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        Tab tabMaster = new Tab("Master Ranks");
        Tab tabNews = new Tab("News Sentiment");
        Tab tabQuality = new Tab("Quality Compounders");
        Tab tabValue = new Tab("Undervalued");
        Tab tabGraham = new Tab("Graham Deep Value");
        Tab tabFinancialTrend = new Tab("Trend Score 7+");
        Tab tabInsider = new Tab("Insider Activity");

        Tabs tabs = new Tabs(tabMaster, tabNews, tabQuality, tabValue, tabGraham, tabFinancialTrend, tabInsider);
        tabs.setWidthFull();

        gridContainer = new VerticalLayout();
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);

        tabs.addSelectedChangeListener(event -> setContent(event.getSelectedTab().getLabel()));

        add(title, description, tabs, gridContainer);

        // Load initial tab
        setContent(tabMaster.getLabel());
    }

    private void setContent(String tabLabel) {
        gridContainer.removeAll();

        HorizontalLayout toolbar = createToolbar();
        Button runAnalysisBtn = (Button) toolbar.getComponentAt(0);

        switch (tabLabel) {
            case "Master Ranks" -> {
                Grid<ScreenerMasterDto> grid = createMasterGrid();
                GridListDataView<ScreenerMasterDto> dataView = grid.setItems(companyScreenerApi.getMasterScreener());
                setupFilters(grid, dataView, ScreenerMasterDto.class);
                runAnalysisBtn.addClickListener(_ -> triggerAnalysisMaster(grid.getSelectedItems(), grid));
                gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
            }
            case "News Sentiment" -> {
                Grid<ScreenerNewsSentimentDto> grid = createNewsSentimentGrid();
                List<ScreenerNewsSentimentDto> data = companyScreenerApi.getNewsSentimentScreener().stream()
                        .filter(item -> item.newsCount30d() != null && item.newsCount30d() > 0)
                        .collect(Collectors.toList());
                GridListDataView<ScreenerNewsSentimentDto> dataView = grid.setItems(data);
                setupFilters(grid, dataView, ScreenerNewsSentimentDto.class);
                runAnalysisBtn.addClickListener(_ -> triggerAnalysisNewsSentiment(grid.getSelectedItems(), grid));
                gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
            }
            case "Insider Activity" -> {
                Grid<ScreenerInsiderDto> grid = createInsiderGrid();
                GridListDataView<ScreenerInsiderDto> dataView = grid.setItems(companyScreenerApi.getInsiderScreener());
                setupFilters(grid, dataView, ScreenerInsiderDto.class);
                runAnalysisBtn.addClickListener(_ -> triggerAnalysisInsider(grid.getSelectedItems(), grid));
                gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
            }
            case null, default -> {
                Grid<ScreenerDto> grid = createStandardGrid();
                List<ScreenerDto> data = switch (tabLabel) {
                    case "Quality Compounders" -> companyScreenerApi.getQualityCompoundersScreener();
                    case "Undervalued" -> companyScreenerApi.getUndervaluedScreener();
                    case "Graham Deep Value" -> companyScreenerApi.getGrahamDeepValueScreener();
                    case "Trend Score 7+" -> companyScreenerApi.getFinancialTrendScreener();
                    case null -> null;
                    default -> List.of();
                };
                GridListDataView<ScreenerDto> dataView = grid.setItems(data);
                setupFilters(grid, dataView, ScreenerDto.class);
                runAnalysisBtn.addClickListener(_ -> triggerAnalysisStandard(grid.getSelectedItems(), grid));
                gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
            }
        }
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        toolbar.addClassNames(LumoUtility.Padding.Bottom.SMALL);

        Button runAnalysisBtn = new Button("Run Analysis", VaadinIcon.PLAY.create());
        runAnalysisBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        toolbar.add(runAnalysisBtn);
        toolbar.getStyle().set("margin-top", "1rem");
        toolbar.getStyle().set("margin-bottom", "0.5rem");
        return toolbar;
    }

    private void triggerAnalysisMaster(Set<ScreenerMasterDto> selectedItems, Grid<ScreenerMasterDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerMasterDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequestEvent(UUID.randomUUID(),
                    item.companyId(), item.ticker(), item.market(), LocalDate.now(), null));
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private void triggerAnalysisNewsSentiment(Set<ScreenerNewsSentimentDto> selectedItems, Grid<ScreenerNewsSentimentDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerNewsSentimentDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequestEvent(UUID.randomUUID(),
                    item.companyId(), item.ticker(), item.market(), LocalDate.now(), null));
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private void triggerAnalysisInsider(Set<ScreenerInsiderDto> selectedItems, Grid<ScreenerInsiderDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerInsiderDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequestEvent(UUID.randomUUID(),
                    item.companyId().intValue(), item.ticker(), item.market(), LocalDate.now(), null));
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private void triggerAnalysisStandard(Set<ScreenerDto> selectedItems, Grid<ScreenerDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequestEvent(UUID.randomUUID(),
                    item.companyId(), item.ticker(), item.market(), LocalDate.now(), null));
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


    private Grid<ScreenerMasterDto> createMasterGrid() {
        Grid<ScreenerMasterDto> grid = new Grid<>(ScreenerMasterDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<ScreenerMasterDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }

        grid.addColumn(ScreenerMasterDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerMasterDto> nameCol = grid.addColumn(ScreenerMasterDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerMasterDto::description);

        grid.addColumn(ScreenerMasterDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.sizeBadge(item.companySize())))
                .setHeader("Size").setAutoWidth(true).setKey("size")
                .setComparator(Comparator.comparing(ScreenerMasterDto::companySize, Comparator.nullsLast(CompanySize::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(Comparator.comparing(ScreenerMasterDto::compositeSignal, Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(Comparator.comparing(ScreenerMasterDto::qualityScore, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel(), item.newsSentimentScore())))
                .setHeader("News Sentiment (30D)").setAutoWidth(true)
                .setComparator(Comparator.comparing(ScreenerMasterDto::newsSentimentScore, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(item -> item.newsCount30d() != null ? String.format("%d articles", item.newsCount30d()) : "-")
                .setHeader("Coverage (30D)").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerMasterDto::newsCount30d, Comparator.nullsFirst(Integer::compareTo)));

        grid.addColumn(ScreenerMasterDto::financialTrendScore).setHeader("Trend Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerMasterDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("Price").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerMasterDto::sharePrice, Comparator.nullsLast(Float::compareTo)));
        grid.addColumn(ScreenerMasterDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        grid.addColumn(ScreenerMasterDto::qualityRank).setHeader("Q-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::valueRank).setHeader("V-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::fscoreRank).setHeader("F-Rank").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(item -> createCompanyDetailsButton(item.companyId())).setHeader("Details").setAutoWidth(true);

        return grid;
    }

    private Grid<ScreenerNewsSentimentDto> createNewsSentimentGrid() {
        Grid<ScreenerNewsSentimentDto> grid = new Grid<>(ScreenerNewsSentimentDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<ScreenerNewsSentimentDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }

        grid.addColumn(ScreenerNewsSentimentDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerNewsSentimentDto> nameCol = grid.addColumn(ScreenerNewsSentimentDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerNewsSentimentDto::description);

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel7d(), item.newsSentiment7d())))
                .setHeader("7D Sentiment").setAutoWidth(true)
                .setComparator(Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment7d, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel14d(), item.newsSentiment14d())))
                .setHeader("14D Sentiment").setAutoWidth(true)
                .setComparator(Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment14d, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel30d(), item.newsSentiment30d())))
                .setHeader("30D Sentiment").setAutoWidth(true)
                .setComparator(Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment30d, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(item -> String.format("%d / %d / %d",
                        item.newsCount7d() != null ? item.newsCount7d() : 0,
                        item.newsCount14d() != null ? item.newsCount14d() : 0,
                        item.newsCount30d() != null ? item.newsCount30d() : 0))
                .setHeader("News Coverage (7D/14D/30D)").setAutoWidth(true).setSortable(true);

        grid.addColumn(item -> item.avgRelevance14d() != null ? String.format(Locale.US, "%.0f%%", item.avgRelevance14d() * 100) : "-")
                .setHeader("Avg Relevance (14D)").setAutoWidth(true).setSortable(true);

        grid.setItemDetailsRenderer(createNewsDetailsRenderer());
        grid.setDetailsVisibleOnClick(true);

        grid.addComponentColumn(item -> createCompanyDetailsButton(item.companyId())).setHeader("Details").setAutoWidth(true);

        return grid;
    }

    private ComponentRenderer<VerticalLayout, ScreenerNewsSentimentDto> createNewsDetailsRenderer() {
        return new ComponentRenderer<>(item -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(true);
            layout.setSpacing(true);
            layout.addClassName(LumoUtility.Background.CONTRAST_5);
            layout.getStyle().set("border-radius", "8px");
            layout.getStyle().set("margin", "0.5rem");

            H4 header = new H4("Recent News Coverage for " + item.companyName());
            header.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
            layout.add(header);

            List<NewsTickerDto> newsList = companyNewsApi.getNewsByTicker(item.ticker(), LocalDate.now().minusDays(14));
            if (newsList.isEmpty()) {
                layout.add(new Paragraph("No individual news articles found in the last 14 days."));
            } else {
                for (NewsTickerDto news : newsList) {
                    VerticalLayout articleBox = new VerticalLayout();
                    articleBox.setPadding(false);
                    articleBox.setSpacing(false);
                    articleBox.addClassNames(LumoUtility.Padding.Vertical.XSMALL);

                    HorizontalLayout titleRow = new HorizontalLayout();
                    titleRow.setWidthFull();
                    titleRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
                    titleRow.setAlignItems(Alignment.CENTER);

                    Span headline = new Span(news.title());
                    headline.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);

                    Span scoreBadge = ViewHelper.newsSentimentBadge(news.tickerSentimentLabel(), news.tickerSentimentScore());

                    titleRow.add(headline, scoreBadge);

                    Paragraph meta = new Paragraph(String.format("Source: %s | Published: %s | Relevance: %.0f%%",
                            news.source() != null ? news.source() : "Unknown",
                            news.timePublished() != null ? news.timePublished().toString().substring(0, 10) : "N/A",
                            news.relevanceScore() != null ? news.relevanceScore() * 100 : 0));
                    meta.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.XSMALL);

                    Paragraph summary = new Paragraph(news.summary());
                    summary.addClassNames(LumoUtility.TextColor.BODY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Top.NONE);

                    articleBox.add(titleRow, meta, summary);

                    Div divider = new Div();
                    divider.setHeight("1px");
                    divider.setWidthFull();
                    divider.addClassName(LumoUtility.Background.CONTRAST_10);

                    layout.add(articleBox, divider);
                }
            }

            return layout;
        });
    }

    private Grid<ScreenerDto> createStandardGrid() {
        Grid<ScreenerDto> grid = new Grid<>(ScreenerDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<ScreenerDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }

        grid.addColumn(ScreenerDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerDto> nameCol = grid.addColumn(ScreenerDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerDto::description);

        grid.addColumn(ScreenerDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.sizeBadge(item.companySize())))
                .setHeader("Size").setAutoWidth(true).setKey("size")
                .setComparator(Comparator.comparing(ScreenerDto::companySize, Comparator.nullsLast(CompanySize::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(Comparator.comparing(ScreenerDto::compositeSignal, Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(Comparator.comparing(ScreenerDto::qualityScore, Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(ScreenerDto::financialTrendScore).setHeader("Trend Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("Price").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerDto::sharePrice, Comparator.nullsLast(Float::compareTo)));
        grid.addColumn(ScreenerDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(item -> createCompanyDetailsButton(item.companyId())).setHeader("Details").setAutoWidth(true);

        return grid;
    }

    private Grid<ScreenerInsiderDto> createInsiderGrid() {
        Grid<ScreenerInsiderDto> grid = new Grid<>(ScreenerInsiderDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<ScreenerInsiderDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }

        grid.addColumn(ScreenerInsiderDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerInsiderDto> nameCol = grid.addColumn(ScreenerInsiderDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(item -> item.sector() + " | " + item.industry());

        grid.addColumn(ScreenerInsiderDto::sector).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.sizeBadge(item.companySize())))
                .setHeader("Size").setAutoWidth(true).setKey("size")
                .setComparator(java.util.Comparator.comparing(ScreenerInsiderDto::companySize, java.util.Comparator.nullsLast(CompanySize::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel(), item.newsSentimentScore())))
                .setHeader("News Sentiment").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerInsiderDto::newsSentimentScore, Comparator.nullsFirst(Float::compareTo)));

        grid.addColumn(new NumberRenderer<>(ScreenerInsiderDto::csuiteBuysValue3m, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("C-Suite Buys 3M").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerInsiderDto::csuiteBuysValue3m, Comparator.nullsFirst(Double::compareTo)));

        grid.addColumn(new NumberRenderer<>(ScreenerInsiderDto::csuiteBuysValue6m, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("C-Suite Buys 6M").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerInsiderDto::csuiteBuysValue6m, Comparator.nullsFirst(Double::compareTo)));

        grid.addColumn(new NumberRenderer<>(ScreenerInsiderDto::csuiteBuysValue12m, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("C-Suite Buys LTM").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(ScreenerInsiderDto::csuiteBuysValue12m, Comparator.nullsFirst(Double::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> {
            boolean hasBuy = item.hasClusterBuy() != null && item.hasClusterBuy();
            Span badge = new Span(hasBuy ? "Yes" : "No");
            badge.getElement().getThemeList().addAll(List.of("badge", hasBuy ? "success" : "contrast"));
            return badge;
        }))
                .setHeader("Cluster Buy").setAutoWidth(true).setSortable(true)
                .setComparator(Comparator.comparing(item -> item.hasClusterBuy() != null && item.hasClusterBuy(), Boolean::compareTo));

        grid.addComponentColumn(item -> createCompanyDetailsButton(item.companyId().intValue())).setHeader("Details").setAutoWidth(true);

        return grid;
    }

    private <T> void setupFilters(Grid<T> grid, GridListDataView<T> dataView, Class<T> type) {
        HeaderRow filterRow = grid.appendHeaderRow();
        ScreenerFilter<T> filter = new ScreenerFilter<>(type);
        dataView.setFilter(filter::test);

        if (grid.getColumnByKey("ticker") != null) {
            ViewHelper.addFilter(grid, filterRow, "ticker", "Ticker", v -> {
                filter.ticker = v;
                dataView.refreshAll();
            });
        }
        if (grid.getColumnByKey("companyName") != null) {
            ViewHelper.addFilter(grid, filterRow, "companyName", "Company", v -> {
                filter.companyName = v;
                dataView.refreshAll();
            });
        }
        if (grid.getColumnByKey("sector") != null) {
            ViewHelper.addFilter(grid, filterRow, "sector", "Sector", v -> {
                filter.sector = v;
                dataView.refreshAll();
            });
        }
        if (grid.getColumnByKey("size") != null) {
            ViewHelper.addFilter(grid, filterRow, "size", "Size", v -> {
                filter.size = v;
                dataView.refreshAll();
            });
        }
    }

    private Button createCompanyDetailsButton(int companyId) {
        return ViewHelper.createCompanyDetailsButton(companyMetadataApi, companyFinancialDataApi, companySharePriceApi, companyNewsApi, companyInsiderTransactionApi, objectMapper, companyId, true);
    }

    /**
     * Unified filter for both Master and Standard screener grids.
     * Uses duck-typing via accessor lambdas set in the constructor.
     */
    private static class ScreenerFilter<T> {
        private final Function<T, String> tickerFn;
        private final Function<T, String> nameFn;
        private final Function<T, String> sectorFn;
        private final Function<T, String> sizeFn;
        String ticker = "", companyName = "", sector = "", size = "";

        ScreenerFilter(Class<T> type) {
            if (type == ScreenerMasterDto.class) {
                tickerFn = t -> ((ScreenerMasterDto) t).ticker();
                nameFn = t -> ((ScreenerMasterDto) t).companyName();
                sectorFn = t -> ((ScreenerMasterDto) t).sectorName();
                sizeFn = t -> {
                    CompanySize size = ((ScreenerMasterDto) t).companySize();
                    return size != null ? size.getDisplayName() : "";
                };
            } else if (type == ScreenerNewsSentimentDto.class) {
                tickerFn = t -> ((ScreenerNewsSentimentDto) t).ticker();
                nameFn = t -> ((ScreenerNewsSentimentDto) t).companyName();
                sectorFn = t -> ((ScreenerNewsSentimentDto) t).sectorName();
                sizeFn = t -> {
                    CompanySize size = ((ScreenerNewsSentimentDto) t).companySize();
                    return size != null ? size.getDisplayName() : "";
                };
            } else if (type == ScreenerInsiderDto.class) {
                tickerFn = t -> ((ScreenerInsiderDto) t).ticker();
                nameFn = t -> ((ScreenerInsiderDto) t).companyName();
                sectorFn = t -> ((ScreenerInsiderDto) t).sector();
                sizeFn = t -> {
                    CompanySize size = ((ScreenerInsiderDto) t).companySize();
                    return size != null ? size.getDisplayName() : "";
                };
            } else {
                tickerFn = t -> ((ScreenerDto) t).ticker();
                nameFn = t -> ((ScreenerDto) t).companyName();
                sectorFn = t -> ((ScreenerDto) t).sectorName();
                sizeFn = t -> {
                    CompanySize size = ((ScreenerDto) t).companySize();
                    return size != null ? size.getDisplayName() : "";
                };
            }
        }

        boolean test(T item) {
            return ViewHelper.matches(tickerFn.apply(item), ticker)
                    && ViewHelper.matches(nameFn.apply(item), companyName)
                    && ViewHelper.matches(sectorFn.apply(item), sector)
                    && ViewHelper.matches(sizeFn.apply(item), size);
        }
    }
}
