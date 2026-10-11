package com.oraculum.ui.components;

import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.factory.CompanyDetailsButtonFactory;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MarketGridComponent extends VerticalLayout {

    private final Grid<CompanyOverviewDto> grid;
    private final CompanyDetailsButtonFactory detailsButtonFactory;
    private final MarketFilterState filterState = new MarketFilterState();
    private Map<Integer, CompanyInstitutionalFlowDto> currentFlowMap = Map.of();
    private GridListDataView<CompanyOverviewDto> dataView;
    private boolean initialized = false;

    public MarketGridComponent(CompanyDetailsButtonFactory detailsButtonFactory) {
        this.detailsButtonFactory = detailsButtonFactory;
        this.grid = new Grid<>(CompanyOverviewDto.class, false);

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        initGridStyle();
        add(grid);
    }

    private void initGridStyle() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<CompanyOverviewDto> multiModel) {
            multiModel.setSelectionColumnFrozen(true);
        }
    }

    public void updateData(List<CompanyOverviewDto> companies,
                           Map<Integer, CompanyInstitutionalFlowDto> instFlowMap) {
        this.currentFlowMap = instFlowMap != null ? instFlowMap : Map.of();
        List<CompanyOverviewDto> items = companies != null ? companies : List.of();

        if (!initialized) {
            buildColumns();
            this.dataView = grid.setItems(items);
            setupFilterRow();
            initialized = true;
        } else {
            this.dataView = grid.setItems(items);
            dataView.setFilter(filterState::test);
        }
    }

    public Set<CompanyOverviewDto> getSelectedItems() {
        return grid.getSelectedItems();
    }

    public void deselectAll() {
        grid.deselectAll();
    }

    private void buildColumns() {
        addBasicColumns();
        addPriceColumns();
        add13FFlowColumns();
        addQualityAndActionColumns();
    }

    private void addBasicColumns() {
        grid.addColumn(CompanyOverviewDto::ticker)
                .setHeader("Ticker").setAutoWidth(true).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<CompanyOverviewDto> nameCol = grid.addColumn(CompanyOverviewDto::companyName)
                .setHeader("Company").setAutoWidth(true).setFrozen(true).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(CompanyOverviewDto::description);

        grid.addColumn(CompanyOverviewDto::sectorName)
                .setHeader("Sector").setAutoWidth(true).setKey("sector").setSortable(true);

        grid.addColumn(CompanyOverviewDto::industryName)
                .setHeader("Industry").setAutoWidth(true).setKey("industry").setSortable(true);
    }

    private void addPriceColumns() {
        grid.addColumn(c -> c.sharePrice() != null ? String.format(Locale.US, "%s%.2f", ViewHelper.getCurrencySymbol(c.currency(), c.market()), c.sharePrice()) : "-")
                .setHeader("Price").setWidth("110px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::sharePrice));

        grid.addColumn(item -> ViewHelper.formatCompact(item.marketCapitalization()))
                .setHeader("Market Cap").setWidth("120px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::marketCapitalization));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1d())))
                .setHeader("1D %").setWidth("100px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1d));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1w())))
                .setHeader("1W %").setWidth("100px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1w));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.priceChangeSpan(item.priceChange1m())))
                .setHeader("1M %").setWidth("120px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::priceChange1m));
    }

    private void add13FFlowColumns() {
        grid.addColumn(new ComponentRenderer<>(item -> {
            var flow = currentFlowMap.get(item.companyId());
            return ViewHelper.priceChangeSpan(flow != null ? flow.sharesChangePct() : null);
        })).setHeader(ViewHelper.createExplainedHeader("13F Flow %", "Institutional net shares change % in the latest quarter reported to SEC."))
           .setWidth("130px").setFlexGrow(0).setSortable(true)
           .setComparator(Comparator.comparing(
                   item -> {
                       var flow = currentFlowMap.get(item.companyId());
                       return flow != null ? flow.sharesChangePct() : null;
                   },
                   Comparator.nullsLast(Float::compareTo)
           ));

        grid.addColumn(item -> {
            var flow = currentFlowMap.get(item.companyId());
            if (flow == null || flow.putCallRatio() == null) return "-";
            return String.format(Locale.US, "%.2f", flow.putCallRatio());
        }).setHeader(ViewHelper.createExplainedHeader("P/C Ratio", "Put / Call option value ratio held by institutional managers."))
          .setWidth("120px").setFlexGrow(0).setSortable(true);
    }

    private void addQualityAndActionColumns() {
        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.qualitySpan(item.qualityScore())))
                .setHeader(ViewHelper.createExplainedHeader("Quality", "Quality Score (0-100). Higher is better. Based on profitability and balance sheet strength."))
                .setWidth("110px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::qualityScore));

        grid.addColumn(new ComponentRenderer<>(item -> ViewHelper.signalBadge(item.compositeSignal())))
                .setHeader("Signal").setWidth("130px").setFlexGrow(0).setSortable(true)
                .setComparator(ViewHelper.nullsAlwaysLast(CompanyOverviewDto::compositeSignal));

        grid.addComponentColumn(item -> detailsButtonFactory.createButton(item.companyId(), true))
                .setHeader("Details").setAutoWidth(true);
    }

    private void setupFilterRow() {
        HeaderRow filterRow = grid.appendHeaderRow();
        dataView.setFilter(filterState::test);

        ViewHelper.addFilter(grid, filterRow, "ticker", "Ticker", v -> {
            filterState.ticker = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "companyName", "Company", v -> {
            filterState.companyName = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "sector", "Sector", v -> {
            filterState.sector = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "industry", "Industry", v -> {
            filterState.industry = v;
            dataView.refreshAll();
        });
    }

    private static class MarketFilterState {
        String ticker = "";
        String companyName = "";
        String sector = "";
        String industry = "";

        boolean test(CompanyOverviewDto item) {
            return ViewHelper.matches(item.ticker(), ticker)
                    && ViewHelper.matches(item.companyName(), companyName)
                    && ViewHelper.matches(item.sectorName(), sector)
                    && ViewHelper.matches(item.industryName(), industry);
        }
    }
}
