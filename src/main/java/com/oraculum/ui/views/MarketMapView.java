package com.oraculum.ui.views;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.common.constant.MarketConstants;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.CompanyScreenerApi;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.components.MarketGridComponent;
import com.oraculum.ui.components.MarketTreemapComponent;
import com.oraculum.ui.factory.CompanyDetailsButtonFactory;
import com.oraculum.ui.service.AnalysisRequestService;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(value = "market-map", layout = MainLayout.class)
@PageTitle("Market Map")
@PermitAll
public class MarketMapView extends VerticalLayout {

    private final CompanyScreenerApi companyScreenerApi;
    private final AnalysisRequestService analysisRequestService;
    private final CompanyDetailsButtonFactory companyDetailsButtonFactory;
    private final CompanyInstitutionalFlowApi companyInstitutionalFlowApi;

    private final MarketTreemapComponent marketTreemap = new MarketTreemapComponent();
    private MarketGridComponent marketGrid;

    public MarketMapView(CompanyScreenerApi companyScreenerApi,
                         AnalysisRequestService analysisRequestService,
                         CompanyDetailsButtonFactory companyDetailsButtonFactory,
                         CompanyInstitutionalFlowApi companyInstitutionalFlowApi) {
        this.companyScreenerApi = companyScreenerApi;
        this.analysisRequestService = analysisRequestService;
        this.companyDetailsButtonFactory = companyDetailsButtonFactory;
        this.companyInstitutionalFlowApi = companyInstitutionalFlowApi;

        setPadding(true);
        setSpacing(false);
        setSizeFull();

        initLayout();
    }

    private void initLayout() {
        this.marketGrid = new MarketGridComponent(companyDetailsButtonFactory);

        add(createTitle());

        List<CompanyOverviewDto> companies = companyScreenerApi.getCompanyOverview();
        Map<Integer, CompanyInstitutionalFlowDto> latestFlowMap = companyInstitutionalFlowApi.getLatestInstitutionalFlowByCompanyId();

        add(createDescription(companies));

        Tab treemapTab = new Tab(VaadinIcon.CHART_GRID.create(), new Span("Treemap View"));
        Tab tableTab = new Tab(VaadinIcon.TABLE.create(), new Span("Table View"));
        Tabs tabs = new Tabs(treemapTab, tableTab);

        ComboBox<String> marketSelect = createMarketSelect(companies);
        ComboBox<String> metricSelect = createMetricSelect();

        HorizontalLayout filtersLayout = new HorizontalLayout(metricSelect, marketSelect);
        filtersLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout controlsLayout = new HorizontalLayout(tabs, filtersLayout);
        controlsLayout.setWidthFull();
        controlsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        controlsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);

        Consumer<Tab> updateView = selectedTab -> {
            contentArea.removeAll();
            List<CompanyOverviewDto> filtered = filterByMarket(companies, marketSelect.getValue());

            metricSelect.setVisible(selectedTab.equals(treemapTab));

            if (selectedTab.equals(treemapTab)) {
                marketTreemap.updateData(filtered, latestFlowMap, metricSelect.getValue());
                contentArea.add(marketTreemap);
            } else {
                marketGrid.updateData(filtered, latestFlowMap);
                contentArea.add(createGridToolbar(marketGrid), marketGrid);
            }
        };

        tabs.addSelectedChangeListener(e -> updateView.accept(e.getSelectedTab()));
        marketSelect.addValueChangeListener(_ -> updateView.accept(tabs.getSelectedTab()));
        metricSelect.addValueChangeListener(_ -> updateView.accept(tabs.getSelectedTab()));

        updateView.accept(treemapTab);

        add(controlsLayout, ViewHelper.wrapInCard(contentArea));
        setFlexGrow(1, contentArea);

        attachClientClickListener();
    }

    private H3 createTitle() {
        H3 title = new H3("Market Map");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem");
        return title;
    }

    private Paragraph createDescription(List<CompanyOverviewDto> companies) {
        LocalDate maxDate = companies != null ? companies.stream()
                .map(CompanyOverviewDto::tradeDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null) : null;
        String asOfText = maxDate != null ? " (Price data as of " + maxDate + ")" : "";
        Paragraph p = new Paragraph("Interactive treemap and sortable table view visualizing market capitalization, sector division, valuation, and price growth trends." + asOfText);
        p.addClassNames(LumoUtility.TextColor.SECONDARY);
        return p;
    }

    private ComboBox<String> createMarketSelect(List<CompanyOverviewDto> companies) {
        List<String> markets = getAvailableMarkets(companies);
        String defaultMarket = markets.contains(MarketConstants.US) ? MarketConstants.US : (markets.isEmpty() ? MarketConstants.US : markets.getFirst());
        ComboBox<String> select = new ComboBox<>();
        select.setPlaceholder("Market");
        select.setItems(markets);
        select.setValue(defaultMarket);
        select.setClearButtonVisible(false);
        return select;
    }

    private ComboBox<String> createMetricSelect() {
        ComboBox<String> select = new ComboBox<>();
        select.setPlaceholder("Price Metric");
        select.setItems("Price: 1-Day Change", "Price: 1-Week Change", "Price: 1-Month Change");
        select.setValue("Price: 1-Day Change");
        select.setClearButtonVisible(false);
        return select;
    }

    private HorizontalLayout createGridToolbar(MarketGridComponent grid) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.addClassNames(LumoUtility.Padding.Bottom.SMALL);

        Button runAnalysisBtn = new Button("Run Analysis", VaadinIcon.PLAY.create());
        runAnalysisBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runAnalysisBtn.addClickListener(_ -> triggerAnalysis(grid.getSelectedItems(), grid));

        toolbar.add(runAnalysisBtn);
        toolbar.getStyle().set("margin-top", "1rem");
        toolbar.getStyle().set("margin-bottom", "0.5rem");
        return toolbar;
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

    private void triggerAnalysis(Set<CompanyOverviewDto> sel, MarketGridComponent grid) {
        if (!validateBatchSelection(sel.size())) return;
        for (CompanyOverviewDto item : sel) {
            CompanyAnalysisRequest req = new CompanyAnalysisRequest(UUID.randomUUID(), item.companyId(), new TickerKeyDto(item.ticker(), item.market()), LocalDate.now(), null);
            analysisRequestService.requestAnalysis(req);
        }
        ViewHelper.showSuccess("Triggered analysis for " + sel.size() + " companies.");
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
        companyDetailsButtonFactory.openCompanyDetailsDialog(companyId);
    }

    private List<String> getAvailableMarkets(List<CompanyOverviewDto> companies) {
        if (companies == null) return List.of(MarketConstants.US);
        List<String> distinct = companies.stream()
                .map(CompanyOverviewDto::market)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (distinct.contains(MarketConstants.US)) {
            distinct.remove(MarketConstants.US);
            distinct.addFirst(MarketConstants.US);
        }
        return distinct.isEmpty() ? List.of(MarketConstants.US) : distinct;
    }

    private List<CompanyOverviewDto> filterByMarket(List<CompanyOverviewDto> companies, String market) {
        if (companies == null) return List.of();
        if (market == null || market.isBlank()) return companies;
        return companies.stream()
                .filter(c -> c.market() != null && c.market().equalsIgnoreCase(market))
                .toList();
    }
}
