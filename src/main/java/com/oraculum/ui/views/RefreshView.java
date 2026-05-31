package com.oraculum.ui.views;

import com.oraculum.ui.MainLayout;
import com.oraculum.ui.request.HarvesterRequest;
import com.oraculum.ui.service.RefreshRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Route(value = "refresh", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Refresh | Oraculum")
public class RefreshView extends VerticalLayout {

    private static final List<String> VARIANTS = List.of("annual", "quarterly", "ttm");
    private static final List<String> TEMPLATES = List.of("general", "banks", "insurance");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RefreshRequestService refreshService;

    public RefreshView(RefreshRequestService refreshService) {
        this.refreshService = refreshService;

        addClassNames(LumoUtility.Padding.MEDIUM);
        setSizeFull();

        Paragraph caption = new Paragraph(
                "Queue refresh requests to Kafka. The harvester consumes them from the configured request topic.");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM);

        Map<Tab, Div> tabContent = new LinkedHashMap<>();
        tabContent.put(new Tab("Metadata"),        buildMetadataTab());
        tabContent.put(new Tab("Company"),           buildCompanyTab());
        tabContent.put(new Tab("Share Price"),      buildSharePriceTab());
        tabContent.put(new Tab("News & Sentiment"), buildNewsTab());
        tabContent.put(new Tab("Income Statement"), buildStatementTab((m, v, t) -> new HarvesterRequest.FetchIncomeStatement(m, v, t)));
        tabContent.put(new Tab("Balance Sheet"),    buildStatementTab((m, v, t) -> new HarvesterRequest.FetchBalanceSheet(m, v, t)));
        tabContent.put(new Tab("Cash Flow"),        buildStatementTab((m, v, t) -> new HarvesterRequest.FetchCashFlowStatement(m, v, t)));

        Tabs tabs = new Tabs(tabContent.keySet().toArray(new Tab[0]));
        tabs.setWidthFull();

        Div pages = new Div();
        pages.setSizeFull();
        tabContent.values().forEach(page -> {
            page.setVisible(false);
            pages.add(page);
        });
        tabContent.values().iterator().next().setVisible(true);

        tabs.addSelectedChangeListener(e -> {
            tabContent.values().forEach(page -> page.setVisible(false));
            tabContent.get(e.getSelectedTab()).setVisible(true);
        });

        setFlexGrow(1, pages);
        add(caption, tabs, pages);
    }

    // -------------------------------------------------------------------------
    // Metadata tab
    // -------------------------------------------------------------------------

    private Div buildMetadataTab() {
        Div content = new Div();
        content.addClassNames(LumoUtility.Padding.MEDIUM);

        H3 title = new H3("Markets & Industries");

        Button marketBtn = new Button("Queue market refresh");
        marketBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        marketBtn.addClickListener(e -> publish(new HarvesterRequest.FetchMarket()));

        Button industryBtn = new Button("Queue industry refresh");
        industryBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        industryBtn.addClickListener(e -> publish(new HarvesterRequest.FetchIndustry()));

        HorizontalLayout buttons = new HorizontalLayout(marketBtn, industryBtn);
        buttons.addClassNames(LumoUtility.Gap.MEDIUM);

        content.add(title, buttons);
        return content;
    }

    // -------------------------------------------------------------------------
    // Company tab
    // -------------------------------------------------------------------------

    private Div buildCompanyTab() {
        Div content = new Div();
        content.addClassNames(LumoUtility.Padding.MEDIUM);

        H3 title = new H3("Company Refresh");

        TextField market = new TextField("Market");
        market.setValue("us");
        market.setWidth("200px");

        Button submit = new Button("Queue company refresh");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(e -> {
            String m = market.getValue().strip().toLowerCase();
            if (m.isBlank()) {
                showError("Market is required.");
                return;
            }
            publish(new HarvesterRequest.FetchCompany(m));
        });

        FormLayout form = new FormLayout(market);
        content.add(title, form, submit);
        return content;
    }

    // -------------------------------------------------------------------------
    // Share Price tab
    // -------------------------------------------------------------------------

    private Div buildSharePriceTab() {
        Div content = new Div();
        content.addClassNames(LumoUtility.Padding.MEDIUM);

        H3 title = new H3("Share Price Refresh");

        TextField market = new TextField("Market");
        market.setValue("us");

        TextField variant = new TextField("Variant");
        variant.setValue("daily");
        variant.setHelperText("SimFin variant name, e.g. 'daily'");

        Checkbox useFromDate = new Checkbox("Use incremental from_date", true);

        DatePicker fromDate = new DatePicker("From date");
        fromDate.setValue(LocalDate.now());
        fromDate.setEnabled(true);
        useFromDate.addValueChangeListener(e -> fromDate.setEnabled(e.getValue()));

        IntegerField safetyWindowDays = new IntegerField("Safety window days");
        safetyWindowDays.setValue(7);
        safetyWindowDays.setMin(0);
        safetyWindowDays.setStepButtonsVisible(true);

        Button submit = new Button("Queue share-price refresh");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(e -> {
            String m = market.getValue().strip().toLowerCase();
            String v = variant.getValue().strip().toLowerCase();
            if (m.isBlank() || v.isBlank()) {
                showError("Market and Variant are required.");
                return;
            }
            int window = safetyWindowDays.getValue() != null ? safetyWindowDays.getValue() : 7;
            String dateStr = useFromDate.getValue() && fromDate.getValue() != null
                    ? fromDate.getValue().format(DATE_FMT) : null;
            publish(new HarvesterRequest.FetchSharePrice(m, v, dateStr, window));
        });

        FormLayout form = new FormLayout(market, variant, useFromDate, fromDate, safetyWindowDays);
        content.add(title, form, submit);
        return content;
    }

    // -------------------------------------------------------------------------
    // News tab
    // -------------------------------------------------------------------------

    private Div buildNewsTab() {
        Div content = new Div();
        content.addClassNames(LumoUtility.Padding.MEDIUM);

        H3 title = new H3("News & Sentiment Refresh");

        Checkbox useFromDate = new Checkbox("Use incremental from_date", true);

        DatePicker fromDate = new DatePicker("From date");
        fromDate.setValue(LocalDate.now());
        fromDate.setEnabled(true);
        useFromDate.addValueChangeListener(e -> fromDate.setEnabled(e.getValue()));

        Button submit = new Button("Queue news refresh");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(e -> {
            String timeFrom = null;
            if (useFromDate.getValue() && fromDate.getValue() != null) {
                timeFrom = fromDate.getValue().atStartOfDay()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
            }
            publish(new HarvesterRequest.FetchNews(timeFrom));
        });

        FormLayout form = new FormLayout(useFromDate, fromDate);
        content.add(title, form, submit);
        return content;
    }

    // -------------------------------------------------------------------------
    // Generic statement tab (income / balance sheet / cash flow)
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface StatementRequestFactory {
        HarvesterRequest create(String market, String variant, List<String> templates);
    }

    private Div buildStatementTab(StatementRequestFactory factory) {
        Div content = new Div();
        content.addClassNames(LumoUtility.Padding.MEDIUM);

        TextField market = new TextField("Market");
        market.setValue("us");

        MultiSelectComboBox<String> variants = new MultiSelectComboBox<>("Variants");
        variants.setItems(VARIANTS);
        variants.setValue(Set.copyOf(VARIANTS));

        MultiSelectComboBox<String> templates = new MultiSelectComboBox<>("Templates");
        templates.setItems(TEMPLATES);
        templates.setValue(Set.copyOf(TEMPLATES));

        Button submit = new Button("Queue refresh");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(e -> {
            String m = market.getValue().strip().toLowerCase();
            if (m.isBlank()) { showError("Market is required."); return; }
            if (variants.getValue().isEmpty()) { showError("Select at least one variant."); return; }
            if (templates.getValue().isEmpty()) { showError("Select at least one template."); return; }

            for (String v : variants.getValue()) {
                publish(factory.create(m, v, List.copyOf(templates.getValue())));
            }
        });

        FormLayout form = new FormLayout(market, variants, templates);
        content.add(form, submit);
        return content;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void publish(HarvesterRequest request) {
        try {
            refreshService.publish(request);
            showSuccess("Published `" + request.getRequestType() + "` [" + request.getCorrelationId() + "]");
        } catch (Exception ex) {
            showError("Failed to publish: " + ex.getMessage());
        }
    }

    private static void showSuccess(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void showError(String message) {
        Notification n = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}