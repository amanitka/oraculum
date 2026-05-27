package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.RefreshView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        Span appName = new Span("Oraculum");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);

        Header header = new Header(appName);
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Gap.SMALL
        );

        addToDrawer(header, buildNav(), buildFooter());
    }

    private SideNav buildNav() {
        SideNav nav = new SideNav();
        nav.addClassNames(LumoUtility.Padding.Horizontal.SMALL);

        SideNavItem analysisItem = new SideNavItem("Analysis", AnalysisView.class,
                VaadinIcon.CHART_LINE.create());
        SideNavItem refreshItem = new SideNavItem("Refresh", RefreshView.class,
                VaadinIcon.REFRESH.create());

        nav.addItem(analysisItem, refreshItem);
        return nav;
    }

    private Footer buildFooter() {
        Footer footer = new Footer();
        footer.addClassNames(LumoUtility.Padding.MEDIUM);
        return footer;
    }
}
