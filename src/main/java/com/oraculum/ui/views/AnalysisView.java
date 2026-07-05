package com.oraculum.ui.views;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.company.api.*;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.api.CompanyAnalysisProgressBroadcasterService;
import com.oraculum.ui.service.AnalysisRequestService;
import com.oraculum.ui.views.components.AnalysisResultRenderer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;

@Route(value = "analysis", layout = MainLayout.class)
@PageTitle("Analysis | Oraculum")
@PermitAll
public class AnalysisView extends VerticalLayout {

    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final CompanyAnalysisApi companyAnalysisApi;
    private final AnalysisRequestService analysisRequestService;
    private final ObjectMapper objectMapper;
    private final CompanyAnalysisProgressBroadcasterService broadcaster;
    private final AnalysisResultRenderer analysisResultRenderer;
    private ComboBox<CompanyDto> companyComboBox;
    private TextArea analysisFocusInput;
    private Grid<CompanyAnalysisDto> grid;
    private java.util.List<CompanyAnalysisDto> gridData;

    public AnalysisView(CompanyMetadataApi companyMetadataApi,
                        CompanyFinancialDataApi companyFinancialDataApi,
                        CompanySharePriceApi companySharePriceApi,
                        CompanyNewsApi companyNewsApi,
                        CompanyInsiderTransactionApi companyInsiderTransactionApi,
                        CompanyAnalysisApi companyAnalysisApi,
                        AnalysisRequestService analysisRequestService,
                        ObjectMapper objectMapper,
                        CompanyAnalysisProgressBroadcasterService broadcaster,
                        AnalysisResultRenderer analysisResultRenderer) {
        this.companyMetadataApi = companyMetadataApi;
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisRequestService = analysisRequestService;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.analysisResultRenderer = analysisResultRenderer;

        setSizeFull();
        getStyle().set("padding-bottom", "2rem");
        setPadding(true);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(createTriggerToolbar());
        content.add(createHistoryGrid());
        content.setFlexGrow(1, content.getComponentAt(1));

        add(content);
        setFlexGrow(1, content);
    }

    private Component createTriggerToolbar() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.getStyle().set("margin-bottom", "1rem");

