package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.EconomyView;
import com.oraculum.ui.views.RefreshView;
import com.oraculum.ui.views.ScreenerView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final java.util.Map<Class<?>, Tab> tabMap = new java.util.HashMap<>();
    private Tabs tabs;

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
        header.setPadding(false);
        header.addClassNames(LumoUtility.Padding.Horizontal.LARGE);
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // Left Zone: Title and Logo
        HorizontalLayout logoLayout = new HorizontalLayout();
        logoLayout.setAlignItems(Alignment.CENTER);
        logoLayout.addClassNames(LumoUtility.Gap.SMALL); // Minimal gap between text and logo

        H1 viewTitle = new H1("oraculum");
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.TextColor.BODY,
                LumoUtility.FontWeight.BOLD);

        Image logo = new Image("images/logo.svg", "Oraculum Logo");
        logo.setHeight("30px"); // Even smaller to match text exactly
        logo.getStyle().set("margin-top", "6px"); // Nudge down slightly to align with the text baseline

        logoLayout.add(logo, viewTitle);

        // Navigation Links using Vaadin Tabs
        RouterLink screenerLink = new RouterLink("Screener", ScreenerView.class);
        RouterLink analysisLink = new RouterLink("Analysis", AnalysisView.class);
        RouterLink companyLink = new RouterLink("Company", CompanyView.class);
        RouterLink economyLink = new RouterLink("Economy", EconomyView.class);
        RouterLink refreshLink = new RouterLink("Refresh", RefreshView.class);

        screenerLink.getStyle().set("text-decoration", "none");
        analysisLink.getStyle().set("text-decoration", "none");
        companyLink.getStyle().set("text-decoration", "none");
        economyLink.getStyle().set("text-decoration", "none");
        refreshLink.getStyle().set("text-decoration", "none");

        Tab tabScreener = new Tab(screenerLink);
        Tab tabAnalysis = new Tab(analysisLink);
        Tab tabCompany = new Tab(companyLink);
        Tab tabEconomy = new Tab(economyLink);
        Tab tabRefresh = new Tab(refreshLink);

        tabMap.put(ScreenerView.class, tabScreener);
        tabMap.put(AnalysisView.class, tabAnalysis);
        tabMap.put(CompanyView.class, tabCompany);
        tabMap.put(EconomyView.class, tabEconomy);
        tabMap.put(RefreshView.class, tabRefresh);

        tabs = new Tabs(tabScreener, tabAnalysis, tabCompany, tabEconomy, tabRefresh);

        // Override the internal Lumo CSS variables that vaadin-tabs use inside their shadow DOM
        tabs.getStyle().set("--lumo-font-size-m", "var(--lumo-font-size-xl)"); // Make text large (H2/H3 scale)
        tabs.getStyle().set("--lumo-secondary-text-color", "var(--lumo-body-text-color)"); // Make inactive tabs black instead of grey/blue

        // Group Logo and Nav together on the left
        HorizontalLayout leftGroup = new HorizontalLayout(logoLayout, tabs);
        leftGroup.setAlignItems(Alignment.CENTER);
        leftGroup.getStyle().set("gap", "48px"); // Exactly 48px distance as requested

        header.add(leftGroup);

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

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (!event.getActiveChain().isEmpty()) {
            com.vaadin.flow.component.HasElement activeView = event.getActiveChain().getFirst();
            Tab selectedTab = tabMap.get(activeView.getClass());
            if (selectedTab != null) {
                tabs.setSelectedTab(selectedTab);
            }
        }
    }
}
