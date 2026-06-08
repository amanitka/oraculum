package com.oraculum.ui.views;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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

    public AnalysisView(CompanyApi companyApi, CompanyAnalysisApi companyAnalysisApi, AnalysisRequestService analysisRequestService) {
        this.companyApi = companyApi;
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisRequestService = analysisRequestService;

        setSizeFull();
        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        Component triggerCard = createTriggerCard();
        Component historyGrid = createHistoryGrid();

        add(triggerCard, historyGrid);
        setFlexGrow(0, triggerCard);
        expand(historyGrid);
    }

    private Component createTriggerCard() {
        Div card = new Div();
        card.addClassNames(LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.MEDIUM);
        card.setWidthFull();
        card.setMaxWidth("1000px");

        H3 title = new H3("Run New Analysis");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);

        ComboBox<MarketDto> marketComboBox = new ComboBox<>("Market");
        marketComboBox.setItems(companyApi.getAllMarkets());
        marketComboBox.setItemLabelGenerator(MarketDto::marketName);
        marketComboBox.setWidth("200px");

        companyComboBox = new ComboBox<>("Company");
        companyComboBox.setEnabled(false);
        companyComboBox.setItemLabelGenerator(c -> String.format("%s - %s", c.ticker(), c.companyName()));
        companyComboBox.setWidth("300px");

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
        variantComboBox.setPlaceholder("Auto (Planner Decides)");
        variantComboBox.setClearButtonVisible(true);
        variantComboBox.setWidthFull();

        Details advancedDetails = new Details("Advanced Options", variantComboBox);
        advancedDetails.setOpened(false);
        advancedDetails.addClassNames(LumoUtility.Margin.Bottom.NONE);

        Button analyzeButton = new Button("Analyze", VaadinIcon.PLAY.create());
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.addClickListener(_ -> triggerAnalysis(companyComboBox.getValue(), variantComboBox.getValue()));

        HorizontalLayout rightGroup = new HorizontalLayout(advancedDetails, analyzeButton);
        rightGroup.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        rightGroup.setSpacing(true);

        HorizontalLayout leftGroup = new HorizontalLayout(marketComboBox, companyComboBox);
        leftGroup.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        leftGroup.setSpacing(true);

        HorizontalLayout formRow = new HorizontalLayout(leftGroup, rightGroup);
        formRow.setWidthFull();
        formRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        formRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        card.add(title, formRow);
        return card;
    }

    private Component createHistoryGrid() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();

        H3 title = new H3("Analysis History");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        grid = new Grid<>(CompanyAnalysisDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(CompanyAnalysisDto::getTicker).setHeader("Ticker");
        grid.addColumn(CompanyAnalysisDto::getMarket).setHeader("Market");

        grid.addColumn(new ComponentRenderer<>(analysis -> {
            AnalysisStatus status = analysis.getStatus();
            String statusName = status != null ? status.name() : "PENDING";
            Span badge = new Span(statusName);
            String theme = "badge";
            if (status != null) {
                switch (status) {
                    case COMPLETED -> theme += " success";
                    case FAILED -> theme += " error";
                    default -> theme += " warning";
                }
            }
            badge.getElement().getThemeList().add(theme);
            return badge;
        })).setHeader("Status");

        grid.addColumn(CompanyAnalysisDto::getConviction).setHeader("Conviction");

        grid.addColumn(new ComponentRenderer<>(analysis -> {
            var outlook = analysis.getOutlook();
            if (outlook == null) {
                Span empty = new Span("PENDING");
                empty.getElement().getThemeList().add("badge contrast");
                return empty;
            }
            String name = outlook.name();
            Span badge = new Span(name);
            String theme = "badge";
            if (name.contains("BULLISH")) {
                theme += " success";
            } else if (name.contains("BEARISH")) {
                theme += " error";
            } else {
                theme += " contrast";
            }
            badge.getElement().getThemeList().add(theme);
            return badge;
        })).setHeader("Outlook");

        grid.addColumn(new ComponentRenderer<>(analysis -> {
            var rec = analysis.getRecommendation();
            if (rec == null) {
                Span empty = new Span("PENDING");
                empty.getElement().getThemeList().add("badge contrast");
                return empty;
            }
            String name = rec.name();
            Span badge = new Span(name);
            String theme = "badge";
            if (name.contains("BUY")) {
                theme += " success primary";
            } else if (name.contains("SELL")) {
                theme += " error";
            } else {
                theme += " contrast";
            }
            badge.getElement().getThemeList().add(theme);
            return badge;
        })).setHeader("Recommendation");

        grid.addColumn(CompanyAnalysisDto::getAnalysisDate).setHeader("Analysis Date");

        grid.addColumn(new ComponentRenderer<>(analysis -> {
            Button viewBtn = new Button(VaadinIcon.EYE.create());
            viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            viewBtn.setAriaLabel("View details");
            viewBtn.setTooltipText("View details");
            viewBtn.addClickListener(e -> showAnalysisDetails(analysis));
            return viewBtn;
        })).setHeader("Actions");

        grid.addItemDoubleClickListener(event -> showAnalysisDetails(event.getItem()));

        grid.setItems(q -> companyAnalysisApi.getCompanyAnalysisList(PageRequest.of(q.getPage(), q.getPageSize())).stream(),
                _ -> (int) companyAnalysisApi.getAnalysisCount());

        layout.add(title, grid);
        layout.expand(grid);
        return layout;
    }

    private void triggerAnalysis(CompanyDto company, StatementVariant variant) {
        if (company == null) {
            showError("Please select a company.");
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

    private void showAnalysisDetails(CompanyAnalysisDto analysis) {
        Dialog dialog = new Dialog();
        dialog.setWidth("85vw");
        dialog.setHeight("85vh");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);

        H3 title = new H3(String.format("%s - %s Analysis", analysis.getTicker(), analysis.getMarket().toUpperCase()));
        title.addClassNames(LumoUtility.Margin.NONE);

        Span statusBadge = new Span(analysis.getStatus().name());
        String statusTheme = "badge";
        if (analysis.getStatus() == AnalysisStatus.COMPLETED) {
            statusTheme += " success";
        } else if (analysis.getStatus() == AnalysisStatus.FAILED) {
            statusTheme += " error";
        }
        statusBadge.getElement().getThemeList().add(statusTheme);

        headerLayout.add(title, statusBadge);

        if (analysis.getRecommendation() != null) {
            Span recBadge = new Span(analysis.getRecommendation().name());
            String recTheme = "badge";
            if (analysis.getRecommendation().name().contains("BUY")) {
                recTheme += " success primary";
            } else if (analysis.getRecommendation().name().contains("SELL")) {
                recTheme += " error";
            } else {
                recTheme += " contrast";
            }
            recBadge.getElement().getThemeList().add(recTheme);
            headerLayout.add(new Span("Rec:"), recBadge);
        }

        if (analysis.getOutlook() != null) {
            Span outlookBadge = new Span(analysis.getOutlook().name());
            String outTheme = "badge";
            if (analysis.getOutlook().name().contains("BULLISH")) {
                outTheme += " success";
            } else if (analysis.getOutlook().name().contains("BEARISH")) {
                outTheme += " error";
            } else {
                outTheme += " contrast";
            }
            outlookBadge.getElement().getThemeList().add(outTheme);
            headerLayout.add(new Span("Outlook:"), outlookBadge);
        }

        if (analysis.getConviction() != null) {
            Span convictionSpan = new Span("Conviction: " + analysis.getConviction() + "/5");
            convictionSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);
            headerLayout.add(convictionSpan);
        }

        dialog.getHeader().add(headerLayout);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        tabSheet.add("Markdown Report", createReportTab(analysis));
        tabSheet.add("JSON Data", createJsonTab(analysis));

        if (analysis.getStatus() == AnalysisStatus.FAILED && analysis.getError() != null) {
            tabSheet.add("Error Details", createErrorTab(analysis));
        }

        dialog.add(tabSheet);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private Component createReportTab(CompanyAnalysisDto analysis) {
        String markdown = analysis.getReport();
        if (markdown == null || markdown.isBlank()) {
            Div empty = new Div();
            empty.setText("No report generated yet.");
            empty.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Padding.LARGE);
            return empty;
        }

        try {
            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String htmlContent = renderer.render(parser.parse(markdown));

            Div container = new Div();
            container.getStyle().set("padding", "24px").set("line-height", "1.6").set("color", "var(--lumo-body-text-color)");

            String css = """
                        <style>
                        .rendered-markdown {
                            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            color: var(--lumo-body-text-color);
                            line-height: 1.6;
                            font-size: 1rem;
                        }
                        .rendered-markdown h1, .rendered-markdown h2, .rendered-markdown h3, .rendered-markdown h4 {
                            color: var(--lumo-header-text-color);
                            font-weight: 600;
                            margin-top: 1.5em;
                            margin-bottom: 0.5em;
                        }
                        .rendered-markdown h1 { font-size: 1.75rem; border-bottom: 1px solid var(--lumo-contrast-10pct); padding-bottom: 0.3em; }
                        .rendered-markdown h2 { font-size: 1.4rem; border-bottom: 1px solid var(--lumo-contrast-10pct); padding-bottom: 0.3em; }
                        .rendered-markdown h3 { font-size: 1.2rem; }
                        .rendered-markdown p { margin-top: 0; margin-bottom: 1em; }
                        .rendered-markdown ul, .rendered-markdown ol { padding-left: 2em; margin-bottom: 1em; }
                        .rendered-markdown li { margin-bottom: 0.5em; }
                        .rendered-markdown code {
                            font-family: monospace;
                            font-size: 0.9em;
                            background-color: var(--lumo-contrast-5pct);
                            padding: 0.25em 0.45em;
                            border-radius: 4px;
                            color: var(--lumo-primary-text-color);
                        }
                        .rendered-markdown pre {
                            background-color: var(--lumo-contrast-5pct);
                            padding: 1.2em;
                            border-radius: 8px;
                            overflow-x: auto;
                            margin-bottom: 1.5em;
                            border: 1px solid var(--lumo-contrast-10pct);
                        }
                        .rendered-markdown pre code {
                            background-color: transparent;
                            padding: 0;
                            font-size: 0.85em;
                            color: inherit;
                        }
                        .rendered-markdown table {
                            border-collapse: collapse;
                            width: 100%;
                            margin-bottom: 1.5em;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 0 0 1px var(--lumo-contrast-10pct);
                        }
                        .rendered-markdown th, .rendered-markdown td {
                            padding: 12px 15px;
                            text-align: left;
                        }
                        .rendered-markdown th {
                            background-color: var(--lumo-contrast-10pct);
                            font-weight: 600;
                            color: var(--lumo-header-text-color);
                        }
                        .rendered-markdown tr:nth-child(even) {
                            background-color: var(--lumo-contrast-5pct);
                        }
                        .rendered-markdown tr {
                            border-bottom: 1px solid var(--lumo-contrast-10pct);
                        }
                        .rendered-markdown tr:last-child {
                            border-bottom: none;
                        }
                        .rendered-markdown blockquote {
                            border-left: 4px solid var(--lumo-primary-color);
                            padding: 0.5em 1.2em;
                            color: var(--lumo-secondary-text-color);
                            background-color: var(--lumo-primary-color-10pct);
                            margin: 1.5em 0;
                            border-radius: 0 8px 8px 0;
                        }
                        </style>
                    """;

            Html html = new Html("<div>" + css + "<div class='rendered-markdown'>" + htmlContent + "</div></div>");
            container.add(html);

            Scroller scroller = new Scroller(container);
            scroller.setSizeFull();
            return scroller;
        } catch (Exception e) {
            TextArea area = new TextArea();
            area.setValue(markdown);
            area.setReadOnly(true);
            area.setSizeFull();
            return area;
        }
    }

    private Component createJsonTab(CompanyAnalysisDto analysis) {
        String jsonData = analysis.getAnalysisData();
        if (jsonData == null || jsonData.isBlank()) {
            Div empty = new Div();
            empty.setText("No JSON data generated.");
            empty.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Padding.LARGE);
            return empty;
        }

        String prettyJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readValue(jsonData, Object.class);
            prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            prettyJson = jsonData;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setSpacing(true);

        final String finalPrettyJson = prettyJson;

        Button copyBtn = new Button("Copy to Clipboard", VaadinIcon.COPY.create());
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        copyBtn.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0);", finalPrettyJson);
            Notification.show("JSON copied to clipboard!", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        Button downloadBtn = new Button("Download JSON", VaadinIcon.DOWNLOAD.create());
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        DownloadHandler downloadHandler = DownloadHandler.fromInputStream(_ -> {
            byte[] bytes = finalPrettyJson.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    analysis.getTicker() + "_analysis.json",
                    "application/json",
                    bytes.length);
        });
        Anchor downloadLink = new Anchor(downloadHandler, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(downloadBtn);

        toolbar.add(copyBtn, downloadLink);

        TextArea textArea = new TextArea();
        textArea.setValue(prettyJson);
        textArea.setReadOnly(true);
        textArea.setSizeFull();
        textArea.getStyle().set("font-family", "monospace");
        textArea.getStyle().set("font-size", "0.9rem");

        layout.add(toolbar, textArea);
        layout.setFlexGrow(1, textArea);
        return layout;
    }

    private Component createErrorTab(CompanyAnalysisDto analysis) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);

        Div errorBanner = new Div();
        errorBanner.setText("Analysis execution encountered an error:");
        errorBanner.addClassNames(LumoUtility.Background.ERROR_10,
                LumoUtility.TextColor.ERROR,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Width.FULL);

        TextArea errorDetails = new TextArea();
        errorDetails.setValue(analysis.getError() != null ? analysis.getError() : "Unknown error");
        errorDetails.setReadOnly(true);
        errorDetails.setSizeFull();
        errorDetails.getStyle().set("font-family", "monospace");

        layout.add(errorBanner, errorDetails);
        layout.setFlexGrow(1, errorDetails);
        return layout;
    }

    private void showError(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
