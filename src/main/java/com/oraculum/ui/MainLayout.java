package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.EconomyView;
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
import com.oraculum.user.api.UserManagementApi;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.ui.components.UserProfilePopover;
import com.vaadin.flow.component.shared.Tooltip;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Map;
import java.util.HashMap;

import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final Map<Class<?>, Tab> tabMap = new HashMap<>();
    private Tabs tabs;
    private final AnalysisUsageApi analysisUsageApi;
    private final CurrentUserApi currentUserApi;
    private final UserManagementApi userManagementApi;
    private final HarvesterBatchApi harvesterBatchApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CompanyMetadataApi companyMetadataApi;

    public MainLayout(AnalysisUsageApi analysisUsageApi,
                      CurrentUserApi currentUserApi,
                      UserManagementApi userManagementApi,
                      HarvesterBatchApi harvesterBatchApi,
                      ApplicationEventPublisher eventPublisher,
                      CompanyMetadataApi companyMetadataApi) {
        this.analysisUsageApi = analysisUsageApi;
        this.currentUserApi = currentUserApi;
        this.userManagementApi = userManagementApi;
        this.harvesterBatchApi = harvesterBatchApi;
        this.eventPublisher = eventPublisher;
        this.companyMetadataApi = companyMetadataApi;
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

        screenerLink.getStyle().set("text-decoration", "none");
        analysisLink.getStyle().set("text-decoration", "none");
        companyLink.getStyle().set("text-decoration", "none");
        economyLink.getStyle().set("text-decoration", "none");

        Tab tabScreener = new Tab(screenerLink);
        Tab tabAnalysis = new Tab(analysisLink);
        Tab tabCompany = new Tab(companyLink);
        Tab tabEconomy = new Tab(economyLink);

        tabMap.put(ScreenerView.class, tabScreener);
        tabMap.put(AnalysisView.class, tabAnalysis);
        tabMap.put(CompanyView.class, tabCompany);
        tabMap.put(EconomyView.class, tabEconomy);

        tabs = new Tabs(tabScreener, tabAnalysis, tabCompany, tabEconomy);
        tabs.getStyle().set("--lumo-font-size-m", "var(--lumo-font-size-xl)");
        tabs.getStyle().set("--lumo-secondary-text-color", "var(--lumo-body-text-color)");

        return tabs;
    }

    private HorizontalLayout createUserProfileGroup() {
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setAlignItems(Alignment.CENTER);

        currentUserApi.getCurrentUser().ifPresent(userDetails -> {
            String displayName = userDetails.getDisplayName();
            UserAnalysisUsage usage = analysisUsageApi.getUsage(userDetails.getId(), userDetails.getLimit());

            Avatar avatar = new Avatar(displayName);
            avatar.getStyle().set("cursor", "pointer");

            // Apply limit outline if exceeded
            if (usage != null && usage.isExceeded()) {
                avatar.getStyle().set("outline", "2px solid var(--lumo-error-color)");
                avatar.getStyle().set("outline-offset", "1px");
            }

            // Wrap in Span to add tooltip and popover
            Span avatarWrapper = new Span(avatar);
            avatarWrapper.getStyle().set("display", "inline-flex");
            avatarWrapper.getStyle().set("align-items", "center");
            avatarWrapper.getStyle().set("justify-content", "center");
            avatarWrapper.getStyle().set("cursor", "pointer");

            // Add tooltip with usage stats
            String tooltipText;
            if (usage != null && usage.isLimited()) {
                tooltipText = "Analyses: " + usage.usedAnalyses() + " / " + usage.limitCount();
            } else {
                tooltipText = "Analyses: Unlimited";
            }
            Tooltip.forComponent(avatarWrapper).setText(tooltipText);

            // Create popover anchored to the wrapper
            new UserProfilePopover(
                    avatarWrapper, userDetails, usage,
                    userManagementApi, harvesterBatchApi, eventPublisher, companyMetadataApi);

            rightGroup.add(avatarWrapper);
        });

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
