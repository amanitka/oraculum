package com.oraculum.ui;

import com.oraculum.ui.views.AnalysisView;
import com.oraculum.ui.views.CompanyView;
import com.oraculum.ui.views.RefreshView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.HashMap;
import java.util.Map;

public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final Tabs navigationTabs = new Tabs();
    private final Map<Class<?>, Tab> tabToViewMap = new HashMap<>();

    public MainLayout() {
        setDrawerOpened(false); // Completely disable the drawer
        createHeader();
    }

    private void createHeader() {
        // Logo
        H1 logo = new H1("Oraculum");
        logo.addClassNames(LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.PRIMARY);

        // Wrap logo in a layout to enforce spacing/alignment
        HorizontalLayout logoLayout = new HorizontalLayout(logo);
        logoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        logoLayout.setPadding(true);
        logoLayout.setWidth("200px");

        // Navigation Menu (Tabs)
        navigationTabs.add(
                createTab("Company", CompanyView.class, VaadinIcon.OFFICE),
                createTab("Analysis", AnalysisView.class, VaadinIcon.CHART_LINE),
                createTab("Refresh", RefreshView.class, VaadinIcon.REFRESH)
        );
        navigationTabs.addClassName(LumoUtility.Margin.Horizontal.AUTO); // Centers the tabs

        // Theme toggle button
        Button themeToggle = new Button(new Icon(VaadinIcon.MOON));
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.addClickListener(e -> {
            var themeList = UI.getCurrent().getElement().getThemeList();
            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK);
                themeList.add(Lumo.LIGHT);
                themeToggle.setIcon(new Icon(VaadinIcon.MOON));
            } else {
                themeList.remove(Lumo.LIGHT);
                themeList.add(Lumo.DARK);
                themeToggle.setIcon(new Icon(VaadinIcon.SUN_O));
            }
        });

        HorizontalLayout rightSideLayout = new HorizontalLayout(themeToggle);
        rightSideLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSideLayout.setPadding(true);
        rightSideLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        rightSideLayout.setWidth("200px"); // Balance the logo width for centering tabs

        HorizontalLayout header = new HorizontalLayout(logoLayout, navigationTabs, rightSideLayout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Background.BASE, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10);

        addToNavbar(true, header);
    }

    private Tab createTab(String viewName, Class<? extends com.vaadin.flow.component.Component> viewClass, VaadinIcon icon) {
        RouterLink link = new RouterLink();
        link.setRoute(viewClass);
        link.add(new Icon(icon), new Span(viewName));

        // Styling the RouterLink to look like a tab item using Lumo utility classes
        link.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.TextColor.SECONDARY,
                "no-underline",
                LumoUtility.FontWeight.MEDIUM
        );

        Tab tab = new Tab(link);
        tabToViewMap.put(viewClass, tab);
        return tab;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Find the active tab based on the current active view class
        Class<?> activeViewLocation = event.getActiveChain().get(0).getClass();
        Tab activeTab = tabToViewMap.get(activeViewLocation);
        if (activeTab != null) {
            navigationTabs.setSelectedTab(activeTab);

            // Reapply styling to ensure active tab stands out visually
            navigationTabs.getChildren().forEach(component -> {
                if (component instanceof Tab t && t.getChildren().findFirst().orElse(null) instanceof RouterLink link) {
                    if (t.equals(activeTab)) {
                        link.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.BOLD);
                        link.removeClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);
                    } else {
                        link.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);
                        link.removeClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.BOLD);
                    }
                }
            });
        }
    }
}
