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
        // Outer wrapper unconditionally taking full space with FlexBox centering
        Div outerHeader = new Div();
        outerHeader.setWidthFull();
        outerHeader.getStyle().set("display", "flex");
        outerHeader.getStyle().set("justify-content", "center");
        outerHeader.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");

        // Inner max-width container that holds the header content
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setMaxWidth("1440px");
        header.setPadding(true);
        header.addClassNames(LumoUtility.Padding.Horizontal.LARGE);
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // Left Zone: Title and Logo
        HorizontalLayout logoLayout = new HorizontalLayout();
        logoLayout.setAlignItems(Alignment.CENTER);
        logoLayout.addClassNames(LumoUtility.Gap.SMALL); // Minimal gap between text and logo

        H1 viewTitle = new H1("oraculum");
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, // Smaller font size
                LumoUtility.Margin.NONE,
                LumoUtility.TextColor.BODY,
                LumoUtility.FontWeight.BOLD);

        com.vaadin.flow.component.html.Image logo = new com.vaadin.flow.component.html.Image("images/logo.svg", "Oraculum Logo");
        logo.setHeight("32px"); // Even smaller to match text exactly
        logo.getStyle().set("margin-top", "6px"); // Nudge down slightly to align with the text baseline

        logoLayout.add(viewTitle, logo);

        // Center Zone: Navigation Links
        HorizontalLayout navLayout = new HorizontalLayout();
        navLayout.addClassNames(LumoUtility.Gap.LARGE);
        navLayout.setAlignItems(Alignment.AUTO);

        RouterLink companyLink = new RouterLink("Company", CompanyView.class);
        RouterLink screenerLink = new RouterLink("Screener", com.oraculum.ui.views.ScreenerView.class);
        RouterLink analysisLink = new RouterLink("Analysis", AnalysisView.class);
        RouterLink refreshLink = new RouterLink("Refresh", RefreshView.class);

        companyLink.addClassName("nav-link");
        screenerLink.addClassName("nav-link");
        analysisLink.addClassName("nav-link");
        refreshLink.addClassName("nav-link");

        navLayout.add(companyLink, screenerLink, analysisLink, refreshLink);

        // Group Logo and Nav together on the left
        HorizontalLayout leftGroup = new HorizontalLayout(logoLayout, navLayout);
        leftGroup.setAlignItems(Alignment.CENTER);
        leftGroup.addClassNames(LumoUtility.Gap.XLARGE); // Generous gap between logo and menu

        // Right Zone: Action/profile icon
        Component userIcon = VaadinIcon.USER.create();

        header.add(leftGroup, userIcon);

        outerHeader.add(header);
        addToNavbar(true, outerHeader);
    }

    @Override
    public void setContent(Component content) {
        Div outer = new Div();
        outer.setSizeFull();
        outer.getStyle().set("display", "flex");
        outer.getStyle().set("justify-content", "center");

        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.setMaxWidth("1440px");
        wrapper.getStyle().set("height", "100%");
        wrapper.getStyle().set("display", "flex");
        wrapper.getStyle().set("flex-direction", "column");
        wrapper.addClassNames(LumoUtility.Padding.Horizontal.LARGE);
        wrapper.add(content);

        outer.add(wrapper);
        super.setContent(outer);
    }
}
