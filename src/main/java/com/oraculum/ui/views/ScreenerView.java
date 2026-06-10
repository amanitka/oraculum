package com.oraculum.ui.views;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.dto.ScreenerDto;
import com.oraculum.company.api.dto.ScreenerMasterDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.service.AnalysisRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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

@Route(value = "screener", layout = MainLayout.class)
@PageTitle("Screener")
public class ScreenerView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final AnalysisRequestService analysisRequestService;
    private final VerticalLayout gridContainer;

    public ScreenerView(CompanyApi companyApi, AnalysisRequestService analysisRequestService) {
        this.companyApi = companyApi;
        this.analysisRequestService = analysisRequestService;
        setSizeFull();
        setPadding(false);

        H3 title = new H3("Investment Screeners");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem"); // Pushes the text down significantly from the top menu
        Paragraph description = new Paragraph("Select a screening strategy below to view fully-materialized market data.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        Tab tabMaster = new Tab("Master Ranks");
        Tab tabQuality = new Tab("Quality Compounders");
        Tab tabValue = new Tab("Undervalued");
        Tab tabGraham = new Tab("Graham Deep Value");
        Tab tabPiotroski = new Tab("Piotroski 7+");

        Tabs tabs = new Tabs(tabMaster, tabQuality, tabValue, tabGraham, tabPiotroski);
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
            setupFilters(grid, dataView, true);
            runAnalysisBtn.addClickListener(_ -> triggerAnalysisMaster(grid.getSelectedItems(), grid));
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
            setupFilters(grid, dataView, false);
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

        grid.addColumn(ScreenerMasterDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerMasterDto> nameCol = grid.addColumn(ScreenerMasterDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerMasterDto::description);

        grid.addColumn(ScreenerMasterDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(ScreenerMasterDto::companySize).setHeader("Size").setAutoWidth(true).setKey("size").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::qualityScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(ScreenerMasterDto::piotroskiFScore).setHeader("F-Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerMasterDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        grid.addColumn(ScreenerMasterDto::qualityRank).setHeader("Q-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::valueRank).setHeader("V-Rank").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerMasterDto::fscoreRank).setHeader("F-Rank").setAutoWidth(true).setSortable(true);

        return grid;
    }

    private Grid<ScreenerDto> createStandardGrid() {
        Grid<ScreenerDto> grid = new Grid<>(ScreenerDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        grid.addColumn(ScreenerDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerDto> nameCol = grid.addColumn(ScreenerDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerDto::description);

        grid.addColumn(ScreenerDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(ScreenerDto::companySize).setHeader("Size").setAutoWidth(true).setKey("size").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::qualityScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(ScreenerDto::piotroskiFScore).setHeader("F-Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        return grid;
    }

    private <T> void setupFilters(Grid<T> grid, GridListDataView<T> dataView, boolean isMaster) {
        HeaderRow filterRow = grid.appendHeaderRow();
        ScreenerFilter<T> filter = new ScreenerFilter<>(isMaster);
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
        ViewHelper.addFilter(grid, filterRow, "size", "Size", v -> {
            filter.size = v;
            dataView.refreshAll();
        });
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

        ScreenerFilter(boolean isMaster) {
            if (isMaster) {
                tickerFn = t -> ((ScreenerMasterDto) t).ticker();
                nameFn = t -> ((ScreenerMasterDto) t).companyName();
                sectorFn = t -> ((ScreenerMasterDto) t).sectorName();
                sizeFn = t -> ((ScreenerMasterDto) t).companySize();
            } else {
                tickerFn = t -> ((ScreenerDto) t).ticker();
                nameFn = t -> ((ScreenerDto) t).companyName();
                sectorFn = t -> ((ScreenerDto) t).sectorName();
                sizeFn = t -> ((ScreenerDto) t).companySize();
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
