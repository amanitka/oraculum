package com.oraculum.ui.components;

import com.oraculum.company.api.dto.SecFilerDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class Tier1FundsInfoDialog extends Dialog {

    public Tier1FundsInfoDialog(List<SecFilerDto> filers) {
        setHeaderTitle("Tier 1 Smart Money (Tracked Superinvestors)");
        setWidth("720px");
        setMaxWidth("95vw");
        setMaxHeight("85vh");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        content.add(createExplanationSection());
        content.add(createFilersList(filers));

        add(content);

        Button closeBtn = new Button("Close", _ -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(closeBtn);
    }

    private Component createExplanationSection() {
        Paragraph p = new Paragraph(
                "Tier 1 represents curated elite hedge funds and legendary value investors. "
                + "Unlike massive passive asset managers (e.g. Vanguard, BlackRock) who mechanically buy the index, "
                + "Tier 1 managers make active, high-conviction decisions with proprietary capital. "
                + "Quarter-over-quarter stake increases or new positions here carry significant informational signal."
        );
        p.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-top", "0")
                .set("margin-bottom", "0.5rem");
        return p;
    }

    private Component createFilersList(List<SecFilerDto> filers) {
        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true);
        list.setWidthFull();

        if (filers == null || filers.isEmpty()) {
            Span empty = new Span("No Tier 1 filers configured.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            list.add(empty);
            return list;
        }

        for (SecFilerDto filer : filers) {
            list.add(createFilerCard(filer));
        }
        return list;
    }

    private Component createFilerCard(SecFilerDto filer) {
        HorizontalLayout card = new HorizontalLayout();
        card.addClassName("metric-card");
        card.setWidthFull();
        card.setPadding(true);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);

        String cleanName = ViewHelper.normalizeFundName(filer.managerName());
        if (cleanName.isBlank()) {
            cleanName = "CIK " + filer.cik();
        }

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);

        Span nameSpan = new Span(cleanName);
        nameSpan.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-m)");
        titleRow.add(nameSpan);

        String investor = ViewHelper.getIconicInvestor(filer.cik());
        if (investor != null) {
            Span investorBadge = new Span(investor);
            investorBadge.getElement().getThemeList().addAll(List.of("badge", "primary", "small"));
            titleRow.add(investorBadge);
        }

        Span secDetail = new Span(formatSecDetail(filer));
        secDetail.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        info.add(titleRow, secDetail);
        card.add(info);
        return card;
    }

    private static String formatSecDetail(SecFilerDto filer) {
        StringBuilder sb = new StringBuilder("SEC CIK: ").append(filer.cik());
        if (filer.managerName() != null && !filer.managerName().isBlank()) {
            sb.append(" · Filing: ").append(filer.managerName().trim());
        }
        if (filer.lastFilingDate() != null) {
            sb.append(" · Last 13F: ").append(filer.lastFilingDate());
        }
        return sb.toString();
    }
}
