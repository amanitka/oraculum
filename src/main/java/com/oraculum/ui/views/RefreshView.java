package com.oraculum.ui.views;

import com.oraculum.database.api.event.RefreshMaterializedViewsEvent;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.context.ApplicationEventPublisher;

@Route(value = "refresh", layout = MainLayout.class)
@PageTitle("Refresh | Oraculum")
public class RefreshView extends VerticalLayout {

    private static final String TILE_STYLE =
            "background: var(--lumo-contrast-5pct);" +
                    "border: 1px solid var(--lumo-contrast-10pct);" +
                    "border-radius: var(--lumo-border-radius-l);" +
                    "padding: var(--lumo-space-l)";

    private final HarvesterBatchApi harvesterBatchApi;
    private final ApplicationEventPublisher eventPublisher;

    public RefreshView(HarvesterBatchApi harvesterBatchApi, ApplicationEventPublisher eventPublisher) {
        this.harvesterBatchApi = harvesterBatchApi;
        this.eventPublisher = eventPublisher;

        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setWidthFull();
        contentArea.setPadding(false);
        contentArea.addClassNames(LumoUtility.Gap.LARGE);

        contentArea.add(createHeader(), createGridSection());
        add(contentArea);
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private Component createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        H3 title = new H3("Data Refresh Console");
        title.getStyle().set("margin-top", "2rem");
        title.getStyle().set("margin-bottom", "1rem");

        Paragraph caption = new Paragraph("Trigger data harvesting and refresh operations across all markets and sources.");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.NONE);

        header.add(title, caption);
        return header;
    }

    // ── Tile Grid ──────────────────────────────────────────────────────────

    private Component createGridSection() {
        Div grid = new Div();
        grid.getStyle().set("display", "grid");
        grid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))");
        grid.getStyle().set("gap", "var(--lumo-space-l)");
        grid.setWidthFull();

        grid.add(createTile("Market Data",
                "Refreshes the list of all supported stock markets.",
                harvesterBatchApi::refreshMarket));

        grid.add(createTile("Industry Data",
                "Refreshes the list of all industry classifications.",
                harvesterBatchApi::refreshIndustry));

        grid.add(createTile("Company List",
                "Refreshes the list of companies across all supported markets.",
                harvesterBatchApi::refreshCompany));

        grid.add(createTile("Fundamentals",
                "Refreshes Income Statements, Balance Sheets, and Cash Flow Statements for all companies.",
                harvesterBatchApi::refreshFundamentals));

        grid.add(createSharePriceTile());

        grid.add(createTile("News & Sentiment",
                "Refreshes recent news articles and sentiment data.",
                harvesterBatchApi::refreshNews));

        grid.add(createTile("Insider Transactions",
                "Refreshes daily insider trading transactions from OpenInsider.",
                harvesterBatchApi::refreshInsiderTransactions));

        grid.add(createTile("Materialized Views",
                "Rebuilds all materialized views and refreshes screener cache. Runs asynchronously.",
                () -> eventPublisher.publishEvent(new RefreshMaterializedViewsEvent())));

        return grid;
    }

    // ── Simple Tile ────────────────────────────────────────────────────────

    private Component createTile(String title, String description, Runnable action) {
        Div tile = new Div();
        tile.getStyle().set("cssText", TILE_STYLE);

        HorizontalLayout header = tileHeader(title, createRefreshButton(title, action));

        Span desc = new Span(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        desc.getStyle().set("margin-top", "var(--lumo-space-s)").set("display", "block");

        tile.add(header, desc);
        return tile;
    }

    // ── Share Price Tile ───────────────────────────────────────────────────

    private Component createSharePriceTile() {
        Div tile = new Div();
        tile.getStyle().set("cssText", TILE_STYLE);

        Checkbox incremental = new Checkbox("Incremental Refresh", true);
        DatePicker fromDate = new DatePicker();
        fromDate.setPlaceholder("Auto (last trade date)");

        incremental.addValueChangeListener(e -> {
            fromDate.setEnabled(e.getValue());
            if (!e.getValue()) fromDate.setValue(null);
        });

        Button btn = createRefreshButton("Share Prices",
                () -> harvesterBatchApi.refreshSharePrices(incremental.getValue(), fromDate.getValue()));

        HorizontalLayout header = tileHeader("Share Prices", btn);

        Span desc = new Span("Refreshes historical daily share prices for all companies.");
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        desc.getStyle().set("margin-top", "var(--lumo-space-s)").set("display", "block");

        HorizontalLayout controls = new HorizontalLayout(incremental, fromDate);
        controls.setAlignItems(Alignment.CENTER);
        controls.addClassNames(LumoUtility.Gap.MEDIUM);
        controls.getStyle().set("margin-top", "var(--lumo-space-m)");

        tile.add(header, desc, controls);
        return tile;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private HorizontalLayout tileHeader(String title, Button button) {
        H4 heading = new H4(title);
        heading.addClassNames(LumoUtility.Margin.NONE);

        HorizontalLayout row = new HorizontalLayout(heading, button);
        row.setWidthFull();
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return row;
    }

    private Button createRefreshButton(String actionName, Runnable action) {
        Button btn = new Button("Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.addClickListener(_ -> executeRefresh(actionName, action));
        return btn;
    }

    private void executeRefresh(String actionName, Runnable refreshAction) {
        try {
            refreshAction.run();
            ViewHelper.showSuccess("Triggered refresh for: " + actionName);
        } catch (Exception ex) {
            ViewHelper.showError("Failed to trigger refresh for " + actionName + ": " + ex.getMessage());
        }
    }
}