package com.oraculum.ui.views.components;

import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.oraculum.ui.views.components.renderer.AgentDataRenderer;
import com.oraculum.ui.views.components.renderer.InvestmentSnapshotRenderer;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import com.oraculum.ui.views.components.renderer.RendererUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
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
    private final InvestmentSnapshotRenderer investmentSnapshotRenderer;
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

        if (analysis.getAnalysisResult() != null) {
            Component summaryTabContent = createSummaryTab(analysis);
            if (summaryTabContent != null) {
                tabSheet.add("Executive Summary", summaryTabContent);
            }
        }

        tabSheet.add("Synthesizer Report", createReportTab(analysis));

        Component workflowDetails = createWorkflowDetailsTab(analysis);
        tabSheet.add("Details", workflowDetails);

        return tabSheet;
    }

    private Component createSummaryTab(CompanyAnalysisDto analysis) {
        Component snapshotCard = investmentSnapshotRenderer.renderInvestmentSnapshot(analysis.getAnalysisResult(), analysis);
        if (snapshotCard == null) return null;

        VerticalLayout layout = new VerticalLayout(snapshotCard);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.getStyle().set("box-sizing", "border-box").set("padding", "24px 16px");

        Scroller scroller = new Scroller(layout, Scroller.ScrollDirection.VERTICAL);
        scroller.setWidthFull();
        scroller.getStyle().set("overflow-x", "hidden");
        return scroller;
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

    private Component createReportTab(CompanyAnalysisDto analysis) {
        String jsonData = analysis.getAnalysisData();
        if (jsonData == null || jsonData.isBlank()) {
            return new Span("No report data available.");
        }

        try {
            JsonNode rootNode = jsonMapper.readTree(jsonData);
            JsonNode synthNode = rootNode.path("SYNTHESIZER");
            if (synthNode.isMissingNode() || synthNode.isNull()) {
                String md = analysis.getAnalysisResult() != null ? analysis.getAnalysisResult().executiveSummary() : null;
                if (md == null || md.isBlank()) {
                    return new Span("No report generated.");
                }
                Component markdownContainer = markdownRenderer.renderMarkdownWithCitations(md, analysis.getAnalysisData());
                markdownContainer.getStyle()
                        .set("max-width", "100%")
                        .set("box-sizing", "border-box")
                        .set("margin", "0 auto")
                        .set("padding", "24px 16px")
                        .set("color", "var(--lumo-body-text-color)");
                
                Scroller scroller = new Scroller(markdownContainer, Scroller.ScrollDirection.VERTICAL);
                scroller.setWidthFull();
                scroller.getStyle().set("overflow-x", "hidden");
                return scroller;
            }

            Component tabContent = agentDataRenderer.createAgentTabContent(synthNode, jsonData);
            if (tabContent == null) {
                return new Span("Empty synthesizer data.");
            }
            return tabContent;

        } catch (Exception e) {
            return new Span("Error rendering report: " + e.getMessage());
        }
    }

    private Component createJsonTab(CompanyAnalysisDto analysis) {
        String prettyJson = "";
        try {
            if (analysis.getAnalysisData() != null) {
                Object json = jsonMapper.readValue(analysis.getAnalysisData(), Object.class);
                prettyJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            }
        } catch (Exception e) {
            prettyJson = "Error formatting JSON: " + e.getMessage();
        }

        TextArea textArea = new TextArea();
        textArea.setValue(prettyJson);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.setMinHeight("600px");
        textArea.getStyle().set("font-family", "monospace").set("font-size", "0.9rem");

        final String finalJson = prettyJson;
        Button copyButton = new Button("Copy JSON", _ -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", finalJson);
            Notification.show("JSON copied to clipboard");
        });
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Anchor downloadAnchor = new Anchor((DownloadEvent event) -> {
            String filename = analysis.getTicker() != null ? "analysis_" + analysis.getTicker() + ".json" : "analysis.json";
            event.setFileName(filename);
            event.getResponse().setHeader("Content-Type", "application/json");
            try (OutputStream out = event.getOutputStream()) {
                out.write(finalJson.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }, "");
        downloadAnchor.getElement().setAttribute("download", true);
        Button downloadButton = new Button("Download JSON");
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        downloadAnchor.add(downloadButton);

        HorizontalLayout toolbar = new HorizontalLayout(copyButton, downloadAnchor);
        toolbar.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(toolbar, textArea);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setFlexGrow(1, textArea);
        return layout;
    }

    private void addAgentTabs(TabSheet tabSheet, String jsonData) {
        if (jsonData == null || jsonData.isBlank()) {
            return;
        }
        try {
            JsonNode rootNode = jsonMapper.readTree(jsonData);

            for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
                String key = entry.getKey();
                if (key.startsWith("SYNTHESIZER") || key.equals(TRACE_CITATIONS_KEY)) {
                    continue;
                }

                Component tabContent = agentDataRenderer.createAgentTabContent(entry.getValue(), jsonData);
                if (tabContent != null) {
                    tabSheet.add(RendererUtil.formatKeyTitle(key), tabContent);
                }
            }
        } catch (Exception e) {
            // Silently fallback without adding tabs
        }
    }
}
