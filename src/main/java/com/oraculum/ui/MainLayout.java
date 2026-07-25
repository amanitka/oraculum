package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.EconomyView;
import com.oraculum.ui.views.MarketMapView;
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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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
        RouterLink marketMapLink = new RouterLink("Market Map", MarketMapView.class);
        RouterLink analysisLink = new RouterLink("Analysis", AnalysisView.class);
        RouterLink companyLink = new RouterLink("Company", CompanyView.class);
        RouterLink economyLink = new RouterLink("Economy", EconomyView.class);

        screenerLink.getStyle().set("text-decoration", "none");
        marketMapLink.getStyle().set("text-decoration", "none");
        analysisLink.getStyle().set("text-decoration", "none");
        companyLink.getStyle().set("text-decoration", "none");
        economyLink.getStyle().set("text-decoration", "none");

        Tab tabScreener = new Tab(screenerLink);
        Tab tabMarketMap = new Tab(marketMapLink);
        Tab tabAnalysis = new Tab(analysisLink);
        Tab tabCompany = new Tab(companyLink);
        Tab tabEconomy = new Tab(economyLink);

        tabMap.put(ScreenerView.class, tabScreener);
        tabMap.put(MarketMapView.class, tabMarketMap);
        tabMap.put(AnalysisView.class, tabAnalysis);
        tabMap.put(CompanyView.class, tabCompany);
        tabMap.put(EconomyView.class, tabEconomy);

        tabs = new Tabs(tabScreener, tabMarketMap, tabAnalysis, tabCompany, tabEconomy);
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

            // Add Glossary Button
            Button glossaryBtn = new Button(VaadinIcon.QUESTION_CIRCLE_O.create());
            glossaryBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            glossaryBtn.setTooltipText("Glossary & Help");
            glossaryBtn.addClickListener(_ -> openGlossary());
            glossaryBtn.getStyle().set("margin-right", "1rem");

            rightGroup.add(glossaryBtn, avatarWrapper);
        });

        return rightGroup;
    }

    private void openGlossary() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Financial Glossary");
        dialog.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(false);
        
        layout.add(createGlossaryItem("Ranks & Scores", null));
        layout.add(createGlossaryItem("Q-Rank (Quality)", "Percentile rank (0-100) comparing a company's profitability and balance sheet strength against its sector."));
        layout.add(createGlossaryItem("V-Rank (Value)", "Percentile rank (0-100) comparing a company's valuation multiples (P/E, EV/EBITDA, P/FCF) against its sector."));
        layout.add(createGlossaryItem("F-Rank (Financial Trend)", "Percentile rank (0-100) comparing a company's fundamental improvement against its sector."));
        layout.add(createGlossaryItem("Trend Score", "Absolute score (0-9) measuring fundamental business improvements (profitability, leverage, liquidity) based on the Piotroski F-Score methodology."));
        
        layout.add(createGlossaryItem("Market Metrics", null));
        layout.add(createGlossaryItem("Vol Velocity", "Ratio of the current trading volume to the historical average volume. Values > 1.0x indicate higher than normal trading activity."));
        layout.add(createGlossaryItem("Reverse DCF", "A reverse discounted cash flow analysis that calculates the implied future growth rate currently priced into the stock."));
        
        layout.add(createGlossaryItem("Signals", null));
        layout.add(createGlossaryItem("Screener Signal", "Composite investment signal derived from combined Q-Rank, V-Rank, and F-Rank scores (Strong Buy, Buy, Neutral, Avoid)."));
        layout.add(createGlossaryItem("News Sentiment", "Aggregated sentiment score from recent financial news coverage."));
        
        dialog.add(layout);
        
        Button closeBtn = new Button("Close", _ -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);
        
        dialog.open();
    }
    
    private Component createGlossaryItem(String term, String desc) {
        if (desc == null) {
            com.vaadin.flow.component.html.H4 header = new com.vaadin.flow.component.html.H4(term);
            header.getStyle().set("margin-top", "1.5rem").set("margin-bottom", "0.5rem").set("color", "var(--lumo-primary-text-color)");
            return header;
        }
        Span termSpan = new Span(term + ": ");
        termSpan.getStyle().set("font-weight", "600");
        Span descSpan = new Span(desc);
        com.vaadin.flow.component.html.Paragraph p = new com.vaadin.flow.component.html.Paragraph(termSpan, descSpan);
        p.getStyle().set("margin-top", "0.2rem").set("margin-bottom", "0.4rem").set("line-height", "1.5").set("color", "var(--lumo-secondary-text-color)");
        return p;
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
