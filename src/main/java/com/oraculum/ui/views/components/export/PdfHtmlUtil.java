package com.oraculum.ui.views.components.export;

import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.nodes.Entities.EscapeMode;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

/**
 * Stateless HTML/XHTML formatting helpers for PDF report generation.
 */
final class PdfHtmlUtil {

    private PdfHtmlUtil() {
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String renderMarkdownXhtml(MarkdownRenderer markdownRenderer, String markdown, JsonNode citationsNode) {
        if (isBlank(markdown)) {
            return "";
        }
        String rawHtml = markdownRenderer.renderHtmlWithCitations(
                markdown, citationsNode, AnalysisPdfCitationHelper::formatPdfCitation);
        Document doc = Jsoup.parseBodyFragment(rawHtml);
        configureXhtmlSettings(doc.outputSettings());
        return doc.body().html();
    }

    static String wrapXhtmlDocument(String title, String css, String bodyHtml) {
        String raw = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
                + "<meta charset=\"UTF-8\"/>"
                + "<title>" + escape(title) + "</title>"
                + "<style>" + css + "</style>"
                + "</head><body>" + bodyHtml + "</body></html>";
        Document doc = Jsoup.parse(raw);
        configureXhtmlSettings(doc.outputSettings());
        return doc.html();
    }

    static String badgeCssClass(String theme) {
        if (theme == null) {
            return "badge badge-contrast";
        }
        return switch (theme) {
            case "success" -> "badge badge-success";
            case "warning" -> "badge badge-warning";
            case "error" -> "badge badge-error";
            case "info" -> "badge badge-info";
            default -> "badge badge-contrast";
        };
    }

    private static void configureXhtmlSettings(OutputSettings settings) {
        settings.syntax(Syntax.xml)
                .escapeMode(EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
    }
}
