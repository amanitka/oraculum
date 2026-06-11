package com.oraculum.ui.views;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.company.api.dto.ScreenerDto;
import com.oraculum.company.api.dto.ScreenerMasterDto;
import com.oraculum.company.api.dto.ScreenerNewsSentimentDto;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Route(value = "screener", layout = MainLayout.class)
@PageTitle("Screener")
public class ScreenerView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final AnalysisRequestService analysisRequestService;
    private final VerticalLayout gridContainer;

    public ScreenerView(CompanyApi companyApi, AnalysisRequestService analysisRequestService) {
        this.companyApi = companyApi;
        this.analysisRequestService = analysisRequestService;
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
        Tab tabPiotroski = new Tab("Piotroski 7+");

        Tabs tabs = new Tabs(tabMaster, tabNews, tabQuality, tabValue, tabGraham, tabPiotroski);
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

        if ("Master Ranks".equals(tabLabel)) {
            Grid<ScreenerMasterDto> grid = createMasterGrid();
            GridListDataView<ScreenerMasterDto> dataView = grid.setItems(companyApi.getMasterScreener());
            setupFilters(grid, dataView, ScreenerMasterDto.class);
            runAnalysisBtn.addClickListener(_ -> triggerAnalysisMaster(grid.getSelectedItems(), grid));
            gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
        } else if ("News Sentiment".equals(tabLabel)) {
            Grid<ScreenerNewsSentimentDto> grid = createNewsSentimentGrid();
            List<ScreenerNewsSentimentDto> data = companyApi.getNewsSentimentScreener().stream()
                    .filter(item -> item.newsCount30d() != null && item.newsCount30d() > 0)
                    .collect(Collectors.toList());
            GridListDataView<ScreenerNewsSentimentDto> dataView = grid.setItems(data);
            setupFilters(grid, dataView, ScreenerNewsSentimentDto.class);
            runAnalysisBtn.addClickListener(_ -> triggerAnalysisNewsSentiment(grid.getSelectedItems(), grid));
            gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
        } else {
            Grid<ScreenerDto> grid = createStandardGrid();
            List<ScreenerDto> data = switch (tabLabel) {
                case "Quality Compounders" -> companyApi.getQualityCompoundersScreener();
                case "Undervalued" -> companyApi.getUndervaluedScreener();
                case "Graham Deep Value" -> companyApi.getGrahamDeepValueScreener();
                case "Piotroski 7+" -> companyApi.getPiotroskiScreener();
                default -> List.of();
            };
            GridListDataView<ScreenerDto> dataView = grid.setItems(data);
            setupFilters(grid, dataView, ScreenerDto.class);
            runAnalysisBtn.addClickListener(_ -> triggerAnalysisStandard(grid.getSelectedItems(), grid));
            gridContainer.add(toolbar, ViewHelper.wrapInCard(grid));
        }
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
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
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequest(UUID.randomUUID(),
                    item.companyId(), item.ticker(), item.market(), LocalDate.now(), null));
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private void triggerAnalysisNewsSentiment(Set<ScreenerNewsSentimentDto> selectedItems, Grid<ScreenerNewsSentimentDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerNewsSentimentDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequest(UUID.randomUUID(),
                    item.companyId(), item.ticker(), item.market(), LocalDate.now(), null));
        }
        ViewHelper.showSuccess("Triggered analysis for " + selectedItems.size() + " companies.");
        grid.deselectAll();
    }

    private void triggerAnalysisStandard(Set<ScreenerDto> selectedItems, Grid<ScreenerDto> grid) {
        if (!validateBatchSelection(selectedItems.size())) return;
        for (ScreenerDto item : selectedItems) {
            analysisRequestService.requestAnalysis(new CompanyAnalysisRequest(UUID.randomUUID(),
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
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::companySize, java.util.Comparator.nullsLast(CompanySize::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::qualityScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel(), item.newsSentimentScore())))
                .setHeader("News Sentiment (30D)").setAutoWidth(true)
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::newsSentimentScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(item -> item.newsCount30d() != null ? String.format("%d articles", item.newsCount30d()) : "-")
                .setHeader("Coverage (30D)").setAutoWidth(true).setSortable(true);

        grid.addColumn(ScreenerMasterDto::piotroskiFScore).setHeader("F-Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerMasterDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        grid.addColumn(ScreenerMasterDto::qualityRank).setHeader("Q-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::valueRank).setHeader("V-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::fscoreRank).setHeader("F-Rank").setAutoWidth(true).setSortable(true);

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
                .setComparator(java.util.Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment7d, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel14d(), item.newsSentiment14d())))
                .setHeader("14D Sentiment").setAutoWidth(true)
                .setComparator(java.util.Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment14d, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.newsSentimentBadge(item.newsSentimentLabel30d(), item.newsSentiment30d())))
                .setHeader("30D Sentiment").setAutoWidth(true)
                .setComparator(java.util.Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment30d, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(item -> String.format("%d / %d / %d",
                        item.newsCount7d() != null ? item.newsCount7d() : 0,
                        item.newsCount14d() != null ? item.newsCount14d() : 0,
                        item.newsCount30d() != null ? item.newsCount30d() : 0))
                .setHeader("News Coverage (7D/14D/30D)").setAutoWidth(true).setSortable(true);

        grid.addColumn(item -> item.avgRelevance14d() != null ? String.format(Locale.US, "%.0f%%", item.avgRelevance14d() * 100) : "-")
                .setHeader("Avg Relevance (14D)").setAutoWidth(true).setSortable(true);

        grid.setItemDetailsRenderer(createNewsDetailsRenderer());
        grid.setDetailsVisibleOnClick(true);

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

            List<NewsTickerDto> newsList = companyApi.getNewsByTicker(item.ticker(), LocalDate.now().minusDays(14));
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
                    titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                    titleRow.setAlignItems(FlexComponent.Alignment.CENTER);

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
                .setComparator(java.util.Comparator.comparing(ScreenerDto::companySize, java.util.Comparator.nullsLast(CompanySize::compareTo)));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::qualityScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(ScreenerDto::piotroskiFScore).setHeader("F-Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

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


    /**
     * Unified filter for both Master and Standard screener grids.
     * Uses duck-typing via accessor lambdas set in the constructor.
     */
    private static class ScreenerFilter<T> {
        private final java.util.function.Function<T, String> tickerFn;
        private final java.util.function.Function<T, String> nameFn;
        private final java.util.function.Function<T, String> sectorFn;
        private final java.util.function.Function<T, String> sizeFn;
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
