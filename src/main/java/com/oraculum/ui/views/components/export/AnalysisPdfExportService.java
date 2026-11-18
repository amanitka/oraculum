package com.oraculum.ui.views.components.export;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Generates downloadable PDF executive investment memos from completed company analyses.
 */
@Service
public class AnalysisPdfExportService {

    private static final String CSS_PATH = "pdf/analysis-report.css";
    private static final String FONT_FAMILY = "Inter";

    private record FontSpec(String resourcePath, int weight, FontStyle style) {
    }

    private record LoadedFont(byte[] bytes, int weight, FontStyle style) {
    }

    private static final List<FontSpec> FONT_SPECS = List.of(
            new FontSpec("fonts/Inter-Regular.ttf", 400, FontStyle.NORMAL),
            new FontSpec("fonts/Inter-Italic.ttf", 400, FontStyle.ITALIC),
            new FontSpec("fonts/Inter-SemiBold.ttf", 600, FontStyle.NORMAL),
            new FontSpec("fonts/Inter-Bold.ttf", 700, FontStyle.NORMAL)
    );

    private final AnalysisPdfHtmlBuilder htmlBuilder;
    private final String reportCss;
    private final List<LoadedFont> loadedFonts;

    public AnalysisPdfExportService(AnalysisPdfHtmlBuilder htmlBuilder) {
        this.htmlBuilder = htmlBuilder;
        this.reportCss = readClasspathText(CSS_PATH);
        this.loadedFonts = FONT_SPECS.stream()
                .map(spec -> new LoadedFont(readClasspathBytes(spec.resourcePath()), spec.weight(), spec.style()))
                .toList();
    }

    /**
     * Renders a PDF byte array for a completed {@link CompanyAnalysisViewDto}.
     */
    public byte[] exportPdf(CompanyAnalysisViewDto analysis) {
        if (analysis == null || analysis.getAnalysisResult() == null) {
            throw new IllegalArgumentException("Analysis result is required for PDF export.");
        }
        if (analysis.getError() != null && !analysis.getError().isBlank()) {
            throw new IllegalStateException("Cannot export PDF for a failed analysis.");
        }
        String xhtml = htmlBuilder.buildXhtml(analysis, reportCss);
        return renderXhtmlToPdf(xhtml);
    }

    /**
     * Builds a sanitized download filename such as {@code AAPL_analysis_2026-11-13.pdf}.
     */
    public String buildFileName(CompanyAnalysisViewDto analysis) {
        if (analysis == null || analysis.getTicker() == null || analysis.getTicker().isBlank()) {
            return "analysis_report.pdf";
        }
        String safeTicker = analysis.getTicker().trim().replaceAll("[^A-Za-z0-9._-]", "_").toUpperCase(Locale.ROOT);
        String datePart = analysis.getAnalysisDate() != null ? "_" + analysis.getAnalysisDate() : "";
        return safeTicker + "_analysis" + datePart + ".pdf";
    }

    private byte[] renderXhtmlToPdf(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.useExternalResourceAccessControl((_, _) -> false, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);
            registerFonts(builder);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF report.", e);
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        for (LoadedFont font : loadedFonts) {
            FSSupplier<InputStream> fontSupplier = () -> new ByteArrayInputStream(font.bytes());
            builder.useFont(fontSupplier, FONT_FAMILY, font.weight(), font.style(), true);
        }
    }

    private static String readClasspathText(String path) {
        return new String(readClasspathBytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] readClasspathBytes(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Missing classpath PDF resource: " + path, e);
        }
    }
}
