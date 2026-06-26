package com.oraculum.ui.views.components;

import com.oraculum.analyst.api.dto.CompanyAnalysisDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisResultRenderer {

    private static final String TRACE_CITATIONS_KEY = "CITATIONS";
    private final JsonMapper jsonMapper;

    public TabSheet renderAnalysisTabs(CompanyAnalysisDto analysis) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        if (analysis.getError() != null) {
            tabSheet.add("Error", createErrorTab(analysis));
            return tabSheet;
        }

        tabSheet.add("Markdown Report", createReportTab(analysis));
        addAgentTabs(tabSheet, analysis.getAnalysisData());
        tabSheet.add("JSON Data", createJsonTab(analysis));

        return tabSheet;
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
        errorDetails.setSizeFull();
        errorDetails.getStyle().set("font-family", "monospace");

        VerticalLayout layout = new VerticalLayout(errorBanner, errorDetails);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setFlexGrow(1, errorDetails);
        return layout;
    }

    private Component createReportTab(CompanyAnalysisDto analysis) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.getStyle().set("overflow-y", "auto");

        String md = analysis.getReport();
        if (md == null || md.isBlank()) {
            layout.add(new Span("No report generated."));
        } else {
            Component markdownContainer = renderMarkdownWithCitations(md, analysis.getAnalysisData());
            markdownContainer.getStyle().set("padding", "24px").set("color", "var(--lumo-body-text-color)");

            Scroller scroller = new Scroller(markdownContainer);
            scroller.setSizeFull();
            layout.add(scroller);
        }

        return layout;
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
        textArea.setSizeFull();
        textArea.getStyle().set("font-family", "monospace").set("font-size", "0.9rem");

        VerticalLayout layout = new VerticalLayout(textArea);
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

                Component tabContent = createAgentTabContent(entry.getValue(), jsonData);
                if (tabContent != null) {
                    tabSheet.add(formatKeyTitle(key), tabContent);
                }
            }
        } catch (Exception e) {
            // Silently fallback without adding tabs
        }
    }

    private Component createAgentTabContent(JsonNode agentData, String jsonData) {
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

            com.vaadin.flow.component.html.H3 fieldHeader = new com.vaadin.flow.component.html.H3(formatKeyTitle(key));
            fieldHeader.addClassName("json-key-header");
            layout.add(fieldHeader);

            if (value.isArray()) {
                layout.add(renderArrayField(key, value, jsonData));
            } else if (value.isObject()) {
                layout.add(renderObjectField(value));
            } else {
                layout.add(renderPrimitiveField(value, jsonData));
            }
        }

        return layout;
    }

    private Component renderArrayField(String key, JsonNode value, String jsonData) {
        UnorderedList list = new UnorderedList();
        list.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL);
        for (JsonNode item : value) {
            if (item.isObject()) {
                if ("recommended_reruns".equals(key)) {
                    list.add(new ListItem(renderRecommendedRerunItem(item)));
                } else {
                    list.add(new ListItem(renderObjectField(item)));
                }
            } else {
                ListItem li = new ListItem();
                li.add(renderMarkdownWithCitations(item.asString(), jsonData));
                list.add(li);
            }
        }
        return list;
    }

    private Component renderRecommendedRerunItem(JsonNode item) {
        Div rerunDiv = new Div();
        rerunDiv.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

        String specialist = item.path("specialist").asString("");
        String severity = item.path("severity").asString("");
        String instruction = item.path("instruction").asString("");

        Span badge = new Span(specialist + " (Severity " + severity + ")");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("error");
        badge.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        badge.getStyle().set("display", "inline-block");

        Paragraph inst = new Paragraph(instruction);
        inst.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);

        rerunDiv.add(badge, inst);
        return rerunDiv;
    }

    private Component renderObjectField(JsonNode value) {
        Pre pre = new Pre(value.toPrettyString());
        pre.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL, LumoUtility.FontSize.SMALL);
        pre.getStyle().set("border-radius", "4px").set("font-family", "monospace");
        return pre;
    }

    private Component renderPrimitiveField(JsonNode value, String jsonData) {
        return renderMarkdownWithCitations(value.asString(), jsonData);
    }

    private Component renderMarkdownWithCitations(String strValue, String jsonData) {
        try {
            JsonNode citationsNode = null;
            if (jsonData != null) {
                JsonNode rootNode = jsonMapper.readTree(jsonData);
                if (rootNode.has(TRACE_CITATIONS_KEY)) {
                    citationsNode = rootNode.get(TRACE_CITATIONS_KEY);
                }
            }

            String processedMd = injectCitations(strValue, jsonData);
            String htmlContent = HtmlRenderer.builder().build()
                    .render(Parser.builder().build().parse(processedMd));

            CitationMarkdownContainer container = new CitationMarkdownContainer(htmlContent, citationsNode);
            container.getStyle().set("line-height", "1.6").set("font-size", "0.9rem");
            return container;
        } catch (Exception e) {
            Paragraph p = new Paragraph(strValue);
            p.getStyle().set("white-space", "pre-wrap");
            p.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.BODY);
            return p;
        }
    }

    private String injectCitations(String markdown, String analysisDataJson) {
        if (analysisDataJson == null || markdown == null) return markdown;
        try {
            JsonNode rootNode = jsonMapper.readTree(analysisDataJson);
            if (!rootNode.has(TRACE_CITATIONS_KEY)) return markdown;
            JsonNode citationsNode = rootNode.get(TRACE_CITATIONS_KEY);

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(markdown);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String citationId = matcher.group(1);
                if (citationsNode.has(citationId)) {
                    String replacement = "<a href=\"javascript:void(0)\" class=\"reference-data-link\" data-reference-id=\""
                            + citationId + "\">[" + citationId + "]</a>";
                    matcher.appendReplacement(sb, replacement);
                } else {
                    matcher.appendReplacement(sb, "[$1]");
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return markdown;
        }
    }

    private String formatKeyTitle(String key) {
        if (key == null || key.isEmpty()) return key;
        String withSpaces = key.replaceAll("([a-z])([A-Z]+)", "$1 $2").replace("_", " ").toLowerCase();
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }

    public class CitationMarkdownContainer extends Div {

        private final JsonNode citationsNode;

        public CitationMarkdownContainer(String html, JsonNode citationsNode) {
            this.citationsNode = citationsNode;
            add(new Html("<div><div class='rendered-markdown'>" + html + "</div></div>"));

            getElement().executeJs(
                    "const links = this.querySelectorAll('.reference-data-link');" +
                            "links.forEach(link => {" +
                            "  link.addEventListener('click', (e) => {" +
                            "    e.preventDefault();" +
                            "    const citationId = link.getAttribute('data-reference-id');" +
                            "    this.$server.showReferenceDataDialog(citationId);" +
                            "  });" +
                            "});"
            );


        }

        @com.vaadin.flow.component.ClientCallable
        public void showReferenceDataDialog(String citationId) {
            if (citationId == null || citationsNode == null || !citationsNode.has(citationId)) return;
            try {
                JsonNode data = citationsNode.get(citationId);

                Dialog dialog = new Dialog();
                String title = "Citation Source [" + citationId + "]";
                if (data.isObject()) {
                    if (data.has("_source") && data.has("_variant")) {
                        title += " - " + data.get("_source").asString() + " (" + data.get("_variant").asString() + ")";
                    } else if (data.has("_source")) {
                        title += " - " + data.get("_source").asString();
                    }
                }
                dialog.setHeaderTitle(title);

                dialog.setWidth("700px");
                dialog.setMaxHeight("85vh");

                Grid<Map.Entry<String, JsonNode>> grid = new Grid<>();
                grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER,
                        com.vaadin.flow.component.grid.GridVariant.LUMO_COMPACT,
                        com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);
                grid.addColumn(entry -> formatKeyTitle(entry.getKey())).setHeader("Property").setAutoWidth(true).setFlexGrow(1);
                grid.addComponentColumn(entry -> {
                    JsonNode val = entry.getValue();
                    if (val.isObject() || val.isArray()) {
                        Pre pre = new Pre(val.toPrettyString());
                        pre.getStyle().set("margin", "0").set("font-size", "0.85em").set("white-space", "pre-wrap");
                        return pre;
                    } else if (val.isNumber()) {
                        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.US);
                        nf.setGroupingUsed(true);
                        nf.setMaximumFractionDigits(4);
                        return new Span(nf.format(val.asDouble()));
                    } else {
                        return new Span(val.asString());
                    }
                }).setHeader("Value").setFlexGrow(2);

                List<Map.Entry<String, JsonNode>> items = new ArrayList<>();
                data.properties().forEach(entry -> {
                    if (!entry.getKey().startsWith("_")) {
                        items.add(entry);
                    }
                });
                grid.setItems(items);

                Button closeButton = new Button("Close", _ -> dialog.close());
                closeButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
                dialog.getFooter().add(closeButton);

                dialog.add(grid);
                dialog.open();

            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
