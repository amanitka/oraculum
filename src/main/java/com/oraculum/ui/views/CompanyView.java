package com.oraculum.ui.views;

import com.oraculum.company.api.CompanyApi;
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

    private final CompanyApi companyApi;
    private final ObjectMapper objectMapper;
    private final Div contentArea;
    private ComboBox<CompanyDto> companyComboBox;

    public CompanyView(CompanyApi companyApi, ObjectMapper objectMapper) {
        this.companyApi = companyApi;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Header
        Component header = createHeader();
        add(header);

        // Main Dashboard Area
        contentArea = new Div();
        contentArea.setSizeFull();
        contentArea.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.LARGE);
        add(contentArea);
        setFlexGrow(1, contentArea);
    }

    private Component createHeader() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(false);
        wrapper.getStyle().set("margin-bottom", "1rem");

        H3 title = new H3("Select company");
        title.getStyle().set("margin-top", "1rem");
        title.getStyle().set("margin-bottom", "0.5rem");

        companyComboBox = new ComboBox<>();
        companyComboBox.setPlaceholder("Search Company or Ticker...");
        companyComboBox.setWidth("400px");
        companyComboBox.setClearButtonVisible(true);

        companyComboBox.setItems(companyApi.getAllCompanies());
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

        CompanyOverviewComponent overview = new CompanyOverviewComponent(companyApi, selectedCompany, objectMapper);
        contentArea.add(overview);
    }

}
