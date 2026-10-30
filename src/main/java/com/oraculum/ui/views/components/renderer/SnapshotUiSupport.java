package com.oraculum.ui.views.components.renderer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

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
        String norm = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('/', '_');
        return switch (norm) {
            case "EXCEPTIONAL", "HIGH", "STRONG", "LOW_HURDLE", "CONSERVATIVE",
                 "ALIGNED", "SUPPORTIVE_UPTREND", "SUPPORTIVE", "BULLISH", "POSITIVE" -> "success";
            case "DEMANDING", "AGGRESSIVE", "CONFLICTING_PEAK_MARGIN_RISK",
                 "DIVERGENT", "PULLBACK_IN_UPTREND", "WARNING", "GAAP_NON_GAAP_BASIS_DIFF",
                 "DIFF", "MIXED", "MODERATE", "MIXED_NEGATIVE", "MIXED_POSITIVE" -> "warning";
            case "SPECULATIVE", "WEAK", "VULNERABLE", "DISTRIBUTION_WARNING",
                 "DOWNTREND", "RISK", "MATERIAL_DIVERGENCE", "BEARISH", "NEGATIVE", "HIGH_RISK" -> "error";
            default -> "contrast";
        };
    }

    static String formatDimensionLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "N/A";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        switch (upper) {
            case "GAAP_NON_GAAP_BASIS_DIFF", "GAAP_NON_GAAP" -> {
                return "GAAP / Non-GAAP Basis Diff";
            }
            case "MATERIAL_DIVERGENCE" -> {
                return "Material Divergence";
            }
            case "DIFF" -> {
                return "Diff";
            }
        }
        String[] words = raw.trim().replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            String lower = word.toLowerCase(Locale.ROOT);
            if (!sb.isEmpty() && isMinorWord(lower)) {
                sb.append(lower);
            } else if (lower.equals("tier1")) {
                sb.append("Tier 1");
            } else if (lower.matches("^fy\\d{1,4}$")) {
                sb.append(lower.toUpperCase(Locale.ROOT));
            } else if (lower.equals("pe")) {
                sb.append("P/E");
            } else if (lower.equals("ps")) {
                sb.append("P/S");
            } else if (lower.equals("pct")) {
                sb.append("%");
            } else if (isAcronym(lower)) {
                sb.append(lower.toUpperCase(Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return sb.toString();
    }

    private static boolean isMinorWord(String word) {
        return switch (word) {
            case "in", "of", "and", "vs", "the", "for", "on", "at" -> true;
            default -> false;
        };
    }

    private static boolean isAcronym(String word) {
        return switch (word) {
            case "gaap", "dcf", "fcf", "eps", "roe", "dso", "ttm", "ev", "ebitda", "cagr" -> true;
            default -> false;
        };
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

    static Div createScoreCard(String title, double score, String badgeText, String badgeTheme) {
        Div tile = createNeutralCard();
        tile.getStyle()
                .set("padding", "12px 14px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px");

        tile.add(buildScoreHeaderRow(title, score), buildSlimScoreTrack(score));

        if (badgeText != null && !badgeText.isBlank()) {
            Span badge = createStatusBadge(formatDimensionLabel(badgeText), badgeTheme);
            badge.getStyle().set("align-self", "flex-start");
            tile.add(badge);
        }
        return tile;
    }

    static HorizontalLayout buildScoreHeaderRow(String title, double score) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);

        Span titleSpan = new Span(title.toUpperCase(Locale.ROOT));
        titleSpan.getStyle()
                .set("font-size", "0.72rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.05em")
                .set("color", "var(--lumo-secondary-text-color)");

        Span scoreSpan = new Span(String.format(Locale.US, "%.1f", score));
        scoreSpan.getStyle()
                .set("font-size", "1.05rem")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)");

        header.add(titleSpan, scoreSpan);
        return header;
    }

    static Div buildSlimScoreTrack(double score) {
        Div track = new Div();
        track.setWidthFull();
        track.getStyle()
                .set("height", "4px")
                .set("background", "rgba(255, 255, 255, 0.08)")
                .set("border-radius", "2px")
                .set("overflow", "hidden");

        double pct = Math.clamp(score * 10.0, 0.0, 100.0);
        String fillCol = score >= 7.5 ? COLOR_GREEN : (score >= 4.5 ? COLOR_AMBER : COLOR_RED);

        Div fill = new Div();
        fill.getStyle()
                .set("width", String.format(Locale.US, "%.0f%%", pct))
                .set("height", "100%")
                .set("background", fillCol)
                .set("border-radius", "2px");
        track.add(fill);
        return track;
    }

    static String resolveScoreBadgeLabel(double score) {
        if (score >= 8.5) return "Exceptional";
        if (score >= 7.0) return "Strong";
        if (score >= 5.0) return "Moderate";
        if (score >= 3.5) return "Vulnerable";
        return "High Risk";
    }

    static String resolveScoreBadgeTheme(double score) {
        if (score >= 7.0) return "success";
        if (score >= 5.0) return "contrast";
        if (score >= 3.5) return "warning";
        return "error";
    }

    static String formatMetricLabel(String key) {
        if (key == null || key.isBlank()) return "Metric";
        return switch (key.trim().toLowerCase(Locale.ROOT)) {
            case "share_price" -> "Share Price";
            case "market_cap" -> "Market Cap";
            case "pe_ttm" -> "P/E (TTM)";
            case "ev_ebitda" -> "EV/EBITDA";
            case "ps_ttm" -> "P/S (TTM)";
            case "p_fcf" -> "P/FCF";
            case "fcf_yield" -> "FCF Yield";
            case "forward_pe_fy1", "forward_p_e_fy1" -> "Forward P/E (FY1)";
            case "forward_pe_fy2", "forward_p_e_fy2" -> "Forward P/E (FY2)";
            case "trailing_pe" -> "Trailing P/E";
            case "reverse_dcf_base_implied_growth" -> "Implied 10Y FCF Growth";
            case "reverse_dcf_zone" -> "Reverse DCF Zone";
            case "historical_fcf_cagr" -> "Historical FCF CAGR";
            case "pe_10y_percentile" -> "P/E 10Y Percentile";
            case "ev_ebitda_10y_percentile" -> "EV/EBITDA 10Y Percentile";
            case "ttm_net_margin" -> "TTM Net Margin";
            case "industry_net_margin" -> "Industry Net Margin";
            case "ttm_roe" -> "TTM ROE";
            case "industry_roe" -> "Industry ROE";
            case "revenue_yoy_growth" -> "Revenue YoY Growth";
            case "industry_revenue_yoy_growth" -> "Industry Revenue YoY Growth";
            case "total_inst_value" -> "Total Institutional Value";
            case "tier1_activity" -> "Tier 1 Activity";
            case "put_call_ratio" -> "Put / Call Ratio";
            case "sector_flow_context" -> "Sector Flow Context";
            case "tier1_net_share_change" -> "Tier 1 Net Share Change";
            case "total_institutional_net_share_change" -> "Total Inst. Net Share Change";
            case "eps_reconciliation_status" -> "EPS Reconciliation Status";
            case "flag_eps_reconciliation_divergence" -> "Reconciliation Divergence";
            default -> formatDimensionLabel(key);
        };
    }
}
