package com.oraculum.ui.views;

import com.oraculum.database.api.event.RefreshMaterializedViewsEvent;
import com.oraculum.harvester.api.HarvesterRequestApi;
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
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.context.ApplicationEventPublisher;


@Route(value = "refresh", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Refresh | Oraculum")
public class RefreshView extends VerticalLayout {

    private final HarvesterRequestApi harvesterRequestApi;
    private final ApplicationEventPublisher eventPublisher;

    public RefreshView(HarvesterRequestApi harvesterRequestApi, ApplicationEventPublisher eventPublisher) {
        this.harvesterRequestApi = harvesterRequestApi;
        this.eventPublisher = eventPublisher;

        // 1. Configure the main view to take full space and center its contents
        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER); // This perfectly centers the content area below

        // 2. Create a constrained wrapper for the header and grid
        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setWidthFull();
        contentArea.setMaxWidth("1100px"); // Locks the width for the tile grid
        contentArea.setPadding(false);
        contentArea.addClassNames(LumoUtility.Gap.LARGE);

        // Add header and grid to the centered wrapper
        contentArea.add(createHeader(), createGridSection());

        // Add the wrapper to the view
        add(contentArea);
    }



    private Component createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        H3 title = new H3("Data Refresh Console");
        title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.XSMALL);

        Paragraph caption = new Paragraph("Trigger data harvesting and refresh operations across all markets and sources.");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.NONE);

        header.add(title, caption);
        // Note: Removed center alignment here. Left-aligned headers look cleaner above tile grids.
        return header;
    }

    private Component createGridSection() {
        Div grid = new Div();
        grid.getStyle().set("display", "grid");
        grid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(320px, 1fr))");
        grid.getStyle().set("gap", "var(--lumo-space-l)");
        grid.setWidthFull(); // Takes full width of the 1100px contentArea
        // Note: Removed margin: 0 auto. The parent VerticalLayout handles centering now.

        grid.add(createSimpleActionTile("Market Data",
                "Refreshes the list of all supported stock markets.",
                harvesterRequestApi::refreshMarket));

        grid.add(createSimpleActionTile("Industry Data",
                "Refreshes the list of all industry classifications.",
                harvesterRequestApi::refreshIndustry));

        grid.add(createSimpleActionTile("Company List",
                "Refreshes the list of companies across all supported markets.",
                harvesterRequestApi::refreshCompany));

        grid.add(createSimpleActionTile("Fundamentals",
                "Refreshes Income Statements, Balance Sheets, and Cash Flow Statements for all companies.",
                harvesterRequestApi::refreshFundamentals));

        grid.add(createSharePriceRefreshTile());

        grid.add(createSimpleActionTile("Materialized Views",
                "Rebuilds all materialized views and refreshes screener cache. Runs asynchronously.",
                () -> eventPublisher.publishEvent(new RefreshMaterializedViewsEvent())));

        grid.add(createSimpleActionTile("News & Sentiment",
                "Refreshes recent news articles and sentiment data.",
                harvesterRequestApi::refreshNews));

        return grid;
    }

    private Component createSimpleActionTile(String title, String description, Runnable action) {
        VerticalLayout tile = new VerticalLayout();
        tile.addClassNames(LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN);

        H4 tileTitle = new H4(title);
        tileTitle.addClassNames(LumoUtility.Margin.NONE);
        Span desc = new Span(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.SMALL);

        HorizontalLayout buttonWrapper = new HorizontalLayout();
        buttonWrapper.setWidthFull();
        buttonWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonWrapper.addClassNames(LumoUtility.Margin.Top.AUTO);

        Button btn = new Button("Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> executeRefresh(title, action));

        buttonWrapper.add(btn);
        tile.add(tileTitle, desc, buttonWrapper);
        return tile;
    }

    private Component createSharePriceRefreshTile() {
        VerticalLayout tile = new VerticalLayout();
        tile.addClassNames(LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN);

        H4 tileTitle = new H4("Share Prices");
        tileTitle.addClassNames(LumoUtility.Margin.NONE);
        Span desc = new Span("Refreshes historical daily share prices for all companies.");
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.SMALL);

        Checkbox incremental = new Checkbox("Incremental Refresh", true);
        DatePicker fromDate = new DatePicker();
        fromDate.setPlaceholder("Auto (last trade date)");

        incremental.addValueChangeListener(e -> {
            fromDate.setEnabled(e.getValue());
            if (!e.getValue()) {
                fromDate.setValue(null);
            }
        });

        HorizontalLayout controls = new HorizontalLayout(incremental, fromDate);
        controls.setAlignItems(Alignment.CENTER);
        controls.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Gap.MEDIUM);

        HorizontalLayout buttonWrapper = new HorizontalLayout();
        buttonWrapper.setWidthFull();
        buttonWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonWrapper.addClassNames(LumoUtility.Margin.Top.AUTO);

        Button btn = new Button("Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> executeRefresh("Share Prices",
                () -> harvesterRequestApi.refreshSharePrices(incremental.getValue(), fromDate.getValue())));

        buttonWrapper.add(btn);
        tile.add(tileTitle, desc, controls, buttonWrapper);
        return tile;
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