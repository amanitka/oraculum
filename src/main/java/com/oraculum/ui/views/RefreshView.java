package com.oraculum.ui.views;

import com.oraculum.harvester.api.HarvesterRequestApi;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "refresh", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Refresh | Oraculum")
public class RefreshView extends VerticalLayout {

    private final HarvesterRequestApi harvesterRequestApi;

    public RefreshView(HarvesterRequestApi harvesterRequestApi) {
        this.harvesterRequestApi = harvesterRequestApi;

        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.LARGE);
        setSizeFull();

        add(createHeader());
        add(createGridSection());
    }

    private static void showSuccess(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void showError(String message) {
        Notification n = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
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
        return header;
    }

    private Component createGridSection() {
        Div grid = new Div();
        grid.getStyle().set("display", "grid");
        grid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(350px, 1fr))");
        grid.getStyle().set("gap", "var(--lumo-space-l)");
        grid.getStyle().set("width", "100%");

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
            showSuccess("Triggered refresh for: " + actionName);
        } catch (Exception ex) {
            showError("Failed to trigger refresh for " + actionName + ": " + ex.getMessage());
        }
    }
}
