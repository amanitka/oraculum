package com.oraculum.ui.views;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.views.components.AnalysisResultRenderer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;
import java.util.UUID;

@Route(value = "analysis/detail", layout = MainLayout.class)
@PageTitle("Analysis Detail | Oraculum")
@PermitAll
public class AnalysisDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CompanyAnalysisApi companyAnalysisApi;
    private final AnalysisResultRenderer analysisResultRenderer;

    public AnalysisDetailView(CompanyAnalysisApi companyAnalysisApi,
                              AnalysisResultRenderer analysisResultRenderer) {
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisResultRenderer = analysisResultRenderer;

        setWidthFull();
        getStyle().set("padding-top", "2rem").set("padding-bottom", "2rem").set("overflow-x", "hidden").set("box-sizing", "border-box");
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
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.addClassNames(LumoUtility.Padding.Vertical.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
        topBar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Button backButton = new Button("Back to Analyses", VaadinIcon.ARROW_LEFT.create(), _ -> UI.getCurrent().navigate(AnalysisView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        String titleText = analysis.getCompanyName() != null && !analysis.getCompanyName().isBlank()
                ? analysis.getCompanyName() + " (" + analysis.getTicker() + ") Financial Analysis Report"
                : analysis.getTicker() + " Financial Analysis Report";

        H3 title = new H3(titleText);
        title.getStyle().set("margin", "0").set("font-size", "1.25rem");

        HorizontalLayout badges = new HorizontalLayout();
        badges.setAlignItems(FlexComponent.Alignment.CENTER);

        if (analysis.getAnalysisResult() != null && analysis.getAnalysisResult().valuation() != null) {
            badges.add(ViewHelper.valuationBadge(analysis.getAnalysisResult().valuation()));
        }

        HorizontalLayout leftGroup = new HorizontalLayout(backButton, title);
        leftGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        leftGroup.getStyle().set("gap", "16px");

        topBar.add(leftGroup, badges);
        add(topBar);

        var resultComponent = analysisResultRenderer.renderAnalysisResult(analysis);
        resultComponent.getElement().getStyle().set("padding-top", "2rem");
        add(resultComponent);
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
