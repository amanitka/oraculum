package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.RefreshView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
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
        addToDrawer(buildNav(), buildFooter());
    }

    private Component buildNav() {
        Div navContainer = new Div();
        navContainer.addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Padding.Vertical.MEDIUM);

        RouterLink analysisLink = createNavLink("Analysis", AnalysisView.class, VaadinIcon.CHART_LINE);
        RouterLink refreshLink = createNavLink("Refresh", RefreshView.class, VaadinIcon.REFRESH);

        navContainer.add(analysisLink, refreshLink);
        return navContainer;
    }

    private RouterLink createNavLink(String text, Class<? extends Component> navigationTarget, VaadinIcon icon) {
        RouterLink link = new RouterLink();
        link.setRoute(navigationTarget);
        link.addClassNames("nav-link");

        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.SMALL);

        Icon iconElement = icon.create();
        iconElement.addClassNames(LumoUtility.TextColor.SECONDARY);

        Span textSpan = new Span(text);
        textSpan.addClassNames(LumoUtility.FontWeight.MEDIUM);

        content.add(iconElement, textSpan);
        link.add(content);

        return link;
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

