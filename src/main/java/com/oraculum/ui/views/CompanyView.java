package com.oraculum.ui.views;

import com.oraculum.company.api.*;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.ui.MainLayout;
import com.oraculum.ui.components.CompanyOverviewComponent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import tools.jackson.databind.ObjectMapper;

@Route(value = "company", layout = MainLayout.class)
@PageTitle("Company Details")
public class CompanyView extends VerticalLayout {

    private final CompanyMetadataApi companyMetadataApi;
    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final ObjectMapper objectMapper;
    private final Div contentArea;
    private ComboBox<CompanyDto> companyComboBox;

    public CompanyView(CompanyMetadataApi companyMetadataApi,
                       CompanyFinancialDataApi companyFinancialDataApi,
                       CompanySharePriceApi companySharePriceApi,
                       CompanyNewsApi companyNewsApi,
                       CompanyInsiderTransactionApi companyInsiderTransactionApi,
                       ObjectMapper objectMapper) {
        this.companyMetadataApi = companyMetadataApi;
        this.companyFinancialDataApi = companyFinancialDataApi;
        this.companySharePriceApi = companySharePriceApi;
        this.companyNewsApi = companyNewsApi;
        this.companyInsiderTransactionApi = companyInsiderTransactionApi;
        this.objectMapper = objectMapper;

        setWidthFull();
        setPadding(false);
        setSpacing(true);
        getStyle().set("padding-top", "2rem");
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Header
        Component header = createHeader();
        add(header);

        // Main Dashboard Area
        contentArea = new Div();
        contentArea.setWidthFull();
        contentArea.getStyle().set("margin-bottom", "2rem");
        contentArea.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.LARGE);
        add(contentArea);
    }

    private Component createHeader() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(false);

        H3 title = new H3("Select company");
        title.getStyle().set("margin-bottom", "1rem");

        companyComboBox = new ComboBox<>();
        companyComboBox.setPlaceholder("Search Company or Ticker...");
        companyComboBox.setWidth("400px");
        companyComboBox.setClearButtonVisible(true);

        companyComboBox.setItems(companyMetadataApi.getAllCompanies());
        companyComboBox.setItemLabelGenerator(c -> String.format("%s - %s", c.ticker(), c.companyName()));

        companyComboBox.addValueChangeListener(_ -> loadCompanyData());

        HorizontalLayout row = new HorizontalLayout(companyComboBox);
        row.setWidthFull();
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        wrapper.add(title, row);
        return wrapper;
    }

    private void loadCompanyData() {
        CompanyDto selectedCompany = companyComboBox.getValue();
        contentArea.removeAll();
        if (selectedCompany == null) {
            Span text = new Span("Select a company to view details");
            text.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM, LumoUtility.TextColor.SECONDARY);
            contentArea.add(text);
            return;
        }

        CompanyOverviewComponent overview = new CompanyOverviewComponent(companyFinancialDataApi, companySharePriceApi, companyNewsApi, companyInsiderTransactionApi, selectedCompany, objectMapper);
        contentArea.add(overview);
    }

}
