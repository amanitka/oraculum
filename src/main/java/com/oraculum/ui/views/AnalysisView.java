package com.oraculum.ui.views;

import com.oraculum.analyst.api.CompanyAnalysisApi;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
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
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.*;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Route(value = "analysis", layout = MainLayout.class)
@PageTitle("Analysis | Oraculum")
public class AnalysisView extends VerticalLayout {

    private final CompanyApi companyApi;
    private final CompanyAnalysisApi companyAnalysisApi;
    private final AnalysisRequestService analysisRequestService;
    private final ObjectMapper objectMapper;
    private ComboBox<CompanyDto> companyComboBox;
    private ComboBox<StatementVariant> variantComboBox;
    private Grid<CompanyAnalysisDto> grid;

    // ── Trigger Toolbar ────────────────────────────────────────────────────

    public AnalysisView(CompanyApi companyApi, CompanyAnalysisApi companyAnalysisApi, AnalysisRequestService analysisRequestService, ObjectMapper objectMapper) {
        this.companyApi = companyApi;
        this.companyAnalysisApi = companyAnalysisApi;
        this.analysisRequestService = analysisRequestService;
        this.objectMapper = objectMapper;

        setSizeFull();
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

    private static String formatJson(String raw) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(raw, Object.class));
        } catch (Exception e) {
            return raw;
        }
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
        companyComboBox.setItems(companyApi.getAllCompanies());

        variantComboBox = new ComboBox<>("Statement Variant");
        variantComboBox.setItems(StatementVariant.values());
        variantComboBox.setPlaceholder("Auto (Planner Decides)");
        variantComboBox.setClearButtonVisible(true);
        variantComboBox.setWidthFull();

        Details advancedDetails = new Details("Advanced Options", variantComboBox);
        advancedDetails.setOpened(false);

        HorizontalLayout leftGroup = new HorizontalLayout(companyComboBox, advancedDetails);
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
            CompanyAnalysisRequestEvent request = new CompanyAnalysisRequestEvent(UUID.randomUUID(),
                    company.id(), company.ticker(), company.market(),
                    LocalDate.now(), variantComboBox.getValue());
            analysisRequestService.requestAnalysis(request);
            ViewHelper.showSuccess("Analysis triggered for " + company.ticker());
            grid.getDataProvider().refreshAll();
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

        List<CompanyAnalysisDto> data = companyAnalysisApi.getCompanyAnalysisList(PageRequest.of(0, 1000)).getContent();
        GridListDataView<CompanyAnalysisDto> dataView = grid.setItems(data);
        setupFilters(dataView);

        layout.add(title, grid);
        layout.expand(grid);

        return ViewHelper.wrapInCard(layout);
    }

    // ── Analysis Details Dialog ────────────────────────────────────────────

    private Grid<CompanyAnalysisDto> buildGrid() {
        Grid<CompanyAnalysisDto> g = new Grid<>(CompanyAnalysisDto.class, false);
        g.setSizeFull();
        g.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        g.addClassName("screener-grid");

        g.addColumn(CompanyAnalysisDto::getTicker).setHeader("Ticker").setKey("ticker").setSortable(true);
        g.addColumn(CompanyAnalysisDto::getMarket).setHeader("Market").setKey("market").setSortable(true);

        g.addColumn(new ComponentRenderer<>(a -> ViewHelper.statusBadge(a.getStatus())))
                .setHeader("Status").setKey("status")
                .setComparator(Comparator.comparing(CompanyAnalysisDto::getStatus, Comparator.nullsLast(Comparator.naturalOrder())))
                .setSortable(true);

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

            Button companyBtn = ViewHelper.createCompanyDetailsButton(companyApi, objectMapper, a.getCompanyId(), false);

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

    // ── Report Tab ─────────────────────────────────────────────────────────

    private void showAnalysisDetails(CompanyAnalysisDto analysis) {
        Dialog dialog = new Dialog();
        dialog.setWidth("85vw");
        dialog.setHeight("85vh");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        dialog.getHeader().add(buildDialogHeader(analysis));

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.add("Markdown Report", createReportTab(analysis));

        addAgentTabs(tabSheet, analysis.getAnalysisData());

        tabSheet.add("JSON Data", createJsonTab(analysis));
        if (analysis.getStatus() == AnalysisStatus.FAILED && analysis.getError() != null) {
            tabSheet.add("Error Details", createErrorTab(analysis));
        }
        dialog.add(tabSheet);

        Button closeButton = new Button("Close", _ -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    // ── JSON Tab ───────────────────────────────────────────────────────────

    private HorizontalLayout buildDialogHeader(CompanyAnalysisDto analysis) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        H3 title = new H3(String.format("%s - %s Analysis", analysis.getTicker(), analysis.getMarket().toUpperCase()));
        title.addClassNames(LumoUtility.Margin.NONE);
        header.add(title, ViewHelper.statusBadge(analysis.getStatus()));

        if (analysis.getRecommendation() != null) {
            header.add(new Span("Rec:"), ViewHelper.recommendationBadge(analysis.getRecommendation()));
        }
        if (analysis.getOutlook() != null) {
            header.add(new Span("Outlook:"), ViewHelper.outlookBadge(analysis.getOutlook()));
        }
        if (analysis.getConviction() != null) {
            Span conv = new Span("Conviction: " + analysis.getConviction() + "/5");
            conv.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);
            header.add(conv);
        }
        return header;
    }

    // ── Error Tab ──────────────────────────────────────────────────────────

    private Component createReportTab(CompanyAnalysisDto analysis) {
        String markdown = analysis.getReport();
        if (markdown == null || markdown.isBlank()) {
            return ViewHelper.emptyPlaceholder("No report generated yet.");
        }
        try {
            String htmlContent = HtmlRenderer.builder().build()
                    .render(Parser.builder().build().parse(markdown));

            Div container = new Div();
            container.getStyle().set("padding", "24px").set("line-height", "1.6").set("color", "var(--lumo-body-text-color)");
            container.add(new Html("<div><div class='rendered-markdown'>" + htmlContent + "</div></div>"));

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

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Component createJsonTab(CompanyAnalysisDto analysis) {
        String jsonData = analysis.getAnalysisData();
        if (jsonData == null || jsonData.isBlank()) {
            return ViewHelper.emptyPlaceholder("No JSON data generated.");
        }

        String prettyJson = formatJson(jsonData);

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setSpacing(true);

        Button copyBtn = new Button("Copy to Clipboard", VaadinIcon.COPY.create());
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        copyBtn.addClickListener(_ -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0);", prettyJson);
            Notification.show("JSON copied to clipboard!", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        Button downloadBtn = new Button("Download JSON", VaadinIcon.DOWNLOAD.create());
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        DownloadHandler downloadHandler = DownloadHandler.fromInputStream(_ -> {
            byte[] bytes = prettyJson.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    analysis.getTicker() + "_analysis.json", "application/json", bytes.length);
        });
        Anchor downloadLink = new Anchor(downloadHandler, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(downloadBtn);
        toolbar.add(copyBtn, downloadLink);

        TextArea textArea = new TextArea();
        textArea.setValue(prettyJson);
        textArea.setReadOnly(true);
        textArea.setSizeFull();
        textArea.getStyle().set("font-family", "monospace").set("font-size", "0.9rem");

        VerticalLayout layout = new VerticalLayout(toolbar, textArea);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setFlexGrow(1, textArea);
        return layout;
    }

    // ── Analysis Filter ────────────────────────────────────────────────────

    private Component createErrorTab(CompanyAnalysisDto analysis) {
        Div errorBanner = new Div();
        errorBanner.setText("Analysis execution encountered an error:");
        errorBanner.addClassNames(LumoUtility.Background.ERROR_10, LumoUtility.TextColor.ERROR,
                LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.FontWeight.BOLD, LumoUtility.Width.FULL);

        TextArea errorDetails = new TextArea();
        errorDetails.setValue(analysis.getError() != null ? analysis.getError() : "Unknown error");
        errorDetails.setReadOnly(true);
        errorDetails.setSizeFull();
        errorDetails.getStyle().set("font-family", "monospace");

        VerticalLayout layout = new VerticalLayout(errorBanner, errorDetails);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setFlexGrow(1, errorDetails);
        return layout;
    }

    // ── Markdown CSS ───────────────────────────────────────────────────────

    private void addAgentTabs(TabSheet tabSheet, String jsonData) {
        if (jsonData == null || jsonData.isBlank()) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonData);

            for (AgentType type : AgentType.values()) {
                if (type == AgentType.SYNTHESIZER) {
                    continue;
                }
                String key = type.name();
                if (rootNode.has(key)) {
                    JsonNode agentData = rootNode.get(key);
                    Component tabContent = createAgentTabContent(agentData);
                    if (tabContent != null) {
                        tabSheet.add(type.getAgentName(), tabContent);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fallback without adding tabs
        }
    }

    private Component createAgentTabContent(JsonNode agentData) {
        if (agentData == null || agentData.properties().isEmpty()) {
            return null;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.getStyle().set("overflow-y", "auto");

        for (Map.Entry<String, JsonNode> field : agentData.properties()) {
            String key = field.getKey();
            JsonNode value = field.getValue();

            H4 fieldHeader = new H4(formatKeyTitle(key));
            fieldHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.XSMALL);
            layout.add(fieldHeader);

            if (value.isArray()) {
                UnorderedList list = new UnorderedList();
                list.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL);
                for (JsonNode item : value) {
                    list.add(new ListItem(item.asString()));
                }
                layout.add(list);
            } else if (value.isObject()) {
                Pre pre = new Pre();
                pre.setText(value.toPrettyString());
                pre.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL, LumoUtility.FontSize.SMALL);
                pre.getStyle().set("border-radius", "4px").set("font-family", "monospace");
                layout.add(pre);
            } else {
                Paragraph p = new Paragraph(value.asString());
                p.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.BODY);
                layout.add(p);
            }
        }

        return layout;
    }

    private String formatKeyTitle(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
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
}
