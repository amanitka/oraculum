package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Locale;

/**
 * Shared styling primitives for executive snapshot and decision-support renderers.
 */
final class SnapshotUiSupport {

    static final String COLOR_GREEN = "#2ecc71";
    static final String COLOR_BLUE = "#3498db";
    static final String COLOR_AMBER = "#ffb300";
    static final String COLOR_ORANGE = "#e67e22";
    static final String COLOR_RED = "#e74c3c";
    static final String COLOR_SLATE = "#95a5a6";

    private SnapshotUiSupport() {
    }

    static Span createStatusBadge(String text, String theme) {
        Span badge = new Span(text != null && !text.isBlank() ? text : "N/A");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(theme != null ? theme : "contrast");
        badge.getStyle()
                .set("font-size", "0.70rem")
                .set("font-weight", "600")
                .set("letter-spacing", "0.04em")
                .set("padding", "2px 8px")
                .set("border-radius", "4px")
                .set("white-space", "nowrap");
        return badge;
    }

    static String resolveDimensionTheme(String value) {
        if (value == null || value.isBlank()) {
            return "contrast";
        }
        String norm = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (norm) {
            case "EXCEPTIONAL", "HIGH", "STRONG", "LOW_HURDLE", "CONSERVATIVE",
                 "ALIGNED", "SUPPORTIVE_UPTREND", "SUPPORTIVE" -> "success";
            case "DEMANDING", "AGGRESSIVE", "CONFLICTING_PEAK_MARGIN_RISK",
                 "DIVERGENT", "PULLBACK_IN_UPTREND", "WARNING" -> "warning";
            case "SPECULATIVE", "WEAK", "VULNERABLE", "DISTRIBUTION_WARNING",
                 "DOWNTREND", "RISK" -> "error";
            default -> "contrast";
        };
    }

    static String formatDimensionLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "N/A";
        }
        return raw.trim().replace('_', ' ');
    }

    static Div createNeutralCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "rgba(var(--lumo-contrast-rgb), 0.03)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "8px")
                .set("padding", "14px 18px")
                .set("box-sizing", "border-box");
        return card;
    }

    static Div createTintedCard(String bgTint, String borderColor) {
        Div card = new Div();
        card.getStyle()
                .set("background", bgTint)
                .set("border", "1px solid " + borderColor)
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("box-sizing", "border-box");
        return card;
    }
}
