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

import java.util.*;

public class InstitutionalFlowGridComponent extends VerticalLayout {

    private final Grid<CompanyOverviewDto> grid;
    private final CompanyDetailsButtonFactory detailsButtonFactory;
    private final MarketFilterState filterState = new MarketFilterState();
    private Map<Integer, CompanyInstitutionalFlowDto> currentFlowMap = Map.of();
    private GridListDataView<CompanyOverviewDto> dataView;
    private boolean initialized = false;

    public InstitutionalFlowGridComponent(CompanyDetailsButtonFactory detailsButtonFactory) {
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

        List<CompanyOverviewDto> filtered = filterAndSortCompanies(companies);

        if (!initialized) {
            buildColumns();
            this.dataView = grid.setItems(filtered);
            setupFilterRow();
            initialized = true;
        } else {
            this.dataView = grid.setItems(filtered);
            dataView.setFilter(filterState::test);
        }
    }

    private List<CompanyOverviewDto> filterAndSortCompanies(List<CompanyOverviewDto> companies) {
        if (companies == null) return List.of();
        return companies.stream()
                .filter(c -> currentFlowMap.containsKey(c.companyId()))
                .sorted((a, b) -> {
                    var fa = currentFlowMap.get(a.companyId());
                    var fb = currentFlowMap.get(b.companyId());
                    long va = fa != null ? fa.totalInstValueUsd() : 0L;
                    long vb = fb != null ? fb.totalInstValueUsd() : 0L;
                    return Long.compare(vb, va);
                })
                .toList();
    }

    private void buildColumns() {
        addIdentifierColumns();
        addPortfolioValueColumns();
        addFlowColumns();
        addBreadthAndSentimentColumns();
        addBuyersAndDetailsColumns();
    }

    private void addIdentifierColumns() {
        grid.addColumn(CompanyOverviewDto::ticker)
                .setHeader("Ticker").setWidth("100px").setFlexGrow(0).setFrozen(true).setKey("ticker").setSortable(true);

        Grid.Column<CompanyOverviewDto> nameCol = grid.addColumn(CompanyOverviewDto::companyName)
                .setHeader("Company").setWidth("220px").setFlexGrow(0).setKey("companyName").setSortable(true);
        nameCol.setTooltipGenerator(CompanyOverviewDto::description);

        grid.addColumn(CompanyOverviewDto::sectorName)
                .setHeader("Sector").setWidth("160px").setFlexGrow(0).setKey("sector").setSortable(true);
    }

    private void addPortfolioValueColumns() {
        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return flow != null && flow.totalInstValueUsd() != null ? ViewHelper.formatCompact(flow.totalInstValueUsd().floatValue()) : "-";
                }).setHeader(ViewHelper.createExplainedHeader("Inst. Portfolio ($)", "Total USD value held by institutional managers in this quarter."))
                .setWidth("185px").setFlexGrow(0).setSortable(true)
                .setComparator(Comparator.comparing(
                        item -> {
                            var flow = currentFlowMap.get(item.companyId());
                            return flow != null ? flow.totalInstValueUsd() : null;
                        },
                        Comparator.nullsLast(Long::compareTo)
                ));

        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return flow != null && flow.totalInstShares() != null ? ViewHelper.formatCompactNumber(flow.totalInstShares().floatValue()) : "-";
                }).setHeader(ViewHelper.createExplainedHeader("Inst. Shares", "Total number of shares held by institutions."))
                .setWidth("150px").setFlexGrow(0).setSortable(true);
    }

    private void addFlowColumns() {
        grid.addColumn(new ComponentRenderer<>(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return ViewHelper.priceChangeSpan(flow != null ? flow.sharesChangePct() : null);
                })).setHeader(ViewHelper.createExplainedHeader("QoQ Flow %", "Percentage change in institutional shares vs previous quarter."))
                .setWidth("160px").setFlexGrow(0).setSortable(true)
                .setComparator(Comparator.comparing(
                        item -> {
                            var flow = currentFlowMap.get(item.companyId());
                            return flow != null ? flow.sharesChangePct() : null;
                        },
                        Comparator.nullsLast(Float::compareTo)
                ));

        grid.addColumn(new ComponentRenderer<>(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return ViewHelper.deltaNumberSpan(flow != null ? flow.netSharesChange() : null);
                })).setHeader(ViewHelper.createExplainedHeader("Net Shares Δ", "Net change in total shares held by institutions."))
                .setWidth("150px").setFlexGrow(0).setSortable(true);
    }

    private void addBreadthAndSentimentColumns() {
        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return flow != null && flow.totalHoldersCount() != null ? flow.totalHoldersCount().toString() : "-";
                }).setHeader(ViewHelper.createExplainedHeader("Holders", "Total number of institutional managers reporting a position."))
                .setWidth("125px").setFlexGrow(0).setSortable(true);

        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return flow != null && flow.putCallRatio() != null ? String.format(Locale.US, "%.2f", flow.putCallRatio()) : "-";
                }).setHeader(ViewHelper.createExplainedHeader("P/C Ratio", "Put to Call option dollar value ratio."))
                .setWidth("140px").setFlexGrow(0).setSortable(true);

        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    return flow != null && flow.top5HoldersPct() != null ? String.format(Locale.US, "%.1f%%", flow.top5HoldersPct()) : "-";
                }).setHeader(ViewHelper.createExplainedHeader("Top 5 Conc.", "Percentage of institutional value owned by Top 5 managers."))
                .setWidth("150px").setFlexGrow(0).setSortable(true);
    }

    private void addBuyersAndDetailsColumns() {
        grid.addColumn(item -> {
                    var flow = currentFlowMap.get(item.companyId());
                    String formatted = flow != null ? ViewHelper.formatFundNames(flow.tier1BuyerNames()) : null;
                    return formatted != null ? formatted : "-";
                }).setHeader(ViewHelper.createExplainedHeader("Smart Money Buyers", "Tier 1 elite funds that accumulated shares in this quarter."))
                .setAutoWidth(true).setFlexGrow(1);

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
    }

    public Set<CompanyOverviewDto> getSelectedItems() {
        return grid.getSelectedItems();
    }

    public void deselectAll() {
        grid.deselectAll();
    }

    private static class MarketFilterState {
        String ticker = "";
        String companyName = "";
        String sector = "";

        boolean test(CompanyOverviewDto item) {
            return ViewHelper.matches(item.ticker(), ticker)
                    && ViewHelper.matches(item.companyName(), companyName)
                    && ViewHelper.matches(item.sectorName(), sector);
        }
    }
}
