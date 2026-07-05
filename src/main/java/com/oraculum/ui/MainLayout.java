package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.EconomyView;
import com.oraculum.ui.views.RefreshView;
import com.oraculum.ui.views.ScreenerView;
import com.oraculum.user.api.CurrentUserApi;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.avatar.Avatar;
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

import com.oraculum.analyst.api.AnalysisUsageApi;
import com.oraculum.analyst.api.dto.UserAnalysisUsage;
import com.vaadin.flow.component.html.Span;


public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final java.util.Map<Class<?>, Tab> tabMap = new java.util.HashMap<>();
    private Tabs tabs;
    private final AnalysisUsageApi analysisUsageApi;
    private final CurrentUserApi currentUserApi;

    public MainLayout(AnalysisUsageApi analysisUsageApi, CurrentUserApi currentUserApi) {
        this.analysisUsageApi = analysisUsageApi;
        this.currentUserApi = currentUserApi;
        setPrimarySection(Section.NAVBAR);
        setDrawerOpened(false);
        addHeaderContent();
    }

    private void addHeaderContent() {
        Div outerHeader = new Div();
        outerHeader.setWidthFull();
        outerHeader.getStyle().set("display", "flex");
        outerHeader.getStyle().set("justify-content", "center");
        outerHeader.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setMaxWidth("1440px");
        header.setPadding(false);
        header.addClassNames(LumoUtility.Padding.Horizontal.LARGE);
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout leftGroup = new HorizontalLayout(createLogoLayout(), createNavigationTabs());
        leftGroup.setAlignItems(Alignment.CENTER);
        leftGroup.getStyle().set("gap", "48px");

        header.add(leftGroup, createUserProfileGroup());
        outerHeader.add(header);
        addToNavbar(true, outerHeader);
    }

    private HorizontalLayout createLogoLayout() {
        HorizontalLayout logoLayout = new HorizontalLayout();
        logoLayout.setAlignItems(Alignment.CENTER);
        logoLayout.addClassNames(LumoUtility.Gap.SMALL);

        H1 viewTitle = new H1("oraculum");
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.TextColor.BODY,
                LumoUtility.FontWeight.BOLD);

        Image logo = new Image("images/logo.svg", "Oraculum Logo");
        logo.setHeight("30px");
        logo.getStyle().set("margin-top", "6px");

        logoLayout.add(logo, viewTitle);
        return logoLayout;
    }

    private Tabs createNavigationTabs() {
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
        tabs.getStyle().set("--lumo-font-size-m", "var(--lumo-font-size-xl)");
        tabs.getStyle().set("--lumo-secondary-text-color", "var(--lumo-body-text-color)");

        return tabs;
    }

    private HorizontalLayout createUserProfileGroup() {
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setAlignItems(Alignment.CENTER);
        rightGroup.setSpacing(true);

        currentUserApi.getCurrentUser().ifPresent(userDetails -> {
            String displayName = userDetails.getDisplayName();
            String role = userDetails.getRole();
            UserAnalysisUsage usage = analysisUsageApi.getUsage(userDetails.getId(), userDetails.getLimit());

            Avatar avatar = new Avatar(displayName);
            rightGroup.add(avatar);

            if (usage != null && usage.isLimited()) {
                Span usageBadge = new Span("Analyses: " + usage.usedAnalyses() + " / " + usage.limitCount());
                usageBadge.getElement().getThemeList().addAll(java.util.List.of("badge", usage.isExceeded() ? "error" : "success"));
                rightGroup.add(usageBadge);
            } else if (usage != null) {
                Span unlimitedBadge = new Span("Analyses: Unlimited");
                unlimitedBadge.getElement().getThemeList().addAll(java.util.List.of("badge", "success"));
                rightGroup.add(unlimitedBadge);
            }

            if ("ADMIN".equals(role)) {
                RouterLink adminLink = new RouterLink("Admin", com.oraculum.ui.views.UserManagementView.class);
                adminLink.getStyle().set("text-decoration", "none");
                adminLink.getStyle().set("margin-right", "10px");
                rightGroup.add(adminLink);
            }

            Span userName = new Span(displayName);
            userName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Right.SMALL);
            rightGroup.add(userName);
        });

        com.vaadin.flow.component.html.Anchor logoutLink = new com.vaadin.flow.component.html.Anchor("/logout", "Logout");
        logoutLink.getStyle().set("text-decoration", "none");
        logoutLink.getStyle().set("color", "var(--lumo-error-text-color)");
        rightGroup.add(logoutLink);

        return rightGroup;
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
