package com.oraculum.ui.components;

import com.oraculum.analyst.api.event.ProcessPendingSecDocumentsEvent;
import com.oraculum.common.constant.MarketConstants;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.database.api.event.RefreshMaterializedViewsEvent;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class DataRefreshComponent extends VerticalLayout {

    public DataRefreshComponent(HarvesterBatchApi harvesterBatchApi,
                                ApplicationEventPublisher eventPublisher,
                                CompanyMetadataApi companyMetadataApi) {
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(createCaption());
        List<RefreshRow> items = createRefreshRows(harvesterBatchApi, eventPublisher, companyMetadataApi);
        add(createGrid(items));
    }

    private Paragraph createCaption() {
        Paragraph caption = new Paragraph("Trigger data harvesting and refresh operations across all markets and sources.");
        caption.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.NONE);
        return caption;
    }

    private List<RefreshRow> createRefreshRows(HarvesterBatchApi harvesterBatchApi,
                                               ApplicationEventPublisher eventPublisher,
                                               CompanyMetadataApi companyMetadataApi) {
        return List.of(
                createSimpleRow("Market Data", "Refreshes the list of all supported stock markets.", harvesterBatchApi::refreshMarket),
                createSimpleRow("Industry Data", "Refreshes the list of all industry classifications.", harvesterBatchApi::refreshIndustry),
                createSimpleRow("Company List", "Refreshes the list of companies across all supported markets.", harvesterBatchApi::refreshCompany),
                createSimpleRow("Fundamentals", "Refreshes Income, Balance Sheets, and Cash Flow Statements.", harvesterBatchApi::refreshFundamentals),
                createTickerDocumentsRow(companyMetadataApi, harvesterBatchApi),
                createDailySecDocumentsRow(harvesterBatchApi),
                createSec13FRow(harvesterBatchApi),
                createSharePricesRow(harvesterBatchApi),
                createSecSummariesRow(eventPublisher),
                createSimpleRow("Insider Transactions", "Refreshes daily insider trading from OpenInsider.", harvesterBatchApi::refreshInsiderTransactions),
                createSimpleRow("Macroeconomic Data", "Refreshes yield curves, inflation, unemployment.", harvesterBatchApi::refreshMacroeconomic),
                createSimpleRow("Materialized Views", "Rebuilds materialized views and refreshes cache (async).", () -> eventPublisher.publishEvent(new RefreshMaterializedViewsEvent())),
                createSimpleRow("Company CUSIPs", "Resolves and synchronizes CUSIP identifiers for tracked companies via Bloomberg OpenFIGI.", harvesterBatchApi::refreshCompanyCusips)
        );
    }

    private RefreshRow createSimpleRow(String title, String description, Runnable action) {
        return new RefreshRow(title, description, null, createRefreshButton(title, action));
    }

    private RefreshRow createTickerDocumentsRow(CompanyMetadataApi companyMetadataApi, HarvesterBatchApi harvesterBatchApi) {
        MultiSelectComboBox<CompanyDto> tickersField = createUsTickersField(companyMetadataApi);
        Button actionButton = createRefreshButton("Ticker Documents", () -> {
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
        return new RefreshRow("Ticker Documents (Historical)", "Refreshes SEC filings for US companies (Stale backfill).", tickersField, actionButton);
    }

    private MultiSelectComboBox<CompanyDto> createUsTickersField(CompanyMetadataApi companyMetadataApi) {
        List<CompanyDto> usCompanies = companyMetadataApi.getAllCompanies().stream()
                .filter(c -> c.market() != null && MarketConstants.US.equalsIgnoreCase(c.market().trim()))
                .sorted(Comparator.comparing(CompanyDto::ticker))
                .toList();

        MultiSelectComboBox<CompanyDto> field = new MultiSelectComboBox<>();
        field.setPlaceholder("Stale check (or select US companies, max 20)");
        field.setWidthFull();
        field.setItems(usCompanies);
        field.setItemLabelGenerator(c -> c.ticker() + " - " + c.companyName());
        field.setClearButtonVisible(true);
        field.addSelectionListener(event -> {
            if (event.getAllSelectedItems().size() > 20) {
                field.setValue(event.getOldSelection());
                Notification.show("Maximum limit of 20 US companies allowed.");
            }
        });
        return field;
    }

    private RefreshRow createDailySecDocumentsRow(HarvesterBatchApi harvesterBatchApi) {
        DatePicker dailyDate = new DatePicker();
        dailyDate.setPlaceholder("Target date (defaults to today)");
        dailyDate.setWidth("200px");

        Button actionButton = createRefreshButton("Daily SEC Documents", () -> {
            LocalDate targetDate = dailyDate.getValue() != null ? dailyDate.getValue() : LocalDate.now();
            harvesterBatchApi.refreshDailyNewSecDocuments(targetDate);
        });

        return new RefreshRow("Daily SEC Documents", "Fast check for new SEC documents using the daily master index.", dailyDate, actionButton);
    }

    private RefreshRow createSec13FRow(HarvesterBatchApi harvesterBatchApi) {
        IntegerField yearField = new IntegerField();
        yearField.setPlaceholder("Year (auto)");
        yearField.setMin(2013);
        yearField.setMax(LocalDate.now().getYear());
        yearField.setWidth("120px");
        yearField.setClearButtonVisible(true);

        ComboBox<Integer> quarterField = new ComboBox<>();
        quarterField.setItems(1, 2, 3, 4);
        quarterField.setItemLabelGenerator(q -> "Q" + q);
        quarterField.setPlaceholder("Quarter (auto)");
        quarterField.setWidth("130px");
        quarterField.setClearButtonVisible(true);

        HorizontalLayout params = new HorizontalLayout(yearField, quarterField);
        params.setAlignItems(Alignment.CENTER);
        params.setSpacing(true);

        Button actionButton = createRefreshButton("SEC 13F Holdings", () -> {
            Integer yr = yearField.getValue();
            Integer qtr = quarterField.getValue();
            if (yr != null && qtr != null) {
                harvesterBatchApi.refresh13FBulk(yr, qtr);
            } else {
                harvesterBatchApi.refresh13FBulk();
            }
        });

        return new RefreshRow("SEC 13F Holdings", "Downloads quarterly institutional holdings (Form 13F bulk dataset). Defaults to latest completed quarter.", params, actionButton);
    }

    private RefreshRow createSharePricesRow(HarvesterBatchApi harvesterBatchApi) {
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

        Button actionButton = createRefreshButton("Share Prices",
                () -> harvesterBatchApi.refreshSharePrices(incremental.getValue(), fromDate.getValue()));

        return new RefreshRow("Share Prices", "Refreshes historical daily share prices.", priceParams, actionButton);
    }

    private RefreshRow createSecSummariesRow(ApplicationEventPublisher eventPublisher) {
        IntegerField limitField = new IntegerField();
        limitField.setValue(50);
        limitField.setMin(1);
        limitField.setStepButtonsVisible(true);
        limitField.setWidth("100px");
        limitField.setTooltipText("Batch Limit");

        HorizontalLayout secParams = new HorizontalLayout(new Span("Limit:"), limitField);
        secParams.setAlignItems(Alignment.CENTER);
        secParams.setSpacing(true);

        Button btnProcessSec = new Button("Process", VaadinIcon.PLAY.create());
        btnProcessSec.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnProcessSec.addClickListener(_ -> {
            int limit = limitField.getValue() != null ? limitField.getValue() : 50;
            eventPublisher.publishEvent(new ProcessPendingSecDocumentsEvent(limit));
            ViewHelper.showSuccess("Started SEC Document processing in the background (Limit: " + limit + ")");
        });

        return new RefreshRow("SEC Document Summaries", "Processes pending raw SEC documents using LLM.", secParams, btnProcessSec);
    }

    private Grid<RefreshRow> createGrid(List<RefreshRow> items) {
        Grid<RefreshRow> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");

        grid.addComponentColumn(this::createTitleColumn)
                .setHeader("Operation").setWidth("200px").setFlexGrow(1);

        grid.addComponentColumn(this::createDescriptionColumn)
                .setHeader("Description & Options").setFlexGrow(4);

        grid.addComponentColumn(this::createActionColumn)
                .setHeader("Action").setWidth("140px").setFlexGrow(0);

        grid.setItems(items);
        return grid;
    }

    private Span createTitleColumn(RefreshRow row) {
        Span title = new Span(row.title());
        title.addClassName(LumoUtility.FontWeight.BOLD);
        return title;
    }

    private Component createDescriptionColumn(RefreshRow row) {
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
    }

    private Component createActionColumn(RefreshRow row) {
        HorizontalLayout cell = new HorizontalLayout(row.actionButton());
        cell.setWidthFull();
        cell.setJustifyContentMode(JustifyContentMode.END);
        return cell;
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
