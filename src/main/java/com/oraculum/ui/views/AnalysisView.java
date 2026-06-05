package com.oraculum.ui.views;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.api.dto.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.MarketDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.service.AnalysisRequestService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Route(value = "analysis", layout = MainLayout.class)
@PageTitle("Analysis | Oraculum")
public class AnalysisView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final CompanyAnalysisApi companyAnalysisApi;
    private final AnalysisRequestService analysisRequestService;

    private ComboBox<CompanyDto> companyComboBox;
    private Grid<CompanyAnalysisDto> grid;

    public AnalysisView(CompanyApi companyApi, CompanyAnalysisApi companyAnalysisApi,
                        AnalysisRequestService analysisRequestService) {
        this.companyApi = companyApi;
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisRequestService = analysisRequestService;

        setSizeFull();
        addClassNames(LumoUtility.Padding.MEDIUM);

        SplitLayout splitLayout = new SplitLayout(createTriggerCard(), createHistoryGrid());
        splitLayout.setSplitterPosition(30);
        splitLayout.setSizeFull();

        add(splitLayout);
    }

    private Component createTriggerCard() {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(false);
        card.setSpacing(false);

        H3 title = new H3("Run New Analysis");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);

        ComboBox<MarketDto> marketComboBox = new ComboBox<>("Market");
        marketComboBox.setItems(companyApi.getAllMarkets());
        marketComboBox.setItemLabelGenerator(MarketDto::marketName);

        companyComboBox = new ComboBox<>("Company");
        companyComboBox.setEnabled(false);
        companyComboBox.setItemLabelGenerator(c -> String.format("%s - %s", c.ticker(), c.companyName()));

        marketComboBox.addValueChangeListener(e -> {
            if (e.getValue() == null) {
                companyComboBox.setItems(Collections.emptyList());
                companyComboBox.setEnabled(false);
            } else {
                List<CompanyDto> companies = companyApi.getCompaniesByMarket(e.getValue().marketId());
                companyComboBox.setItems(companies);
                companyComboBox.setEnabled(true);
            }
        });

        ComboBox<StatementVariant> variantComboBox = new ComboBox<>("Statement Variant");
        variantComboBox.setItems(StatementVariant.values());
        variantComboBox.setValue(StatementVariant.TTM);

        Button analyzeButton = new Button("Analyze");
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.addClickListener(_ -> triggerAnalysis(companyComboBox.getValue(), variantComboBox.getValue()));

        FormLayout formLayout = new FormLayout(marketComboBox, companyComboBox, variantComboBox);
        card.add(title, formLayout, analyzeButton);
        return card;
    }

    private Component createHistoryGrid() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();

        grid = new Grid<>(CompanyAnalysisDto.class, false);
        grid.addColumn(CompanyAnalysisDto::getTicker).setHeader("Ticker");
        grid.addColumn(CompanyAnalysisDto::getMarket).setHeader("Market");
        grid.addColumn(new ComponentRenderer<>(analysis -> {
            AnalysisStatus status = analysis.getStatus();
            String theme = "badge";
            switch (status) {
                case COMPLETED -> theme += " success";
                case FAILED -> theme += " error";
            }
            Div statusBadge = new Div();
            statusBadge.setText(status.name());
            statusBadge.getElement().getThemeList().add(theme);
            return statusBadge;
        })).setHeader("Status");
        grid.addColumn(CompanyAnalysisDto::getConviction).setHeader("Conviction");
        grid.addColumn(CompanyAnalysisDto::getOutlook).setHeader("Outlook");
        grid.addColumn(CompanyAnalysisDto::getRecommendation).setHeader("Recommendation");
        grid.addColumn(CompanyAnalysisDto::getAnalysisDate).setHeader("Analysis Date");

        grid.setItems(q -> companyAnalysisApi.getCompanyAnalysisList(PageRequest.of(q.getPage(), q.getPageSize()))
                .stream(), q -> (int) companyAnalysisApi.getAnalysisCount());

        layout.add(grid);
        return layout;
    }

    private void triggerAnalysis(CompanyDto company, StatementVariant variant) {
        if (company == null || variant == null) {
            showError("Please select a company and a variant.");
            return;
        }

        try {
            CompanyAnalysisRequest request = new CompanyAnalysisRequest(UUID.randomUUID(),
                    company.id(),
                    company.ticker(),
                    company.market(),
                    LocalDate.now(),
                    variant);
            analysisRequestService.requestAnalysis(request);
            showSuccess("Analysis triggered for " + company.ticker());
            grid.getDataProvider().refreshAll();
        } catch (Exception e) {
            showError("Failed to trigger analysis: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
