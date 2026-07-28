package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.RequiredArgsConstructor;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MarkdownRenderer {

    private static final String TRACE_CITATIONS_KEY = "CITATIONS";
    private final JsonMapper jsonMapper;

    public Component renderMarkdownWithCitations(String strValue, String jsonData) {
        try {
            JsonNode citationsNode = null;
            if (jsonData != null && !jsonData.isBlank()) {
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
        if (analysisDataJson == null || markdown == null || analysisDataJson.isBlank()) return markdown;
        try {
            JsonNode rootNode = jsonMapper.readTree(analysisDataJson);
            if (!rootNode.has(TRACE_CITATIONS_KEY)) return markdown;
            JsonNode citationsNode = rootNode.get(TRACE_CITATIONS_KEY);

            Pattern pattern = Pattern.compile("\\[([\\d,\\s\\?\u26A0\uFE0F]+)\\]");
            Matcher matcher = pattern.matcher(markdown);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String inner = matcher.group(1);
                String[] parts = inner.split(",");
                StringBuilder replacement = new StringBuilder("[");

                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    String id = part.replaceAll("[^\\d]", "");
                    String suffix = part.replaceAll("[\\d]", "").trim();

                    if (!id.isEmpty() && citationsNode.has(id)) {
                        replacement.append("<a href=\"javascript:void(0)\" class=\"reference-data-link\" data-reference-id=\"")
                                .append(id).append("\">").append(id).append("</a>");
                        if (!suffix.isEmpty()) {
                            replacement.append(" ").append(suffix);
                        }
                    } else {
                        replacement.append(part);
                    }

                    if (i < parts.length - 1) {
                        replacement.append(", ");
                    }
                }
                replacement.append("]");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return markdown;
        }
    }

    public static class CitationMarkdownContainer extends Div {

        private final JsonNode citationsNode;

        public CitationMarkdownContainer(String html, JsonNode citationsNode) {
            this.citationsNode = citationsNode;

            String style = "<style>" +
                    ".rendered-markdown { font-size: 1rem; line-height: 1.7; }" +
                    ".rendered-markdown h1, .rendered-markdown h2, .rendered-markdown h3 { margin-top: 2rem; margin-bottom: 1rem; font-weight: 600; color: var(--lumo-header-text-color); }" +
                    ".rendered-markdown h2 { border-bottom: 1px solid var(--lumo-contrast-10pct); padding-bottom: 0.5rem; }" +
                    ".rendered-markdown p { margin-bottom: 1.25rem; }" +
                    ".rendered-markdown strong { font-weight: 600; color: var(--lumo-primary-text-color); }" +
                    ".reference-data-link { font-size: 0.75rem; vertical-align: super; background: var(--lumo-contrast-10pct); border-radius: 4px; padding: 2px 4px; text-decoration: none; color: var(--lumo-primary-text-color); font-weight: bold; margin-left: 2px; transition: all 0.2s ease; }" +
                    ".reference-data-link:hover { background: var(--lumo-primary-color); color: white; }" +
                    "</style>";

            add(new Html("<div>" + style + "<div class='rendered-markdown'>" + html + "</div></div>"));

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

        @ClientCallable
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
                grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,
                        GridVariant.LUMO_COMPACT,
                        GridVariant.LUMO_WRAP_CELL_CONTENT);
                grid.addColumn(entry -> RendererUtil.formatKeyTitle(entry.getKey())).setHeader("Property").setAutoWidth(true).setFlexGrow(1);
                grid.addComponentColumn(entry -> {
                    JsonNode val = entry.getValue();
                    if (val.isObject() || val.isArray()) {
                        Pre pre = new Pre(val.toPrettyString());
                        pre.getStyle().set("margin", "0").set("font-size", "0.85em").set("white-space", "pre-wrap");
                        return pre;
                    } else if (val.isNumber()) {
                        NumberFormat nf = NumberFormat.getInstance(Locale.US);
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
                closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                dialog.getFooter().add(closeButton);

                dialog.add(grid);
                dialog.open();
            } catch (Exception ignored) {
            }
        }
    }
}
