package com.oraculum.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;

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
    public static Span statusBadge(String statusName) {
        Span badge = new Span(statusName != null ? statusName : "PENDING");
        String theme = "badge";
        if (statusName != null) {
            if (statusName.contains("COMPLETED")) theme += " success";
            else if (statusName.contains("FAILED")) theme += " error";
            else theme += " warning";
        }
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge Span for outlook values (BULLISH, BEARISH, NEUTRAL).
     */
    public static Span outlookBadge(String outlookName) {
        if (outlookName == null) {
            Span empty = new Span("PENDING");
            empty.getElement().getThemeList().add("badge contrast");
            return empty;
        }
        Span badge = new Span(outlookName);
        String theme = "badge";
        if (outlookName.contains("BULLISH")) theme += " success";
        else if (outlookName.contains("BEARISH")) theme += " error";
        else theme += " contrast";
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge Span for recommendation values (BUY, SELL, HOLD).
     */
    public static Span recommendationBadge(String recName) {
        if (recName == null) {
            Span empty = new Span("PENDING");
            empty.getElement().getThemeList().add("badge contrast");
            return empty;
        }
        Span badge = new Span(recName);
        String theme = "badge";
        if (recName.contains("BUY")) theme += " success primary";
        else if (recName.contains("SELL")) theme += " error";
        else theme += " contrast";
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /**
     * Creates a themed badge for screener signal values (STRONG_BUY, BUY, AVOID, SELL).
     */
    public static Span signalBadge(String signal) {
        Span badge = new Span(signal != null ? signal : "N/A");
        badge.getElement().getThemeList().add("badge");
        if ("STRONG_BUY".equals(signal) || "BUY".equals(signal)) {
            badge.getElement().getThemeList().add("success");
        } else if ("AVOID".equals(signal) || "SELL".equals(signal)) {
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
}
