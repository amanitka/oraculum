package com.oraculum.ui.factory;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyValuationApi;
import com.oraculum.ui.ViewHelper;
import com.oraculum.ui.components.CompanyOverviewComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CompanyDetailsButtonFactory {

    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final CompanyValuationApi companyValuationApi;
    private final ObjectMapper objectMapper;

    public CompanyDetailsButtonFactory(CompanyMetadataApi companyMetadataApi,
                                       CompanyFinancialDataApi companyFinancialDataApi,
                                       CompanySharePriceApi companySharePriceApi,
                                       CompanyNewsApi companyNewsApi,
                                       CompanyInsiderTransactionApi companyInsiderTransactionApi,
                                       CompanyValuationApi companyValuationApi,
                                       ObjectMapper objectMapper) {
        this.companyMetadataApi = companyMetadataApi;
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.companyValuationApi = companyValuationApi;
        this.objectMapper = objectMapper;
    }

    public Button createButton(int companyId, boolean showText) {
        Button btn;
        if (showText) {
            btn = new Button("View", VaadinIcon.CHART_LINE.create());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        } else {
            btn = new Button(VaadinIcon.CHART_LINE.create());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            btn.setAriaLabel("View company tearsheet");
            btn.setTooltipText("View company tearsheet");
        }
        btn.addClickListener(_ -> openCompanyDetailsDialog(companyId));
        return btn;
    }

    public void openCompanyDetailsDialog(int companyId) {
        CompanyDto company = companyMetadataApi.getCompanyById(companyId);
        if (company != null) {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle(company.companyName() + " (" + company.ticker() + ")");
            dialog.setWidth("90vw");
            dialog.setHeight("90vh");
            dialog.add(new CompanyOverviewComponent(companyFinancialDataApi, companySharePriceApi, companyNewsApi, companyInsiderTransactionApi, companyValuationApi, company, objectMapper));

            Button headerCloseBtn = new Button(VaadinIcon.CLOSE.create(), _ -> dialog.close());
            headerCloseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            headerCloseBtn.setAriaLabel("Close dialog");
            dialog.getHeader().add(headerCloseBtn);

            Button closeBtn = new Button("Close", _ -> dialog.close());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dialog.getFooter().add(closeBtn);

            // Trigger window resize when dialog completes opening animation so external JS charts (ApexCharts) resize correctly
            dialog.addOpenedChangeListener(e -> {
                if (e.isOpened()) {
                    UI.getCurrent().getPage().executeJs("setTimeout(() => window.dispatchEvent(new Event('resize')), 250);");
                }
            });

            dialog.open();
        } else {
            ViewHelper.showError("Company details not found.");
        }
    }
}
