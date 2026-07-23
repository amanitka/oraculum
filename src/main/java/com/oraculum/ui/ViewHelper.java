package com.oraculum.ui;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.domain.AnalysisStatus;

import com.oraculum.company.api.*;
import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.api.domain.NewsSentimentLabel;
import com.oraculum.company.api.domain.ScreenerSignal;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.ui.components.CompanyOverviewComponent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.function.Function;

import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Shared UI utilities for Oraculum views.
 * Centralises notification handling, filter fields, badge rendering, and common layout patterns.
 */
public final class ViewHelper {

    private ViewHelper() {
    }

    // ── Notifications ──────────────────────────────────────────────────────

    public static void showError(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    // ── Filter Fields ──────────────────────────────────────────────────────

    /**
     * Creates a standard filter TextField used in grid header rows.
     */
    public static TextField createFilterField(String placeholder, Consumer<String> filterAction) {
        TextField filter = new TextField();
        filter.setPlaceholder("Filter " + placeholder);
        filter.setClearButtonVisible(true);
        filter.setWidthFull();
        filter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> filterAction.accept(e.getValue()));
        return filter;
    }

    /**
     * Adds a filter TextField to a grid's header row for the given column key.
     */
    public static <T> void addFilter(Grid<T> grid, HeaderRow filterRow, String columnKey,
                                     String placeholder, Consumer<String> action) {
        filterRow.getCell(grid.getColumnByKey(columnKey))
                .setComponent(createFilterField(placeholder, action));
    }

    // ── Badge Rendering ────────────────────────────────────────────────────

