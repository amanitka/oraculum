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
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MarkdownRenderer {

    private static final String TRACE_CITATIONS_KEY = "CITATIONS";
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\d,\\s\\?\u26A0\uFE0F]+)\\]");
    private final JsonMapper jsonMapper;

    public Component renderMarkdownWithCitations(String strValue, String jsonData) {
        try {
            JsonNode citationsNode = extractCitationsNode(jsonData);
            String processedMd = injectCitations(strValue, citationsNode, this::formatInteractiveCitation);
            String htmlContent = renderMarkdownToHtml(processedMd);

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

    /**
     * Renders markdown to HTML using a caller-provided citation formatter {@code (id, suffix) -> html}.
     */
    public String renderHtmlWithCitations(String markdown, JsonNode citationsNode, BiFunction<String, String, String> citationFormatter) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String processedMd = injectCitations(markdown, citationsNode, citationFormatter);
        return renderMarkdownToHtml(processedMd);
    }

    /**
     * Extracts the {@code CITATIONS} JSON node from {@code analysisData}, or {@code null} when unavailable.
     */
    public JsonNode extractCitationsNode(String jsonData) {
        if (jsonData == null || jsonData.isBlank()) {
            return null;
        }
        try {
            JsonNode rootNode = jsonMapper.readTree(jsonData);
            return rootNode.has(TRACE_CITATIONS_KEY) ? rootNode.get(TRACE_CITATIONS_KEY) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Collects citation IDs referenced in {@code text} that exist in {@code citationsNode}, in appearance order.
     */
    public static List<String> extractCitationIds(String text, JsonNode citationsNode) {
        if (text == null || text.isBlank() || citationsNode == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        Matcher matcher = CITATION_PATTERN.matcher(text);
        while (matcher.find()) {
            for (String part : matcher.group(1).split(",")) {
                String id = part.trim().replaceAll("[^\\d]", "");
                if (!id.isEmpty() && citationsNode.has(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private String renderMarkdownToHtml(String markdown) {
        return HtmlRenderer.builder().build()
                .render(Parser.builder().build().parse(markdown));
    }

    private String injectCitations(String markdown, JsonNode citationsNode, BiFunction<String, String, String> formatter) {
        if (markdown == null || citationsNode == null) {
            return markdown;
        }
        Matcher matcher = CITATION_PATTERN.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = formatCitationGroup(matcher.group(1), citationsNode, formatter);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String formatCitationGroup(String inner, JsonNode citationsNode, BiFunction<String, String, String> formatter) {
        String[] parts = inner.split(",");
        StringBuilder replacement = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                replacement.append(", ");
            }
            replacement.append(formatSingleCitation(parts[i].trim(), citationsNode, formatter));
        }
        return replacement.append("]").toString();
    }

    private String formatSingleCitation(String part, JsonNode citationsNode, BiFunction<String, String, String> formatter) {
        String id = part.replaceAll("[^\\d]", "");
        if (id.isEmpty() || !citationsNode.has(id)) {
            return part;
        }
        String suffix = part.replaceAll("[\\d]", "").trim();
        return formatter.apply(id, suffix);
    }

    private String formatInteractiveCitation(String id, String suffix) {
        boolean isUnverifiable = suffix.contains("?");
        String titleAttr = isUnverifiable ? " title=\"This claim couldn't be automatically verified against the underlying data\"" : "";
        String classAttr = isUnverifiable ? "reference-data-link unverifiable" : "reference-data-link";
        String suffixPart = suffix.isEmpty() ? "" : " " + suffix;
        return "<a href=\"javascript:void(0)\" class=\"" + classAttr + "\" data-reference-id=\""
                + id + "\"" + titleAttr + ">" + id + suffixPart + "</a>";
    }

    public static class CitationMarkdownContainer extends Div {

        private final JsonNode citationsNode;

        public CitationMarkdownContainer(String html, JsonNode citationsNode) {
            this.citationsNode = citationsNode;

            String style = "<style>" +
                    ".rendered-markdown { font-size: 1rem; line-height: 1.7; }" +
                    ".rendered-markdown h1, .rendered-markdown h2, .rendered-markdown h3 { margin-top: 2rem; margin-bottom: 1rem; font-weight: 600; color: var(--lumo-header-text-color); }" +
                    ".rendered-markdown h1 { font-size: 1.35rem; }" +
                    ".rendered-markdown h2 { font-size: 1.15rem; border-bottom: 1px solid var(--lumo-contrast-10pct); padding-bottom: 0.5rem; }" +
                    ".rendered-markdown h3 { font-size: 1.05rem; }" +
                    ".rendered-markdown h4 { font-size: 1rem; font-weight: 600; margin-top: 1.5rem; margin-bottom: 0.5rem; }" +
                    ".rendered-markdown p { margin-bottom: 1.25rem; }" +
                    ".rendered-markdown p:last-child { margin-bottom: 0; }" +
                    ".rendered-markdown strong { font-weight: 600; color: var(--lumo-primary-text-color); }" +
                    ".reference-data-link { font-size: 0.75rem; vertical-align: super; background: var(--lumo-contrast-10pct); border-radius: 4px; padding: 2px 4px; text-decoration: none; color: var(--lumo-primary-text-color); font-weight: bold; margin-left: 2px; transition: all 0.2s ease; }" +
                    ".reference-data-link:hover { background: var(--lumo-primary-color); color: white; }" +
                    ".reference-data-link.unverifiable { background: var(--lumo-warning-color-10pct); color: var(--lumo-warning-text-color); border: 1px solid var(--lumo-warning-color-50pct); }" +
                    ".reference-data-link.unverifiable:hover { background: var(--lumo-warning-color); color: var(--lumo-warning-contrast-color); }" +
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
