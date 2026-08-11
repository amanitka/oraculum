package com.oraculum.ui.views.components;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.ui.views.components.renderer.AgentDataRenderer;
import com.oraculum.ui.views.components.renderer.AnalysisOverviewRenderer;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.RendererUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisResultRenderer {

    private static final String TRACE_CITATIONS_KEY = "CITATIONS";
    private final JsonMapper jsonMapper;
    private final AnalysisOverviewRenderer analysisOverviewRenderer;
    private final MarkdownRenderer markdownRenderer;
    private final AgentDataRenderer agentDataRenderer;

    public Component renderAnalysisResult(CompanyAnalysisDto analysis) {
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(false);
        root.setWidthFull();
        root.add(renderAnalysisTabs(analysis));
        return root;
    }

    public TabSheet renderAnalysisTabs(CompanyAnalysisDto analysis) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        if (analysis.getError() != null) {
            tabSheet.add("Error", createErrorTab(analysis));
            return tabSheet;
        }

        tabSheet.add("Overview", createOverviewTab(analysis));
        tabSheet.add("Scenarios", createScenariosTab(analysis));
        tabSheet.add("Report", createReportTab(analysis));
        tabSheet.add("Details", createWorkflowDetailsTab(analysis));
        return tabSheet;
    }

    private Component createOverviewTab(CompanyAnalysisDto analysis) {
        VerticalLayout layout = createBaseContainer();
        if (analysis.getAnalysisResult() != null) {
            Component overviewCard = analysisOverviewRenderer.renderOverviewTab(analysis.getAnalysisResult(), analysis);
            if (overviewCard != null) layout.add(overviewCard);
        } else if (analysis.getAnalysisData() != null && !analysis.getAnalysisData().isBlank()) {
            layout.add(renderLegacyJsonReportFallback(analysis.getAnalysisData()));
        }
        return wrapInScroller(layout);
    }

    private Component createScenariosTab(CompanyAnalysisDto analysis) {
        VerticalLayout layout = createBaseContainer();
        if (analysis.getAnalysisResult() != null) {
            Component scenariosCard = analysisOverviewRenderer.renderScenariosTab(analysis.getAnalysisResult(), analysis);
            if (scenariosCard != null) layout.add(scenariosCard);
        }
        return wrapInScroller(layout);
    }

    private Component createReportTab(CompanyAnalysisDto analysis) {
        VerticalLayout layout = createBaseContainer();
        AnalysisResult result = analysis.getAnalysisResult();
        String jsonData = analysis.getAnalysisData();
        if (result != null) {
            if (result.executiveSummary() != null && !result.executiveSummary().isBlank()) {
                addReportSection(layout, "Executive Summary", result.executiveSummary(), jsonData);
            }
            if (result.recommendationReasoning() != null && !result.recommendationReasoning().isBlank()) {
                addReportSection(layout, "Valuation Justification", result.recommendationReasoning(), jsonData);
            }
        }
        return wrapInScroller(layout);
    }

    private void addReportSection(VerticalLayout layout, String title, String markdownText, String jsonData) {
        H4 header = new H4(title);
        header.getStyle()
                .set("margin-top", "24px")
                .set("margin-bottom", "14px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "8px")
                .set("font-size", "1.15rem")
                .set("color", "var(--lumo-header-text-color)");
        layout.add(header);
        layout.add(markdownRenderer.renderMarkdownWithCitations(markdownText, jsonData));
    }

    private Component renderLegacyJsonReportFallback(String jsonData) {
        try {
            JsonNode rootNode = jsonMapper.readTree(jsonData);
            JsonNode synthNode = rootNode.path("SYNTHESIZER");
            if (!synthNode.isMissingNode() && !synthNode.isNull()) {
                Component tabContent = agentDataRenderer.createAgentTabContent(synthNode, jsonData);
                if (tabContent != null) return tabContent;
            }
        } catch (Exception ignored) {
        }
        return new Span("No report data available.");
    }

    private Component createWorkflowDetailsTab(CompanyAnalysisDto analysis) {
        TabSheet subTabSheet = new TabSheet();
        subTabSheet.setWidthFull();
        addAgentTabs(subTabSheet, analysis.getAnalysisData());
        subTabSheet.add("JSON Data", createJsonTab(analysis));
        return subTabSheet;
    }

    private Component createErrorTab(CompanyAnalysisDto analysis) {
        Div errorBanner = new Div();
        errorBanner.setText("Analysis execution encountered an error:");
        errorBanner.addClassNames(LumoUtility.Background.ERROR_10, LumoUtility.TextColor.ERROR,
                LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.FontWeight.BOLD, LumoUtility.Width.FULL);

        TextArea errorDetails = new TextArea();
        errorDetails.setValue(analysis.getError() != null ? analysis.getError() : "Unknown error");
        errorDetails.setReadOnly(true);
        errorDetails.setWidthFull();
        errorDetails.setMinHeight("400px");
        errorDetails.getStyle().set("font-family", "monospace");

        VerticalLayout layout = new VerticalLayout(errorBanner, errorDetails);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setFlexGrow(1, errorDetails);
        return layout;
    }

    private Component createJsonTab(CompanyAnalysisDto analysis) {
        String jsonText = formatJson(analysis.getAnalysisData());

        TextArea textArea = new TextArea();
        textArea.setValue(jsonText);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.setMinHeight("600px");
        textArea.getStyle().set("font-family", "monospace").set("font-size", "0.9rem");

        VerticalLayout layout = new VerticalLayout(buildJsonToolbar(analysis, jsonText), textArea);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setFlexGrow(1, textArea);
        return layout;
    }

    private HorizontalLayout buildJsonToolbar(CompanyAnalysisDto analysis, String jsonText) {
        Button copyButton = new Button("Copy JSON", _ -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", jsonText);
            Notification.show("JSON copied to clipboard");
        });
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Anchor downloadAnchor = createDownloadAnchor(analysis, jsonText);
        HorizontalLayout toolbar = new HorizontalLayout(copyButton, downloadAnchor);
        toolbar.setSpacing(true);
        return toolbar;
    }

    private Anchor createDownloadAnchor(CompanyAnalysisDto analysis, String jsonText) {
        Anchor downloadAnchor = new Anchor((DownloadEvent event) -> {
            String filename = analysis.getTicker() != null ? "analysis_" + analysis.getTicker() + ".json" : "analysis.json";
            event.setFileName(filename);
            event.getResponse().setHeader("Content-Type", "application/json");
            try (OutputStream out = event.getOutputStream()) {
                out.write(jsonText.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }, "");
        downloadAnchor.getElement().setAttribute("download", true);

        Button downloadButton = new Button("Download JSON");
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        downloadAnchor.add(downloadButton);
        return downloadAnchor;
    }

    private String formatJson(String jsonData) {
        if (jsonData == null) return "";
        try {
            Object json = jsonMapper.readValue(jsonData, Object.class);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return "Error formatting JSON: " + e.getMessage();
        }
    }

    private void addAgentTabs(TabSheet tabSheet, String jsonData) {
        if (jsonData == null || jsonData.isBlank()) return;

        try {
            JsonNode rootNode = jsonMapper.readTree(jsonData);
            for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
                String key = entry.getKey();
                if (key.startsWith("SYNTHESIZER") || key.equals(TRACE_CITATIONS_KEY)) continue;

                Component tabContent = agentDataRenderer.createAgentTabContent(entry.getValue(), jsonData);
                if (tabContent != null) {
                    tabSheet.add(RendererUtil.formatKeyTitle(key), tabContent);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private VerticalLayout createBaseContainer() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.getStyle()
                .set("max-width", "100%")
                .set("box-sizing", "border-box")
                .set("margin", "0 auto")
                .set("padding", "24px 16px");
        return layout;
    }

    private Scroller wrapInScroller(Component content) {
        Scroller scroller = new Scroller(content, Scroller.ScrollDirection.VERTICAL);
        scroller.setWidthFull();
        scroller.getStyle().set("overflow-x", "hidden");
        return scroller;
    }
}
