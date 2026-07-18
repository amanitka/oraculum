package com.oraculum.ui.components;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.database.api.event.RefreshMaterializedViewsEvent;
import com.oraculum.analyst.api.event.ProcessPendingSecDocumentsEvent;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataRefreshComponent extends VerticalLayout {

    public DataRefreshComponent(HarvesterBatchApi harvesterBatchApi,
                                ApplicationEventPublisher eventPublisher,
                                CompanyMetadataApi companyMetadataApi) {

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        Paragraph caption = new Paragraph("Trigger data harvesting and refresh operations across all markets and sources.");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.NONE);
        add(caption);

        // Load US companies for the Ticker Documents selector
        List<CompanyDto> usCompanies = companyMetadataApi.getAllCompanies().stream()
                .filter(c -> c.market() != null && "US".equalsIgnoreCase(c.market().trim()))
                .sorted(Comparator.comparing(CompanyDto::ticker))
                .toList();

        // Build grid items
        List<RefreshRow> items = new ArrayList<>();

        // 1. Market Data
        items.add(new RefreshRow(
                "Market Data",
                "Refreshes the list of all supported stock markets.",
                null,
                createRefreshButton("Market Data", harvesterBatchApi::refreshMarket)
        ));

        // 2. Industry Data
        items.add(new RefreshRow(
                "Industry Data",
                "Refreshes the list of all industry classifications.",
                null,
                createRefreshButton("Industry Data", harvesterBatchApi::refreshIndustry)
        ));

        // 3. Company List
        items.add(new RefreshRow(
                "Company List",
                "Refreshes the list of companies across all supported markets.",
                null,
                createRefreshButton("Company List", harvesterBatchApi::refreshCompany)
        ));

        // 4. Fundamentals
        items.add(new RefreshRow(
                "Fundamentals",
                "Refreshes Income, Balance Sheets, and Cash Flow Statements.",
                null,
                createRefreshButton("Fundamentals", harvesterBatchApi::refreshFundamentals)
        ));

        // 5. Ticker Documents
        MultiSelectComboBox<CompanyDto> tickersField = new MultiSelectComboBox<>();
        tickersField.setPlaceholder("Stale check (or select US companies, max 20)");
        tickersField.setWidthFull();
        tickersField.setItems(usCompanies);
        tickersField.setItemLabelGenerator(c -> c.ticker() + " - " + c.companyName());
        tickersField.setClearButtonVisible(true);
        tickersField.addSelectionListener(event -> {
            if (event.getAllSelectedItems().size() > 20) {
                tickersField.setValue(event.getOldSelection());
                Notification.show("Maximum limit of 20 US companies allowed.");
            }
        });

        Button btnTickerDocs = createRefreshButton("Ticker Documents", () -> {
            var selected = tickersField.getValue();
            if (selected == null || selected.isEmpty()) {
                harvesterBatchApi.refreshStaleSecDocuments();
            } else {
                List<TickerKeyDto> tickers = selected.stream()
                        .map(c -> new TickerKeyDto(c.ticker(), c.market()))
                        .toList();
                harvesterBatchApi.refreshSecDocuments(tickers);
            }
        });

        items.add(new RefreshRow(
                "Ticker Documents",
                "Refreshes SEC filings for US companies.",
                tickersField,
                btnTickerDocs
        ));

        // 6. Share Prices
        Checkbox incremental = new Checkbox("Incremental", true);
        DatePicker fromDate = new DatePicker();
        fromDate.setPlaceholder("Auto date");
        fromDate.setWidth("140px");
        incremental.addValueChangeListener(e -> {
            fromDate.setEnabled(e.getValue());
            if (!e.getValue()) fromDate.setValue(null);
        });
        HorizontalLayout priceParams = new HorizontalLayout(incremental, fromDate);
        priceParams.setAlignItems(Alignment.CENTER);
        priceParams.setSpacing(true);

        Button btnSharePrices = createRefreshButton("Share Prices",
                () -> harvesterBatchApi.refreshSharePrices(incremental.getValue(), fromDate.getValue()));

        items.add(new RefreshRow(
                "Share Prices",
                "Refreshes historical daily share prices.",
                priceParams,
                btnSharePrices
        ));

        // 7. SEC Document Summaries
        IntegerField limitField = new IntegerField();
        limitField.setValue(50);
        limitField.setMin(1);
        limitField.setStepButtonsVisible(true);
        limitField.setWidth("100px");
        limitField.setTooltipText("Batch Limit");

        IntegerField priorityField = new IntegerField();
        priorityField.setValue(3);
        priorityField.setMin(1);
        priorityField.setStepButtonsVisible(true);
        priorityField.setWidth("100px");
        priorityField.setTooltipText("Max Priority");

        HorizontalLayout secParams = new HorizontalLayout(
                new Span("Limit:"), limitField,
                new Span("Priority:"), priorityField
        );
        secParams.setAlignItems(Alignment.CENTER);
        secParams.setSpacing(true);

        Button btnProcessSec = new Button("Process", VaadinIcon.PLAY.create());
        btnProcessSec.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnProcessSec.addClickListener(_ -> {
            int limit = limitField.getValue() != null ? limitField.getValue() : 50;
            int priority = priorityField.getValue() != null ? priorityField.getValue() : 3;
            eventPublisher.publishEvent(new ProcessPendingSecDocumentsEvent(limit, priority));
            ViewHelper.showSuccess("Started SEC Document processing in the background (Limit: " + limit + ")");
        });

        items.add(new RefreshRow(
                "SEC Document Summaries",
                "Processes pending raw SEC documents using LLM.",
                secParams,
                btnProcessSec
        ));

        // 8. News & Sentiment
        items.add(new RefreshRow(
                "News & Sentiment",
                "Refreshes recent news articles and sentiment data.",
                null,
                createRefreshButton("News & Sentiment", harvesterBatchApi::refreshNews)
        ));

        // 9. Insider Transactions
        items.add(new RefreshRow(
                "Insider Transactions",
                "Refreshes daily insider trading from OpenInsider.",
                null,
                createRefreshButton("Insider Transactions", harvesterBatchApi::refreshInsiderTransactions)
        ));

        // 10. Macroeconomic Data
        items.add(new RefreshRow(
                "Macroeconomic Data",
                "Refreshes yield curves, inflation, unemployment.",
                null,
                createRefreshButton("Macroeconomic Data", harvesterBatchApi::refreshMacroeconomic)
        ));

        // 11. Materialized Views
        items.add(new RefreshRow(
                "Materialized Views",
                "Rebuilds materialized views and refreshes cache (async).",
                null,
                createRefreshButton("Materialized Views", () -> eventPublisher.publishEvent(new RefreshMaterializedViewsEvent()))
        ));

        // Configure Grid
        Grid<RefreshRow> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");

        grid.addComponentColumn(row -> {
            Span title = new Span(row.title());
            title.addClassName(LumoUtility.FontWeight.BOLD);
            return title;
        }).setHeader("Operation").setWidth("200px").setFlexGrow(1);

        grid.addComponentColumn(row -> {
            VerticalLayout cell = new VerticalLayout();
            cell.setPadding(false);
            cell.setSpacing(false);

            Span desc = new Span(row.description());
            desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            cell.add(desc);

            if (row.parameters() != null) {
                Div wrapper = new Div(row.parameters());
                wrapper.getStyle().set("margin-top", "var(--lumo-space-s)");
                wrapper.getStyle().set("width", "100%");
                cell.add(wrapper);
            }

            return cell;
        }).setHeader("Description & Options").setFlexGrow(4);

        grid.addComponentColumn(row -> {
            HorizontalLayout cell = new HorizontalLayout(row.actionButton());
            cell.setWidthFull();
            cell.setJustifyContentMode(JustifyContentMode.END);
            return cell;
        }).setHeader("Action").setWidth("140px").setFlexGrow(0);

        grid.setItems(items);
        add(grid);
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

    private record RefreshRow(String title, String description, Component parameters, Button actionButton) {

    }
}
