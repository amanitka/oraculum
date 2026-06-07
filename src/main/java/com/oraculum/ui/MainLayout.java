package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.RefreshView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout implements RouterLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        H1 viewTitle = new H1("Oraculum");
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.BOLD);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        com.vaadin.flow.component.orderedlayout.VerticalLayout drawerLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.addClassNames(LumoUtility.Background.CONTRAST_5);

        Component nav = buildNav();
        drawerLayout.add(nav, buildFooter());
        drawerLayout.expand(nav);

        addToDrawer(drawerLayout);
    }

    private Component buildNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Company", CompanyView.class, com.vaadin.flow.component.icon.VaadinIcon.OFFICE.create()));
        nav.addItem(new SideNavItem("Analysis", AnalysisView.class, com.vaadin.flow.component.icon.VaadinIcon.CHART_LINE.create()));
        nav.addItem(new SideNavItem("Refresh", RefreshView.class, com.vaadin.flow.component.icon.VaadinIcon.REFRESH.create()));
        return nav;
    }

    private Footer buildFooter() {
        Footer footer = new Footer();
        footer.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Margin.Top.AUTO);
        Span footerText = new Span("v1.0.0");
        footerText.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        footer.add(footerText);
        return footer;
    }
}