        H3 title = new H3("Run New Analysis");
        title.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.TextColor.SECONDARY);
        title.getStyle().set("margin-bottom", "1rem");
        title.getStyle().set("margin-top", "2rem");

        companyComboBox = new ComboBox<>();
        companyComboBox.setPlaceholder("Select company...");
        companyComboBox.setItemLabelGenerator(c -> String.format("%s - %s", c.ticker(), c.companyName()));
        companyComboBox.setWidth("400px");
        companyComboBox.setClearButtonVisible(true);
        companyComboBox.setItems(companyMetadataApi.getAllCompanies());

        analysisFocusInput = new TextArea("Custom Instructions / Focus");
        analysisFocusInput.setPlaceholder("Optional: e.g., Focus on AI revenue impact and margin trends...");
        analysisFocusInput.setWidth("400px");

        HorizontalLayout leftGroup = new HorizontalLayout(companyComboBox, analysisFocusInput);
        leftGroup.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        leftGroup.setSpacing(true);

        Button analyzeButton = new Button("Analyze", VaadinIcon.PLAY.create());
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.addClickListener(_ -> triggerAnalysis());

        HorizontalLayout row = new HorizontalLayout(leftGroup, analyzeButton);
        row.setWidthFull();
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        wrapper.add(title, row);
        return wrapper;
    }

    private void triggerAnalysis() {
        CompanyDto company = companyComboBox.getValue();
        if (company == null) {
            ViewHelper.showError("Please select a company.");
            return;
        }
        try {
            UUID correlationId = UUID.randomUUID();

            CompanyAnalysisDto transientDto = new CompanyAnalysisDto();
            transientDto.setId(correlationId);
            transientDto.setCompanyId(company.id());
            transientDto.setTicker(company.ticker());
            transientDto.setMarket(company.market());
            transientDto.setStatus(AnalysisStatus.QUEUED);
            transientDto.setAnalysisDate(LocalDate.now());

            gridData.addFirst(transientDto);
            grid.getDataProvider().refreshAll();

            CompanyAnalysisRequest requestDto = new CompanyAnalysisRequest(correlationId, company.id(), company.ticker(), company.market(), LocalDate.now(), analysisFocusInput.getValue());
            analysisRequestService.requestAnalysis(requestDto);
            ViewHelper.showSuccess("Analysis triggered for " + company.ticker());
        } catch (Exception e) {
            ViewHelper.showError("Failed to trigger analysis: " + e.getMessage());
        }
    }

    private Component createHistoryGrid() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();

        H3 title = new H3("Analysis History");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        title.getStyle().set("margin-bottom", "1rem");
        title.getStyle().set("margin-top", "1rem");
        grid = buildGrid();

        List<CompanyAnalysisDto> data = companyAnalysisApi.getCompanyAnalysisList(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        gridData = new java.util.ArrayList<>(data);
        GridListDataView<CompanyAnalysisDto> dataView = grid.setItems(gridData);
        setupFilters(dataView);

        layout.add(title, grid);
        layout.expand(grid);

        return ViewHelper.wrapInCard(layout);
    }

    private Grid<CompanyAnalysisDto> buildGrid() {
        Grid<CompanyAnalysisDto> g = new Grid<>(CompanyAnalysisDto.class, false);
        g.setSizeFull();
        g.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        g.addClassName("screener-grid");

        g.addColumn(CompanyAnalysisDto::getTicker).setHeader("Ticker").setKey("ticker").setSortable(true);
        g.addColumn(CompanyAnalysisDto::getMarket).setHeader("Market").setKey("market").setSortable(true);

        g.addColumn(new ComponentRenderer<>(a -> {
                    if (a.getStatus() == AnalysisStatus.QUEUED || a.getStatus() == AnalysisStatus.RUNNING) {
                        return new ProgressCell(a, broadcaster, this::refreshGridData);
                    }
                    return ViewHelper.statusBadge(a.getStatus());
                })).setHeader("Status").setKey("status")
                .setComparator(Comparator.comparing(CompanyAnalysisDto::getStatus, Comparator.nullsLast(Comparator.naturalOrder())))
                .setSortable(true)
                .setAutoWidth(true);

        g.addColumn(CompanyAnalysisDto::getConviction).setHeader("Conviction").setSortable(true);

        g.addColumn(new ComponentRenderer<>(a -> ViewHelper.outlookBadge(a.getOutlook())))
                .setHeader("Outlook").setKey("outlook")
                .setComparator(Comparator.comparing(CompanyAnalysisDto::getOutlook, Comparator.nullsLast(Comparator.naturalOrder())))
                .setSortable(true);

        g.addColumn(new ComponentRenderer<>(a -> ViewHelper.recommendationBadge(a.getRecommendation())))
                .setHeader("Recommendation").setKey("recommendation")
                .setComparator(Comparator.comparing(CompanyAnalysisDto::getRecommendation, Comparator.nullsLast(Comparator.naturalOrder())))
                .setSortable(true);

        g.addColumn(CompanyAnalysisDto::getAnalysisDate).setHeader("Analysis Date").setSortable(true);

        g.addColumn(new ComponentRenderer<>(a -> {
            Button reportBtn = new Button(VaadinIcon.EYE.create());
            reportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            reportBtn.setAriaLabel("View report");
            reportBtn.setTooltipText("View report");
            reportBtn.addClickListener(_ -> showAnalysisDetails(a));

            Button companyBtn = ViewHelper.createCompanyDetailsButton(companyMetadataApi, companyFinancialDataApi, companySharePriceApi, companyNewsApi, companyInsiderTransactionApi, objectMapper, a.getCompanyId(), false);

            HorizontalLayout actions = new HorizontalLayout(reportBtn, companyBtn);
            actions.setSpacing(true);
            return actions;
        })).setHeader("Actions");

        g.addItemDoubleClickListener(event -> showAnalysisDetails(event.getItem()));
        return g;
    }

    private void setupFilters(GridListDataView<CompanyAnalysisDto> dataView) {
        HeaderRow filterRow = grid.appendHeaderRow();
        AnalysisFilter filter = new AnalysisFilter();
        dataView.setFilter(filter::test);

        ViewHelper.addFilter(grid, filterRow, "ticker", "Ticker", v -> {
            filter.ticker = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "market", "Market", v -> {
            filter.market = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "status", "Status", v -> {
            filter.status = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "outlook", "Outlook", v -> {
            filter.outlook = v;
            dataView.refreshAll();
        });
        ViewHelper.addFilter(grid, filterRow, "recommendation", "Rec.", v -> {
            filter.recommendation = v;
            dataView.refreshAll();
        });
    }

    private void showAnalysisDetails(CompanyAnalysisDto analysis) {
        Dialog dialog = new Dialog();
        dialog.setWidth("85vw");
        dialog.setHeight("85vh");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        dialog.add(analysisResultRenderer.renderAnalysisTabs(analysis));

        Button closeButton = new Button("Close", _ -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void refreshGridData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            List<CompanyAnalysisDto> latest = companyAnalysisApi.getCompanyAnalysisList(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
            gridData.clear();
            gridData.addAll(latest);
            grid.getDataProvider().refreshAll();
        }));
    }

    private static class AnalysisFilter {
        String ticker, market, status, outlook, recommendation;

        boolean test(CompanyAnalysisDto a) {
            return ViewHelper.matches(a.getTicker(), ticker)
                    && ViewHelper.matches(a.getMarket(), market)
                    && ViewHelper.matches(a.getStatus() != null ? a.getStatus().getDisplayName() : "Pending", status)
                    && ViewHelper.matches(a.getOutlook() != null ? a.getOutlook().getDisplayName() : "Pending", outlook)
                    && ViewHelper.matches(a.getRecommendation() != null ? a.getRecommendation().getDisplayName() : "Pending", recommendation);
        }
    }

    private static class ProgressCell extends Span {
        private final Runnable unregister;

        public ProgressCell(CompanyAnalysisDto analysis, CompanyAnalysisProgressBroadcasterService broadcaster, Runnable onComplete) {
            getElement().getThemeList().add("badge");
            if (analysis.getStatus() == AnalysisStatus.QUEUED) {
                getElement().getThemeList().add("contrast");
            } else {
                getElement().getThemeList().add("warning");
            }

            getStyle().set("display", "inline-flex");
            getStyle().set("align-items", "center");
            getStyle().set("gap", "8px");

            com.vaadin.flow.component.progressbar.ProgressBar progressBar = new com.vaadin.flow.component.progressbar.ProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setWidth("40px");
            progressBar.getStyle().set("margin", "0");

            Span label = new Span(analysis.getStatus() == AnalysisStatus.QUEUED ? "Queued..." : "Running...");

            add(progressBar, label);

            unregister = broadcaster.register(update -> {
                if (!update.analysisId().equals(analysis.getId())) {
                    return;
                }
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (update.isDone()) {
                        onComplete.run();
                    } else {
                        label.setText("Running: " + update.agentType().getAgentName());
                        getElement().getThemeList().remove("contrast");
                        getElement().getThemeList().add("warning");
                        analysis.setStatus(AnalysisStatus.RUNNING);
                    }
                }));
            });

            addDetachListener(_ -> unregister.run());
        }
    }


}
