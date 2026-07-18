package com.oraculum.ui.components;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.user.api.UserManagementApi;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.tabs.TabSheet;
import org.springframework.context.ApplicationEventPublisher;

public class AdministrationDialog extends Dialog {

    public AdministrationDialog(UserManagementApi userManagementApi,
                                HarvesterBatchApi harvesterBatchApi,
                                ApplicationEventPublisher eventPublisher,
                                CompanyMetadataApi companyMetadataApi) {
        setHeaderTitle("Administration");
        setWidth("90vw");
        setHeight("90vh");

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.addThemeName("bordered");

        tabSheet.add("User Management", new UserManagementComponent(userManagementApi));
        tabSheet.add("Data Refresh", new DataRefreshComponent(harvesterBatchApi, eventPublisher, companyMetadataApi));

        add(tabSheet);

        Button closeButton = new Button("Close", _ -> close());
        getFooter().add(closeButton);
    }
}
