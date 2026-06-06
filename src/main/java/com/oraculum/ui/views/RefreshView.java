package com.oraculum.ui.views;

import com.oraculum.harvester.api.HarvesterRequestApi;
import com.oraculum.harvester.api.dto.*;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Route(value = "refresh", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Refresh | Oraculum")
public class RefreshView extends VerticalLayout {

    private static final List<String> VARIANTS = List.of("annual", "quarterly", "ttm");
    private static final List<String> TEMPLATES = List.of("general", "banks", "insurance");

    private final HarvesterRequestApi harvesterRequestApi;

    public RefreshView(HarvesterRequestApi harvesterRequestApi) {
        this.harvesterRequestApi = harvesterRequestApi;

        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.LARGE);
        setSizeFull();

        add(createHeader());
        add(buildMetadataSection());
        add(buildCompanySection());
        add(buildStatementsSection());
    }

    private static void showSuccess(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    // -------------------------------------------------------------------------
    // Sections
    // -------------------------------------------------------------------------

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
        Paragraph caption = new Paragraph("Queue refresh requests to Kafka. The harvester consumes them from the configured request " +
                "topic" + ".");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.NONE);
        header.add(title, caption);
        return header;
    }

    private Component buildMetadataSection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.addClassNames(LumoUtility.Gap.MEDIUM);

        layout.add(createSimpleActionCard("Market Data",
                "Refreshes the list of all supported stock markets.",
                () -> publish(FetchMarketRequest.builder().build())));
        layout.add(createSimpleActionCard("Industry Data",
                "Refreshes the list of all industry classifications.",
                () -> publish(FetchIndustryRequest.builder().build())));

        return layout;
    }

    // -------------------------------------------------------------------------
    // Card Builders
    // -------------------------------------------------------------------------

    private Component buildCompanySection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.addClassNames(LumoUtility.Gap.MEDIUM);

        layout.add(createCompanyRefreshCard());
        layout.add(createSharePriceRefreshCard());

        return layout;
    }

    private Component buildStatementsSection() {
        Div grid = new Div();
        grid.setWidthFull();
        grid.addClassNames(LumoUtility.Display.GRID, LumoUtility.Gap.MEDIUM);
        grid.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(350px, 1fr))");

        grid.add(createStatementRefreshCard("Income Statements", (m, v, t) -> new FetchIncomeStatementRequest(m, v, t)));
        grid.add(createStatementRefreshCard("Balance Sheets", (m, v, t) -> new FetchBalanceSheetRequest(m, v, t)));
        grid.add(createStatementRefreshCard("Cash Flow Statements", (m, v, t) -> new FetchCashFlowStatementRequest(m, v, t)));
        grid.add(createNewsRefreshCard());

        return grid;
    }

    private VerticalLayout createCard(String title) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10);
        card.setWidthFull();

        H4 cardTitle = new H4(title);
        cardTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        card.add(cardTitle);

        return card;
    }

    private Component createSimpleActionCard(String title, String description, Runnable action) {
        VerticalLayout card = createCard(title);

        Paragraph desc = new Paragraph(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.Margin.Top.NONE);

        Button btn = new Button("Queue Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> action.run());

        card.add(desc, btn);
        return card;
    }

    private Component createCompanyRefreshCard() {
        VerticalLayout card = createCard("Company List");

        TextField market = new TextField("Market");
        market.setValue("us");
        market.setWidthFull();

        Button btn = new Button("Queue Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> queueCompanyRefresh(market.getValue()));

        FormLayout form = new FormLayout(market);
        card.add(form, btn);
        return card;
    }

    private Component createSharePriceRefreshCard() {
        VerticalLayout card = createCard("Share Prices");

        TextField market = new TextField("Market");
        market.setValue("us");

        TextField variant = new TextField("Variant");
        variant.setValue("daily");

        Checkbox useFromDate = new Checkbox("Incremental Refresh", true);
        DatePicker fromDate = new DatePicker("From Date");
        fromDate.setValue(LocalDate.now());
        useFromDate.addValueChangeListener(e -> fromDate.setEnabled(e.getValue()));

        IntegerField safetyWindow = new IntegerField("Safety Window (days)");
        safetyWindow.setValue(7);

        Button btn = new Button("Queue Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> queueSharePriceRefresh(market.getValue(),
                variant.getValue(),
                useFromDate.getValue(),
                fromDate.getValue(),
                safetyWindow.getValue()));

        FormLayout form = new FormLayout(market, variant, useFromDate, fromDate, safetyWindow);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("300px", 2));

        card.add(form, btn);
        return card;
    }

    // -------------------------------------------------------------------------
    // Request Dispatchers
    // -------------------------------------------------------------------------

    private Component createStatementRefreshCard(String title, StatementRequestFactory factory) {
        VerticalLayout card = createCard(title);

        TextField market = new TextField("Market");
        market.setValue("us");

        MultiSelectComboBox<String> variants = new MultiSelectComboBox<>("Variants");
        variants.setItems(VARIANTS);
        variants.setValue(Set.copyOf(VARIANTS));

        MultiSelectComboBox<String> templates = new MultiSelectComboBox<>("Templates");
        templates.setItems(TEMPLATES);
        templates.setValue(Set.copyOf(TEMPLATES));

        Button btn = new Button("Queue Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> queueStatementRefresh(market.getValue(), variants.getValue(), templates.getValue(), factory));

        FormLayout form = new FormLayout(market, variants, templates);
        card.add(form, btn);
        return card;
    }

    private Component createNewsRefreshCard() {
        VerticalLayout card = createCard("News & Sentiment");

        Checkbox useFromDate = new Checkbox("Incremental Refresh", true);
        DatePicker fromDate = new DatePicker("From Date");
        fromDate.setValue(LocalDate.now());
        useFromDate.addValueChangeListener(e -> fromDate.setEnabled(e.getValue()));

        Button btn = new Button("Queue Refresh", VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.addClickListener(e -> queueNewsRefresh(useFromDate.getValue(), fromDate.getValue()));

        FormLayout form = new FormLayout(useFromDate, fromDate);
        card.add(form, btn);
        return card;
    }

    private void queueCompanyRefresh(String market) {
        String m = market.strip().toLowerCase();
        if (m.isBlank()) {
            showError("Market is required.");
            return;
        }
        publish(FetchCompanyRequest.builder().market(m).build());
    }

    private void queueSharePriceRefresh(String market, String variant, boolean useDate, LocalDate date, Integer window) {
        String m = market.strip().toLowerCase();
        String v = variant.strip().toLowerCase();
        if (m.isBlank() || v.isBlank()) {
            showError("Market and Variant are required.");
            return;
        }
        String dateStr = useDate && date != null ? date.format(DATE_FMT) : null;
        int win = window != null ? window : 7;
        publish(new FetchSharePricePriceRequest(m, v, dateStr, win));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void queueStatementRefresh(String market, Set<String> variants, Set<String> templates, StatementRequestFactory factory) {
        String m = market.strip().toLowerCase();
        if (m.isBlank()) {
            showError("Market is required.");
            return;
        }
        if (variants.isEmpty()) {
            showError("Select at least one variant.");
            return;
        }
        if (templates.isEmpty()) {
            showError("Select at least one template.");
            return;
        }

        for (String v : variants) {
            publish(factory.create(m, v, List.copyOf(templates)));
        }
    }

    private void queueNewsRefresh(boolean useDate, LocalDate date) {
        String timeFrom = null;
        if (useDate && date != null) {
            timeFrom = date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
        }
        publish(FetchNewsRequest.builder().timeFrom(timeFrom).build());
    }

    private void publish(HarvesterRequest request) {
        try {
            harvesterRequestApi.publishRequest(request);
            showSuccess("Published `" + request.getRequestType() + "` [" + request.getCorrelationId() + "]");
        } catch (Exception ex) {
            showError("Failed to publish: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface StatementRequestFactory {
        HarvesterRequest create(String market, String variant, List<String> templates);
    }
}