    /**
     * Creates a themed badge Span for status values (COMPLETED, FAILED, etc.).
     */
    public static Span statusBadge(AnalysisStatus status) {
        String text = status != null ? status.getDisplayName() : "Pending";
        Span badge = new Span(text);
        String theme = "badge";
        if (status != null) {
            if (status == AnalysisStatus.COMPLETED) theme += " success";
            else if (status == AnalysisStatus.FAILED) theme += " error";
            else if (status == AnalysisStatus.RUNNING) theme += " warning";
            else theme += " contrast";
        } else {
            theme += " contrast";
        }
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge Span for outlook values (BULLISH, BEARISH, NEUTRAL).
     */
    public static Span outlookBadge(AnalysisOutlook outlook) {
        String text = outlook != null ? outlook.getDisplayName() : "Pending";
        Span badge = new Span(text);
        String theme = "badge";
        if (outlook != null) {
            if (outlook == AnalysisOutlook.BULLISH) theme += " success";
            else if (outlook == AnalysisOutlook.BEARISH) theme += " error";
            else theme += " contrast";
        } else {
            theme += " contrast";
        }
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge Span for recommendation values (BUY, SELL, HOLD, NEUTRAL).
     */
    public static Span recommendationBadge(AnalysisRecommendation rec) {
        String text = rec != null ? rec.getDisplayName() : "Pending";
        Span badge = new Span(text);
        String theme = "badge";
        if (rec != null) {
            if (rec == AnalysisRecommendation.BUY) theme += " success primary";
            else if (rec == AnalysisRecommendation.SELL) theme += " error";
            else theme += " contrast";
        } else {
            theme += " contrast";
        }
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge for screener signal values (STRONG_BUY, BUY, AVOID, SELL).
     */
    public static Span signalBadge(String signalCode) {
        ScreenerSignal signal = ScreenerSignal.fromCode(signalCode);
        String text = signal != null ? signal.getDisplayName() : (signalCode != null ? signalCode : "N/A");
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge");
        if (signal == ScreenerSignal.STRONG_BUY || signal == ScreenerSignal.BUY) {
            badge.getElement().getThemeList().add("success");
        } else if (signal == ScreenerSignal.AVOID) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    /**
     * Creates a themed badge for company size values (LARGE, MID, SMALL, MICRO).
     */
    public static Span sizeBadge(CompanySize size) {
        String text = size != null ? size.getDisplayName() : "N/A";
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge");
        if (size == CompanySize.LARGE) {
            badge.getElement().getThemeList().add("primary");
        } else if (size == CompanySize.MID) {
            badge.getElement().getThemeList().add("success");
        } else if (size == CompanySize.SMALL) {
            badge.getElement().getThemeList().add("warning");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    /**
     * Creates a themed badge for news sentiment values (BULLISH, SOMEWHAT_BULLISH, NEUTRAL, etc.).
     */
    public static Span newsSentimentBadge(String labelCode, Float score) {
        NewsSentimentLabel label = NewsSentimentLabel.fromCode(labelCode);
        String text = label != null ? label.getDisplayName() : "No News";
        if (score != null) {
            text += String.format(Locale.US, " (%+.2f)", score);
        }
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge");
        if (label == NewsSentimentLabel.BULLISH || label == NewsSentimentLabel.SOMEWHAT_BULLISH) {
            badge.getElement().getThemeList().add("success");
        } else if (label == NewsSentimentLabel.BEARISH || label == NewsSentimentLabel.SOMEWHAT_BEARISH) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    /**
     * Creates a color-coded quality score Span (green ≥ 80, red < 40).
     */
    public static Span qualitySpan(Float score) {
        Span span = new Span(score != null ? String.format(Locale.US, "%.1f", score) : "-");
        if (score != null) {
            if (score >= 80) span.addClassName(LumoUtility.TextColor.SUCCESS);
            else if (score < 40) span.addClassName(LumoUtility.TextColor.ERROR);
        }
        span.addClassName(LumoUtility.FontWeight.BOLD);
        return span;
    }

    /**
     * Creates a color-coded price change percentage Span (+2.5% green, -1.2% red).
     */
    public static Span priceChangeSpan(Float changePct) {
        if (changePct == null) {
            return new Span("-");
        }
        Span span = new Span(String.format(Locale.US, "%+.2f%%", changePct));
        if (changePct > 0) {
            span.addClassName(LumoUtility.TextColor.SUCCESS);
        } else if (changePct < 0) {
            span.addClassName(LumoUtility.TextColor.ERROR);
        } else {
            span.addClassName(LumoUtility.TextColor.SECONDARY);
        }
        span.addClassName(LumoUtility.FontWeight.MEDIUM);
        return span;
    }

    /**
     * Returns a comparator that extracts a key with nullsFirst natural order, so that when Vaadin
     * reverses the comparator for Descending sort (largest to smallest), nulls are placed at the very bottom.
     */
    public static <T, U extends Comparable<? super U>> Comparator<T> nullsAlwaysLast(Function<T, U> keyExtractor) {
        return Comparator.comparing(keyExtractor, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    // ── Common Layout ──────────────────────────────────────────────────────

    /**
     * Wraps a component in a full-size Div with the screener-card class.
     */
    public static Div wrapInCard(Component content) {
        Div card = new Div();
        card.addClassName("screener-card");
        card.setSizeFull();
        card.add(content);
        return card;
    }

    /**
     * Creates a secondary-styled empty placeholder Div.
     */
    public static Div emptyPlaceholder(String text) {
        Div empty = new Div();
        empty.setText(text);
        empty.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Padding.LARGE);
        return empty;
    }

    // ── String matching ────────────────────────────────────────────────────

    /**
     * Case-insensitive substring match used by grid filters.
     */
    public static boolean matches(String value, String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty()
                || (value != null && value.toLowerCase().contains(searchTerm.toLowerCase()));
    }

    // ── Components ─────────────────────────────────────────────────────────

    /**
     * Creates a standard button that opens the Company Overview Tearsheet dialog.
     *
     * @param showText if true, shows "View" text; if false, shows icon-only button with tooltip
     */
    public static Button createCompanyDetailsButton(CompanyMetadataApi companyMetadataApi, CompanyFinancialDataApi companyFinancialDataApi, CompanySharePriceApi companySharePriceApi, CompanyNewsApi companyNewsApi, CompanyInsiderTransactionApi companyInsiderTransactionApi, CompanyValuationApi companyValuationApi, ObjectMapper objectMapper, int companyId, boolean showText) {
        Button btn;
        if (showText) {
            btn = new Button("View", VaadinIcon.CHART_LINE.create());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        } else {
            btn = new Button(VaadinIcon.CHART_LINE.create());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            btn.setAriaLabel("View company tearsheet");
            btn.setTooltipText("View company tearsheet");
        }
        btn.addClickListener(_ -> {
            CompanyDto company = companyMetadataApi.getCompanyById(companyId);
            if (company != null) {
                Dialog dialog = new Dialog();
                dialog.setWidth("90vw");
                dialog.setHeight("90vh");
                dialog.add(new CompanyOverviewComponent(companyFinancialDataApi, companySharePriceApi, companyNewsApi, companyInsiderTransactionApi, companyValuationApi, company, objectMapper));

                Button closeBtn = new Button("Close", _ -> dialog.close());
                closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                dialog.getFooter().add(closeBtn);

                // Trigger window resize when dialog completes opening animation so external JS charts (ApexCharts) resize correctly
                dialog.addOpenedChangeListener(e -> {
                    if (e.isOpened()) {
                        UI.getCurrent().getPage().executeJs("setTimeout(() => window.dispatchEvent(new Event('resize')), 250);");
                    }
                });

                dialog.open();
            } else {
                showError("Company details not found.");
            }
        });
        return btn;
    }

    /**
     * Resolves the proper currency symbol based on currency code and market context.
     */
    public static String getCurrencySymbol(String currencyCode, String market) {
        if (currencyCode != null && !currencyCode.isBlank()) {
            String upper = currencyCode.toUpperCase();
            switch (upper) {
                case "EUR" -> {
                    return "€";
                }
                case "CAD" -> {
                    return "CA$";
                }
                case "CNY", "RMB", "HKD", "JPY" -> {
                    return "¥";
                }
                case "GBP" -> {
                    return "£";
                }
            }
            if (!upper.equals("USD")) return upper + " ";
        }

        if (market != null) {
            String upperMkt = market.toUpperCase();
            switch (upperMkt) {
                case "DE", "EU" -> {
                    return "€";
                }
                case "CA" -> {
                    return "CA$";
                }
                case "CN", "HK", "JP" -> {
                    return "¥";
                }
                case "UK", "GB" -> {
                    return "£";
                }
            }
        }

        return "$";
    }

    public static String getCurrencySymbol(String currencyCode) {
        return getCurrencySymbol(currencyCode, null);
    }
}
