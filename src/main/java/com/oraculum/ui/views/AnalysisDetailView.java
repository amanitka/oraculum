package com.oraculum.ui.views;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.factory.CompanyDetailsButtonFactory;
import com.oraculum.ui.views.components.AnalysisResultRenderer;
import com.oraculum.ui.views.components.export.AnalysisPdfExportService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Route(value = "analysis/detail", layout = MainLayout.class)
@PageTitle("Analysis Detail | Oraculum")
@PermitAll
public class AnalysisDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CompanyAnalysisApi companyAnalysisApi;
    private final AnalysisResultRenderer analysisResultRenderer;
    private final CompanyDetailsButtonFactory companyDetailsButtonFactory;
    private final AnalysisPdfExportService analysisPdfExportService;

    public AnalysisDetailView(CompanyAnalysisApi companyAnalysisApi,
                              AnalysisResultRenderer analysisResultRenderer,
                              CompanyDetailsButtonFactory companyDetailsButtonFactory,
                              AnalysisPdfExportService analysisPdfExportService) {
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisResultRenderer = analysisResultRenderer;
        this.companyDetailsButtonFactory = companyDetailsButtonFactory;
        this.analysisPdfExportService = analysisPdfExportService;

        setWidthFull();
        getStyle().set("padding-top", "1rem").set("padding-bottom", "2rem").set("overflow-x", "hidden").set("box-sizing", "border-box");
        setPadding(true);
        setSpacing(false);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        removeAll();

        if (parameter == null || parameter.isBlank()) {
            showNotFound("No analysis ID specified.");
            return;
        }

        try {
            UUID id = UUID.fromString(parameter);
            Optional<CompanyAnalysisViewDto> analysisOpt = companyAnalysisApi.getAnalysisById(id);

            if (analysisOpt.isEmpty()) {
                showNotFound("Analysis not found for ID: " + parameter);
                return;
            }

            renderView(analysisOpt.get());
        } catch (IllegalArgumentException e) {
            showNotFound("Invalid analysis ID format: " + parameter);
        }
    }

    private void renderView(CompanyAnalysisViewDto analysis) {
        add(createTopBar(analysis));

        var resultComponent = analysisResultRenderer.renderAnalysisResult(analysis);
        resultComponent.getElement().getStyle().set("padding-top", "0.75rem");
        add(resultComponent);
    }

    private HorizontalLayout createTopBar(CompanyAnalysisViewDto analysis) {
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.addClassNames(LumoUtility.Padding.Vertical.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
        topBar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        topBar.add(createLeftHeaderGroup(analysis), createRightHeaderGroup(analysis));
        return topBar;
    }

    private HorizontalLayout createLeftHeaderGroup(CompanyAnalysisViewDto analysis) {
        Button backButton = new Button("Back to Analyses", VaadinIcon.ARROW_LEFT.create(), _ -> UI.getCurrent().navigate(AnalysisView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        String titleText = analysis.getCompanyName() != null && !analysis.getCompanyName().isBlank()
                ? analysis.getCompanyName() + " (" + analysis.getTicker() + ") Financial Analysis Report"
                : analysis.getTicker() + " Financial Analysis Report";

        H3 title = new H3(titleText);
        title.getStyle().set("margin", "0").set("font-size", "1.25rem");

        HorizontalLayout leftGroup = new HorizontalLayout(backButton, title);
        leftGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        leftGroup.getStyle().set("gap", "16px");
        return leftGroup;
    }

    private HorizontalLayout createRightHeaderGroup(CompanyAnalysisViewDto analysis) {
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.getStyle().set("gap", "12px");

        if (canExportPdf(analysis)) {
            rightGroup.add(createExportPdfAnchor(analysis));
        }

        Button companyOverviewBtn = companyDetailsButtonFactory.createButton(analysis.getCompanyId(), true);
        companyOverviewBtn.setText("Company Overview");
        rightGroup.add(companyOverviewBtn);

        if (analysis.getAnalysisResult() != null && analysis.getAnalysisResult().valuation() != null) {
            rightGroup.add(ViewHelper.valuationBadge(analysis.getAnalysisResult().valuation()));
        }
        return rightGroup;
    }

    private boolean canExportPdf(CompanyAnalysisViewDto analysis) {
        return analysis.getError() == null && analysis.getAnalysisResult() != null;
    }

    private Anchor createExportPdfAnchor(CompanyAnalysisViewDto analysis) {
        Anchor downloadAnchor = new Anchor((DownloadEvent event) -> writePdfDownload(event, analysis), "");
        downloadAnchor.getElement().setAttribute("download", true);

        Button exportButton = new Button("Export PDF", VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        downloadAnchor.add(exportButton);
        return downloadAnchor;
    }

    private void writePdfDownload(DownloadEvent event, CompanyAnalysisViewDto analysis) {
        byte[] pdfBytes;
        try {
            pdfBytes = analysisPdfExportService.exportPdf(analysis);
        } catch (RuntimeException e) {
            log.error("Failed to export analysis {} to PDF", analysis.getId(), e);
            event.getResponse().setStatus(500);
            return;
        }
        event.setFileName(analysisPdfExportService.buildFileName(analysis));
        event.setContentType("application/pdf");
        event.setContentLength(pdfBytes.length);
        try (OutputStream out = event.getOutputStream()) {
            out.write(pdfBytes);
        } catch (IOException e) {
            log.error("Failed to stream PDF for analysis {}", analysis.getId(), e);
        }
    }

    private void showNotFound(String message) {
        Button backButton = new Button("Back to Analyses", VaadinIcon.ARROW_LEFT.create(), _ -> UI.getCurrent().navigate(AnalysisView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Span errorSpan = new Span(message);
        errorSpan.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        VerticalLayout layout = new VerticalLayout(backButton, errorSpan);
        layout.setPadding(true);
        layout.setSpacing(true);
        add(layout);
    }
}
