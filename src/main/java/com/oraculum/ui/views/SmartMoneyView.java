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
import com.oraculum.ui.components.InstitutionalFlowGridComponent;
import com.oraculum.ui.components.InstitutionalFlowTreemapComponent;
import com.oraculum.ui.components.QuarterStepperComponent;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Route(value = "smart-money", layout = MainLayout.class)
@PageTitle("Smart Money")
@PermitAll
public class SmartMoneyView extends VerticalLayout {

    private final CompanyScreenerApi companyScreenerApi;
    private final AnalysisRequestService analysisRequestService;
    private final CompanyDetailsButtonFactory companyDetailsButtonFactory;
    private final CompanyInstitutionalFlowApi companyInstitutionalFlowApi;

    private final InstitutionalFlowTreemapComponent smartMoneyTreemap = new InstitutionalFlowTreemapComponent();
    private InstitutionalFlowGridComponent smartMoneyGrid;

    public SmartMoneyView(CompanyScreenerApi companyScreenerApi,
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
        this.smartMoneyGrid = new InstitutionalFlowGridComponent(companyDetailsButtonFactory);

        add(createTitle());

        List<CompanyOverviewDto> usCompanies = companyScreenerApi.getCompanyOverview().stream()
                .filter(c -> MarketConstants.US.equalsIgnoreCase(c.market()))
                .toList();
        List<LocalDate> availablePeriods = companyInstitutionalFlowApi.getAvailableReportPeriods();
        LocalDate defaultPeriod = availablePeriods.isEmpty() ? null : availablePeriods.getFirst();

        add(createDescription());

        Tab treemapTab = new Tab(VaadinIcon.MONEY_EXCHANGE.create(), new Span("13F Flow Map"));
        Tab tableTab = new Tab(VaadinIcon.RECORDS.create(), new Span("Details Table"));
        Tabs tabs = new Tabs(treemapTab, tableTab);

        ComboBox<String> metricSelect = createFlowMetricSelect();

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);

        QuarterStepperComponent[] stepperHolder = new QuarterStepperComponent[1];

        Consumer<Tab> updateView = selectedTab -> {
            contentArea.removeAll();
            LocalDate period = stepperHolder[0] != null ? stepperHolder[0].getSelectedPeriod() : defaultPeriod;
            Map<Integer, CompanyInstitutionalFlowDto> flowMap = companyInstitutionalFlowApi.getInstitutionalFlowByPeriod(period);

            metricSelect.setVisible(selectedTab.equals(treemapTab));

            if (selectedTab.equals(treemapTab)) {
                smartMoneyTreemap.updateData(usCompanies, flowMap, metricSelect.getValue());
                contentArea.add(smartMoneyTreemap);
            } else {
                smartMoneyGrid.updateData(usCompanies, flowMap);
                contentArea.add(createToolbar(smartMoneyGrid), smartMoneyGrid);
            }
        };

        QuarterStepperComponent stepper = new QuarterStepperComponent(
                availablePeriods,
                defaultPeriod,
                _ -> updateView.accept(tabs.getSelectedTab())
        );
        stepperHolder[0] = stepper;

        HorizontalLayout filtersLayout = new HorizontalLayout(stepper, metricSelect);
        filtersLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        filtersLayout.getStyle().set("gap", "16px");

        HorizontalLayout controlsLayout = new HorizontalLayout(tabs, filtersLayout);
        controlsLayout.setWidthFull();
        controlsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        controlsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        tabs.addSelectedChangeListener(e -> updateView.accept(e.getSelectedTab()));
        metricSelect.addValueChangeListener(_ -> updateView.accept(tabs.getSelectedTab()));

        updateView.accept(treemapTab);

        add(controlsLayout, ViewHelper.wrapInCard(contentArea));
        setFlexGrow(1, contentArea);

        attachClientClickListener();
    }

    private H3 createTitle() {
        H3 title = new H3("Smart Money (13F Institutional Flow)");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);
        title.getStyle().set("margin-top", "2rem");
        return title;
    }

    private Paragraph createDescription() {
        Paragraph p = new Paragraph("Track where institutional capital, hedge funds, and Tier 1 superinvestors are allocating or trimming positions quarter-over-quarter.");
        p.addClassNames(LumoUtility.TextColor.SECONDARY);
        return p;
    }

    private ComboBox<String> createFlowMetricSelect() {
        ComboBox<String> select = new ComboBox<>();
        select.setPlaceholder("Color Metric");
        select.setItems("Institutional Net Flow %", "Put / Call Sentiment", "Smart Money (Tier 1 Flow)");
        select.setValue("Institutional Net Flow %");
        select.setWidth("240px");
        select.setClearButtonVisible(false);
        return select;
    }

    private HorizontalLayout createToolbar(InstitutionalFlowGridComponent grid) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.addClassNames(LumoUtility.Padding.Bottom.SMALL);

        Button runAnalysisBtn = new Button("Run Analysis", VaadinIcon.PLAY.create());
        runAnalysisBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runAnalysisBtn.addClickListener(_ -> {
            Set<CompanyOverviewDto> sel = grid.getSelectedItems();
            if (!validateBatchSelection(sel.size())) return;
            for (CompanyOverviewDto item : sel) {
                CompanyAnalysisRequest req = new CompanyAnalysisRequest(UUID.randomUUID(), item.companyId(), new TickerKeyDto(item.ticker(), item.market()), LocalDate.now(), null);
                analysisRequestService.requestAnalysis(req);
            }
            ViewHelper.showSuccess("Triggered analysis for " + sel.size() + " companies.");
            grid.deselectAll();
        });

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
}
