package com.oraculum.ui.components;

import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CompanyInstitutionalTabComponent extends VerticalLayout {

    private final CompanyInstitutionalFlowApi institutionalFlowApi;
    private final int companyId;

    public CompanyInstitutionalTabComponent(CompanyInstitutionalFlowApi institutionalFlowApi, int companyId) {
        this.institutionalFlowApi = institutionalFlowApi;
        this.companyId = companyId;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        initContent();
    }

    private void initContent() {
        List<CompanyInstitutionalFlowDto> history = institutionalFlowApi.getInstitutionalFlowHistory(companyId);
        if (history == null || history.isEmpty()) {
            add(createEmptyState());
            return;
        }

        CompanyInstitutionalFlowDto latest = history.getFirst();

        add(createKpiSummaryBar(latest));
        add(createTier1Section(latest));
        add(createHistorySection(history));
    }

    private Component createEmptyState() {
        VerticalLayout empty = new VerticalLayout();
        empty.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        empty.setPadding(true);

        Span msg = new Span("No SEC 13F institutional ownership data available for this company.");
        msg.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-style", "italic");
        empty.add(msg);
        return empty;
    }

    private Component createKpiSummaryBar(CompanyInstitutionalFlowDto latest) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        bar.getStyle()
                .set("margin-bottom", "0.5rem")
                .set("flex-wrap", "wrap")
                .set("gap", "1rem");

        String portVal = latest.totalInstValueUsd() != null ? ViewHelper.formatCompact(latest.totalInstValueUsd().floatValue()) : "-";
        bar.add(createKpiCard("Inst. Portfolio", portVal, null));

        String sharesVal = latest.totalInstShares() != null ? ViewHelper.formatCompactNumber(latest.totalInstShares().floatValue()) : "-";
        bar.add(createKpiCard("Inst. Shares", sharesVal, null));

        Float changePct = latest.sharesChangePct();
        String flowVal = changePct != null ? String.format(Locale.US, "%.2f%%", Math.abs(changePct)) : "-";
        bar.add(createKpiCard("QoQ Flow %", flowVal, changePct));

        Long netShares = latest.netSharesChange();
        Float netDelta = netShares != null ? netShares.floatValue() : null;
        bar.add(createKpiCard("Net Shares Δ", formatNetShares(netShares), netDelta));

        String holdersVal = latest.totalHoldersCount() != null ? String.format(Locale.US, "%,d", latest.totalHoldersCount()) : "-";
        bar.add(createKpiCard("Filers", holdersVal, null));

        String concVal = latest.top5HoldersPct() != null ? String.format(Locale.US, "%.1f%%", latest.top5HoldersPct()) : "-";
        Component top5Card = createKpiCard("Top 5 Conc.", concVal, null);
        if (latest.topHoldersNames() != null && !latest.topHoldersNames().isEmpty()) {
            top5Card.getElement().setAttribute("title", "Top 5 largest holders: " + String.join(", ", latest.topHoldersNames()));
        }
        bar.add(top5Card);

        Long t1Delta = latest.tier1NetValueChangeUsd();
        Float t1DeltaFloat = t1Delta != null ? t1Delta.floatValue() : null;
        String t1FlowVal = t1Delta != null ? (t1Delta >= 0 ? "+" : "-") + ViewHelper.formatCompact(Math.abs(t1Delta.floatValue())) : "-";
        bar.add(createKpiCard("Tier 1 Flow", t1FlowVal, t1DeltaFloat));

        String pcVal = latest.putCallRatio() != null ? String.format(Locale.US, "%.2f", latest.putCallRatio()) : "-";
        bar.add(createKpiCard("P/C Ratio", pcVal, null));

        return bar;
    }

    private Component createKpiCard(String label, String value, Float delta) {
        VerticalLayout item = new VerticalLayout();
        item.addClassName("metric-card");
        item.setWidth(null);
        item.getStyle().set("flex", "1 1 140px").set("max-width", "220px");
        item.setPadding(true);
        item.setSpacing(false);
        item.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span lblSpan = new Span(label);
        lblSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase");

        HorizontalLayout valLayout = new HorizontalLayout();
        valLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        valLayout.setSpacing(true);

        Span valSpan = new Span(value);
        valSpan.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");

        if (delta != null) {
            Icon icon = delta >= 0 ? VaadinIcon.ARROW_UP.create() : VaadinIcon.ARROW_DOWN.create();
            icon.setSize("14px");
            icon.setColor(delta >= 0 ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
            valSpan.getStyle().set("color", delta >= 0 ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
            valLayout.add(icon);
        }

        valLayout.add(valSpan);
        item.add(lblSpan, valLayout);
        return item;
    }

    private Component createTier1Section(CompanyInstitutionalFlowDto latest) {
        VerticalLayout sec = new VerticalLayout();
        sec.setWidthFull();
        sec.setPadding(false);
        sec.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H4 title = new H4("Tier 1 Smart Money (Superinvestors)");
        title.getStyle().set("margin-top", "0.25rem").set("margin-bottom", "0.25rem");

        Button viewFundsBtn = new Button("Tracked Funds ⓘ", _ -> new Tier1FundsInfoDialog(institutionalFlowApi.getTier1Filers()).open());
        viewFundsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        header.add(title, viewFundsBtn);

        HorizontalLayout badgesLayout = new HorizontalLayout();
        badgesLayout.setWidthFull();
        badgesLayout.getStyle().set("gap", "0.5rem").set("flex-wrap", "wrap").set("align-items", "center");

        addTier1SummaryBadge(badgesLayout, latest);
        addFundBadges(badgesLayout, latest.tier1BuyerNames(), true);
        addFundBadges(badgesLayout, latest.tier1SellerNames(), false);

        sec.add(header, badgesLayout);
        return sec;
    }

    private void addTier1SummaryBadge(HorizontalLayout container, CompanyInstitutionalFlowDto latest) {
        int count = latest.tier1HoldersCount() != null ? latest.tier1HoldersCount() : 0;
        String val = latest.tier1ValueUsd() != null ? ViewHelper.formatCompact(latest.tier1ValueUsd().floatValue()) : "$0";
        String deltaStr = "";
        if (latest.tier1NetValueChangeUsd() != null && latest.tier1NetValueChangeUsd() != 0) {
            String sign = latest.tier1NetValueChangeUsd() > 0 ? "+" : "-";
            deltaStr = " | Net Flow: " + sign + ViewHelper.formatCompact(Math.abs(latest.tier1NetValueChangeUsd().floatValue()));
        }
        Span pill = new Span(count + " Elite Funds (" + val + deltaStr + ")");
        pill.getElement().getThemeList().addAll(List.of("badge", "primary"));
        container.add(pill);
    }

    private void addFundBadges(HorizontalLayout container, List<String> fundNames, boolean isBuyer) {
        if (fundNames == null || fundNames.isEmpty()) return;
        for (String fund : fundNames) {
            Span badge = new Span();
            Icon icon = isBuyer ? VaadinIcon.ARROW_UP.create() : VaadinIcon.ARROW_DOWN.create();
            icon.setSize("12px");
            badge.add(icon, new Span(" " + ViewHelper.normalizeFundName(fund)));
            badge.getElement().getThemeList().addAll(List.of("badge", isBuyer ? "success" : "error"));
            container.add(badge);
        }
    }

    private Component createHistorySection(List<CompanyInstitutionalFlowDto> history) {
        VerticalLayout sec = new VerticalLayout();
        sec.setWidthFull();
        sec.setPadding(false);
        sec.setSpacing(true);

        H4 title = new H4("Quarterly 13F History");
        title.getStyle().set("margin-top", "0.5rem").set("margin-bottom", "0.25rem");

        Grid<CompanyInstitutionalFlowDto> grid = new Grid<>(CompanyInstitutionalFlowDto.class, false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        addHistoryGridColumns(grid);
        grid.setItems(history.stream()
                .sorted(Comparator.comparing(CompanyInstitutionalFlowDto::reportPeriod).reversed())
                .toList());

        sec.add(title, grid);
        return sec;
    }

    private void addHistoryGridColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        addPeriodAndPortfolioColumns(grid);
        addSharesAndFlowColumns(grid);
        addFilersAndOptionColumns(grid);
        addTier1ActivityColumns(grid);
    }

    private void addPeriodAndPortfolioColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        grid.addColumn(f -> formatQuarter(f.reportPeriod())).setHeader("Quarter").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(CompanyInstitutionalFlowDto::reportPeriod).setHeader("Period").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.totalInstValueUsd() != null ? ViewHelper.formatCompact(f.totalInstValueUsd().floatValue()) : "-")
                .setHeader("Portfolio ($)").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.deltaCurrencySpan(f.netValueChangeUsd())))
                .setHeader("Net Flow ($)").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
    }

    private void addSharesAndFlowColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        grid.addColumn(f -> f.totalInstShares() != null ? ViewHelper.formatCompactNumber(f.totalInstShares().floatValue()) : "-")
                .setHeader("Shares").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.deltaNumberSpan(f.netSharesChange())))
                .setHeader("Net Shares Δ").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.priceChangeSpan(f.sharesChangePct())))
                .setHeader("QoQ Flow %").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
    }

    private void addFilersAndOptionColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        grid.addColumn(f -> f.totalHoldersCount() != null ? String.format(Locale.US, "%,d", f.totalHoldersCount()) : "-")
                .setHeader("Holders").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(this::createBuyersSellersCell))
                .setHeader("Buyers / Sellers").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(this::createNewClosedCell))
                .setHeader("New / Closed").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(this::createTop5HoldersCell))
                .setHeader(ViewHelper.createExplainedHeader("Top 5 Conc.", "Share of total institutional float held by the top 5 largest filers overall (e.g. Vanguard, BlackRock, State Street)"))
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(this::createPutCallRatioCell))
                .setHeader("P/C Ratio").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
    }

    private Component createTop5HoldersCell(CompanyInstitutionalFlowDto f) {
        if (f.top5HoldersPct() == null) return new Span("-");
        Span s = new Span(String.format(Locale.US, "%.1f%%", f.top5HoldersPct()));
        if (f.topHoldersNames() != null && !f.topHoldersNames().isEmpty()) {
            s.setTitle("Top 5 largest holders: " + String.join(", ", f.topHoldersNames()));
        }
        return s;
    }

    private void addTier1ActivityColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        grid.addColumn(CompanyInstitutionalTabComponent::formatTier1Holdings)
                .setHeader(ViewHelper.createExplainedHeader("Tier 1 Funds", "Number of curated superinvestor/elite hedge funds holding position and total dollar value at quarter end"))
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.deltaCurrencySpan(f.tier1NetValueChangeUsd())))
                .setHeader(ViewHelper.createExplainedHeader("Tier 1 Flow ($)", "Net quarterly dollar change in holdings by Tier 1 funds"))
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(this::createTier1ActivityCell))
                .setHeader("Tier 1 Activity").setAutoWidth(true).setResizable(true).setFlexGrow(1);
    }

    private Component createBuyersSellersCell(CompanyInstitutionalFlowDto f) {
        Integer buyers = f.fundsBuyingCount();
        Integer sellers = f.fundsSellingCount();
        if (buyers == null && sellers == null) return new Span("-");

        Span bSpan = new Span(buyers != null ? String.format(Locale.US, "%,d", buyers) : "0");
        bSpan.getStyle().set("color", "var(--lumo-success-text-color)");

        Span sep = new Span(" / ");
        sep.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span sSpan = new Span(sellers != null ? String.format(Locale.US, "%,d", sellers) : "0");
        sSpan.getStyle().set("color", "var(--lumo-error-text-color)");

        Span container = new Span(bSpan, sep, sSpan);
        container.getStyle().set("white-space", "nowrap").set("font-weight", "500");
        return container;
    }

    private Component createNewClosedCell(CompanyInstitutionalFlowDto f) {
        Integer newCount = f.fundsNewCount();
        Integer closedCount = f.fundsClosedCount();
        if (newCount == null && closedCount == null) return new Span("-");

        Span nSpan = new Span(newCount != null ? "+" + newCount : "0");
        nSpan.getStyle().set("color", "var(--lumo-success-text-color)");

        Span sep = new Span(" / ");
        sep.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span cSpan = new Span(closedCount != null ? "-" + closedCount : "0");
        cSpan.getStyle().set("color", "var(--lumo-error-text-color)");

        Span container = new Span(nSpan, sep, cSpan);
        container.getStyle().set("white-space", "nowrap").set("font-weight", "500");
        return container;
    }

    private Component createPutCallRatioCell(CompanyInstitutionalFlowDto f) {
        if (f.putCallRatio() == null) return new Span("-");
        Span s = new Span(String.format(Locale.US, "%.2f", f.putCallRatio()));
        if (f.putValueUsd() != null && f.callValueUsd() != null) {
            String pStr = ViewHelper.formatCompact(f.putValueUsd().floatValue());
            String cStr = ViewHelper.formatCompact(f.callValueUsd().floatValue());
            s.setTitle("Puts: " + pStr + " | Calls: " + cStr);
        }
        return s;
    }

    private static String formatTier1Holdings(CompanyInstitutionalFlowDto f) {
        if (f.tier1HoldersCount() == null || f.tier1HoldersCount() == 0) return "-";
        String val = f.tier1ValueUsd() != null ? ViewHelper.formatCompact(f.tier1ValueUsd().floatValue()) : "$0";
        return f.tier1HoldersCount() + " (" + val + ")";
    }

    private Component createTier1ActivityCell(CompanyInstitutionalFlowDto flow) {
        List<String> buyers = flow.tier1BuyerNames();
        List<String> sellers = flow.tier1SellerNames();
        boolean hasBuyers = buyers != null && !buyers.isEmpty();
        boolean hasSellers = sellers != null && !sellers.isEmpty();

        if (!hasBuyers && !hasSellers) {
            return new Span("-");
        }

        HorizontalLayout cell = new HorizontalLayout();
        cell.setSpacing(true);
        cell.getStyle().set("gap", "0.25rem").set("flex-wrap", "wrap").set("align-items", "center");

        if (hasBuyers) {
            for (String b : buyers) {
                cell.add(createMiniFundBadge(b, true));
            }
        }
        if (hasSellers) {
            for (String s : sellers) {
                cell.add(createMiniFundBadge(s, false));
            }
        }
        return cell;
    }

    private Span createMiniFundBadge(String fundName, boolean isBuyer) {
        Span badge = new Span();
        Icon icon = isBuyer ? VaadinIcon.ARROW_UP.create() : VaadinIcon.ARROW_DOWN.create();
        icon.setSize("10px");
        String name = ViewHelper.normalizeFundName(fundName);
        badge.add(icon, new Span(" " + name));
        badge.getElement().getThemeList().addAll(List.of("badge", isBuyer ? "success" : "error"));
        badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        badge.setTitle(name + (isBuyer ? " (Buyer / Added stake)" : " (Seller / Reduced or Closed stake)"));
        return badge;
    }

    private static String formatQuarter(LocalDate period) {
        if (period == null) return "-";
        int q = (period.getMonthValue() - 1) / 3 + 1;
        return "Q" + q + " " + period.getYear();
    }

    private static String formatNetShares(Long netShares) {
        if (netShares == null) return "-";
        String sign = netShares > 0 ? "+" : "";
        return sign + ViewHelper.formatCompactNumber(netShares.floatValue());
    }
}
