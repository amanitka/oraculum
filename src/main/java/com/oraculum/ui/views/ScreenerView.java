package com.oraculum.ui.views;

import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.dto.ScreenerDto;
import com.oraculum.company.api.dto.ScreenerMasterDto;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Route(value = "screener", layout = MainLayout.class)
@PageTitle("Screener")
public class ScreenerView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final VerticalLayout gridContainer;

    public ScreenerView(CompanyApi companyApi) {
        this.companyApi = companyApi;
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
        if ("Master Ranks".equals(tabLabel)) {
            Grid<ScreenerMasterDto> grid = createMasterGrid();
            List<ScreenerMasterDto> data = companyApi.getMasterScreener();
            GridListDataView<ScreenerMasterDto> dataView = grid.setItems(data);
            setupMasterFilters(grid, dataView);
            gridContainer.add(wrapGrid(grid));
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
            setupStandardFilters(grid, dataView);
            gridContainer.add(wrapGrid(grid));
        }
    }

    private Div wrapGrid(Component grid) {
        Div card = new Div();
        card.addClassName("screener-card");
        card.setSizeFull();
        card.add(grid);
        return card;
    }

    private Span createSignalBadge(String signal) {
        Span badge = new Span(signal != null ? signal : "N/A");
        badge.getElement().getThemeList().add("badge");
        if ("STRONG_BUY".equals(signal) || "BUY".equals(signal)) {
            badge.getElement().getThemeList().add("success");
        } else if ("AVOID".equals(signal) || "SELL".equals(signal)) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private Span createQualitySpan(Float score) {
        Span span = new Span(score != null ? String.format(Locale.US, "%.1f", score) : "-");
        if (score != null) {
            if (score >= 80) span.addClassName(LumoUtility.TextColor.SUCCESS);
            else if (score < 40) span.addClassName(LumoUtility.TextColor.ERROR);
        }
        span.addClassName(LumoUtility.FontWeight.BOLD);
        return span;
    }

    private Grid<ScreenerMasterDto> createMasterGrid() {
        Grid<ScreenerMasterDto> grid = new Grid<>(ScreenerMasterDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");

        grid.addColumn(ScreenerMasterDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerMasterDto> nameCol = grid.addColumn(ScreenerMasterDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerMasterDto::description);

        grid.addColumn(ScreenerMasterDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(ScreenerMasterDto::companySize).setHeader("Size").setAutoWidth(true).setKey("size").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> createSignalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerMasterDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> createQualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
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

        grid.addColumn(ScreenerDto::ticker).setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<ScreenerDto> nameCol = grid.addColumn(ScreenerDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(ScreenerDto::description);

        grid.addColumn(ScreenerDto::sectorName).setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);
        grid.addColumn(ScreenerDto::companySize).setHeader("Size").setAutoWidth(true).setKey("size").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(item -> createSignalBadge(item.compositeSignal()))).setHeader("Signal").setAutoWidth(true).setKey("signal")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::compositeSignal, java.util.Comparator.nullsLast(String::compareTo)));
        grid.addColumn(new ComponentRenderer<>(item -> createQualitySpan(item.qualityScore()))).setHeader("Quality").setAutoWidth(true).setKey("quality")
                .setComparator(java.util.Comparator.comparing(ScreenerDto::qualityScore, java.util.Comparator.nullsLast(Float::compareTo)));

        grid.addColumn(ScreenerDto::piotroskiFScore).setHeader("F-Score").setAutoWidth(true).setSortable(true);
        grid.addColumn(new NumberRenderer<>(ScreenerDto::sharePrice, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(ScreenerDto::peRatio).setHeader("P/E").setAutoWidth(true).setSortable(true);

        return grid;
    }

    private void setupMasterFilters(Grid<ScreenerMasterDto> grid, GridListDataView<ScreenerMasterDto> dataView) {
        HeaderRow filterRow = grid.appendHeaderRow();
        MasterFilter filter = new MasterFilter();
        dataView.setFilter(filter::test);

        TextField tickerFilter = createFilterField("Ticker", val -> {
            filter.ticker = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("ticker")).setComponent(tickerFilter);

        TextField nameFilter = createFilterField("Company", val -> {
            filter.companyName = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("companyName")).setComponent(nameFilter);

        TextField sectorFilter = createFilterField("Sector", val -> {
            filter.sector = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("sector")).setComponent(sectorFilter);

        TextField sizeFilter = createFilterField("Size", val -> {
            filter.size = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("size")).setComponent(sizeFilter);
    }

    private void setupStandardFilters(Grid<ScreenerDto> grid, GridListDataView<ScreenerDto> dataView) {
        HeaderRow filterRow = grid.appendHeaderRow();
        StandardFilter filter = new StandardFilter();
        dataView.setFilter(filter::test);

        TextField tickerFilter = createFilterField("Ticker", val -> {
            filter.ticker = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("ticker")).setComponent(tickerFilter);

        TextField nameFilter = createFilterField("Company", val -> {
            filter.companyName = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("companyName")).setComponent(nameFilter);

        TextField sectorFilter = createFilterField("Sector", val -> {
            filter.sector = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("sector")).setComponent(sectorFilter);

        TextField sizeFilter = createFilterField("Size", val -> {
            filter.size = val;
            dataView.refreshAll();
        });
        filterRow.getCell(grid.getColumnByKey("size")).setComponent(sizeFilter);
    }

    private TextField createFilterField(String placeholder, Consumer<String> filterAction) {
        TextField filter = new TextField();
        filter.setPlaceholder("Filter " + placeholder);
        filter.setClearButtonVisible(true);
        filter.setWidthFull();
        filter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filter.setValueChangeMode(ValueChangeMode.EAGER);
        filter.addValueChangeListener(e -> filterAction.accept(e.getValue()));
        return filter;
    }

    private static class MasterFilter {
        String ticker = "";
        String companyName = "";
        String sector = "";
        String size = "";

        boolean test(ScreenerMasterDto dto) {
            return matches(dto.ticker(), ticker) && matches(dto.companyName(), companyName) && matches(dto.sectorName(), sector) && matches(dto.companySize(), size);
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }

    private static class StandardFilter {
        String ticker = "";
        String companyName = "";
        String sector = "";
        String size = "";

        boolean test(ScreenerDto dto) {
            return matches(dto.ticker(), ticker) && matches(dto.companyName(), companyName) && matches(dto.sectorName(), sector) && matches(dto.companySize(), size);
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty() || (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }
}
