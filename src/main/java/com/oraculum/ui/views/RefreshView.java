package com.oraculum.ui.views;

import com.oraculum.harvester.api.HarvesterRequestApi;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDate;

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
        grid.setWidthFull();
        grid.addClassNames(LumoUtility.Display.GRID);
        grid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))");
        grid.getStyle().set("gap", "var(--lumo-space-m)");

        grid.add(createSimpleActionCard("Market Data",
                "Refreshes the list of all supported stock markets.",
                harvesterRequestApi::refreshMarket));

        grid.add(createSimpleActionCard("Industry Data",
                "Refreshes the list of all industry classifications.",
                harvesterRequestApi::refreshIndustry));

        grid.add(createSimpleActionCard("Company List",
                "Refreshes the list of companies across all supported markets.",
                harvesterRequestApi::refreshCompany));

        grid.add(createSimpleActionCard("Fundamentals",
                "Refreshes Income Statements, Balance Sheets, and Cash Flow Statements for all companies.",
                harvesterRequestApi::refreshFundamentals));

        grid.add(createSharePriceRefreshCard());

        grid.add(createSimpleActionCard("News & Sentiment",
                "Refreshes recent news articles and sentiment data.",
                harvesterRequestApi::refreshNews));

        return grid;
    }

    private Div createCard(String title) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.XSMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10,
                "refresh-card"
        );
        card.setWidthFull();

        H4 cardTitle = new H4(title);
        cardTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        card.add(cardTitle);

        return card;
    }

    private Component createSimpleActionCard(String title, String description, Runnable action) {
        Div card = createCard(title);

        Paragraph desc = new Paragraph(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.Margin.Top.NONE);

        Button btn = new Button("Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClassNames(LumoUtility.Margin.Top.AUTO); // mt-auto

        btn.addClickListener(e -> executeRefresh(title, action));

        card.add(desc, btn);
        return card;
    }

    private Component createSharePriceRefreshCard() {
        Div card = createCard("Share Prices");

        Paragraph desc = new Paragraph("Refreshes historical daily share prices for all companies.");
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.SMALL, LumoUtility.Margin.Top.NONE);

        Checkbox incremental = new Checkbox("Incremental Refresh", true);
        DatePicker fromDate = new DatePicker("From Date");
        fromDate.setPlaceholder("Auto (last trade date)");

        incremental.addValueChangeListener(e -> {
            fromDate.setEnabled(e.getValue());
            if (!e.getValue()) {
                fromDate.setValue(null);
            }
        });

        FormLayout form = new FormLayout(incremental, fromDate);
        form.setWidthFull();
        form.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        Button btn = new Button("Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClassNames(LumoUtility.Margin.Top.AUTO); // mt-auto
        btn.addClickListener(e -> executeRefresh("Share Prices", () ->
                harvesterRequestApi.refreshSharePrices(incremental.getValue(), fromDate.getValue())
        ));

        card.add(desc, form, btn);
        return card;
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
