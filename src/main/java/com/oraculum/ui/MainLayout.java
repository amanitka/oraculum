package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.RefreshView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout implements RouterLayout {

    public MainLayout() {
        addHeaderContent();
    }

    private void addHeaderContent() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // Left Zone: Logo
        H1 viewTitle = new H1("Oraculum");
        viewTitle.addClassNames(LumoUtility.FontSize.XLARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.BOLD);

        // Center Zone: Navigation Links
        HorizontalLayout navLayout = new HorizontalLayout();
        navLayout.addClassNames(LumoUtility.Gap.LARGE);
        navLayout.setAlignItems(Alignment.AUTO);

        RouterLink companyLink = new RouterLink("Company", CompanyView.class);
        companyLink.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);

        RouterLink screenerLink = new RouterLink("Screener", com.oraculum.ui.views.ScreenerView.class);
        screenerLink.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);

        RouterLink analysisLink = new RouterLink("Analysis", AnalysisView.class);
        analysisLink.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);

        RouterLink refreshLink = new RouterLink("Refresh", RefreshView.class);
        refreshLink.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);

        companyLink.addClassName("nav-link");
        screenerLink.addClassName("nav-link");
        analysisLink.addClassName("nav-link");
        refreshLink.addClassName("nav-link");

        navLayout.add(companyLink, screenerLink, analysisLink, refreshLink);

        // Right Zone: Action/profile icon
        Component userIcon = VaadinIcon.USER.create();

        header.add(viewTitle, navLayout, userIcon);

        addToNavbar(true, header);
    }

    @Override
    public void setContent(Component content) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.setMaxWidth("1440px");
        wrapper.addClassNames(LumoUtility.Margin.Horizontal.AUTO, LumoUtility.Padding.Horizontal.LARGE);
        wrapper.add(content);
        super.setContent(wrapper);
    }
}
