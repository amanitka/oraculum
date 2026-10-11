package com.oraculum.ui.components;

import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
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
        bar.add(createKpiCard("Top 5 Conc.", concVal, null));

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

        H4 title = new H4("Tier 1 Smart Money (Superinvestors)");
        title.getStyle().set("margin-top", "0.25rem").set("margin-bottom", "0.25rem");

        HorizontalLayout badgesLayout = new HorizontalLayout();
        badgesLayout.setWidthFull();
        badgesLayout.getStyle().set("gap", "0.5rem").set("flex-wrap", "wrap").set("align-items", "center");

        addTier1SummaryBadge(badgesLayout, latest);
        addFundBadges(badgesLayout, latest.tier1BuyerNames(), true);
        addFundBadges(badgesLayout, latest.tier1SellerNames(), false);

        sec.add(title, badgesLayout);
        return sec;
    }

    private void addTier1SummaryBadge(HorizontalLayout container, CompanyInstitutionalFlowDto latest) {
        int count = latest.tier1HoldersCount() != null ? latest.tier1HoldersCount() : 0;
        String val = latest.tier1ValueUsd() != null ? ViewHelper.formatCompact(latest.tier1ValueUsd().floatValue()) : "$0";
        Span pill = new Span(count + " Elite Funds (" + val + ")");
        pill.getElement().getThemeList().addAll(List.of("badge", "primary"));
        container.add(pill);
    }

    private void addFundBadges(HorizontalLayout container, List<String> fundNames, boolean isBuyer) {
        if (fundNames == null || fundNames.isEmpty()) return;
        for (String fund : fundNames) {
            Span badge = new Span();
            Icon icon = isBuyer ? VaadinIcon.ARROW_UP.create() : VaadinIcon.ARROW_DOWN.create();
            icon.setSize("12px");
            badge.add(icon, new Span(" " + fund));
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
        grid.setHeight("600px");

        addHistoryGridColumns(grid);
        grid.setItems(history.stream()
                .sorted(Comparator.comparing(CompanyInstitutionalFlowDto::reportPeriod).reversed())
                .toList());

        sec.add(title, grid);
        return sec;
    }

    private void addHistoryGridColumns(Grid<CompanyInstitutionalFlowDto> grid) {
        grid.addColumn(f -> formatQuarter(f.reportPeriod())).setHeader("Quarter").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(CompanyInstitutionalFlowDto::reportPeriod).setHeader("Period").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.totalInstValueUsd() != null ? ViewHelper.formatCompact(f.totalInstValueUsd().floatValue()) : "-")
                .setHeader("Portfolio ($)").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.totalInstShares() != null ? ViewHelper.formatCompactNumber(f.totalInstShares().floatValue()) : "-")
                .setHeader("Shares").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.priceChangeSpan(f.sharesChangePct())))
                .setHeader("QoQ Flow %").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(new ComponentRenderer<>(f -> ViewHelper.deltaNumberSpan(f.netSharesChange())))
                .setHeader("Net Shares Δ").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.totalHoldersCount() != null ? String.format(Locale.US, "%,d", f.totalHoldersCount()) : "-")
                .setHeader("Holders").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.top5HoldersPct() != null ? String.format(Locale.US, "%.1f%%", f.top5HoldersPct()) : "-")
                .setHeader("Top 5 Conc.").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.putCallRatio() != null ? String.format(Locale.US, "%.2f", f.putCallRatio()) : "-")
                .setHeader("P/C Ratio").setAutoWidth(true).setSortable(true).setResizable(true);
        grid.addColumn(f -> f.tier1BuyerNames() != null && !f.tier1BuyerNames().isEmpty() ? String.join(", ", f.tier1BuyerNames()) : "-")
                .setHeader("Tier 1 Buyers").setAutoWidth(true).setResizable(true).setFlexGrow(1);
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
