package com.oraculum.ui.components;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.harvester.api.HarvesterBatchApi;
import com.oraculum.user.api.UserManagementApi;
import com.oraculum.user.api.dto.OraculumUserDetails;
import com.oraculum.analyst.api.dto.UserAnalysisUsage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.context.ApplicationEventPublisher;

public class UserProfilePopover extends Popover {

    public UserProfilePopover(Span target,
                              OraculumUserDetails userDetails,
                              UserAnalysisUsage usage,
                              UserManagementApi userManagementApi,
                              HarvesterBatchApi harvesterBatchApi,
                              ApplicationEventPublisher eventPublisher,
                              CompanyMetadataApi companyMetadataApi) {
        setTarget(target);
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidth("260px");
        layout.setAlignItems(Alignment.START);

        // 1. Identity Section
        Span nameSpan = new Span(userDetails.getDisplayName());
        nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        
        Span emailSpan = new Span(userDetails.getName()); // getName() returns email in OraculumUserDetails
        emailSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        VerticalLayout idLayout = new VerticalLayout(nameSpan, emailSpan);
        idLayout.setPadding(false);
        idLayout.setSpacing(false);
        
        layout.add(idLayout);

        // 2. Role & Usage Badges
        Div badgesLayout = new Div();
        badgesLayout.getStyle().set("display", "flex");
        badgesLayout.getStyle().set("gap", "var(--lumo-space-s)");
        badgesLayout.getStyle().set("flex-wrap", "wrap");

        // Role Badge
        Span roleBadge = new Span(userDetails.getRole());
        roleBadge.getElement().getThemeList().addAll(java.util.List.of("badge", "contrast"));
        badgesLayout.add(roleBadge);

        // Usage Badge
        if (usage != null && usage.isLimited()) {
            Span usageBadge = new Span("Analyses: " + usage.usedAnalyses() + " / " + usage.limitCount());
            usageBadge.getElement().getThemeList().addAll(java.util.List.of("badge", usage.isExceeded() ? "error" : "success"));
            badgesLayout.add(usageBadge);
        } else if (usage != null) {
            Span unlimitedBadge = new Span("Analyses: Unlimited");
            unlimitedBadge.getElement().getThemeList().addAll(java.util.List.of("badge", "success"));
            badgesLayout.add(unlimitedBadge);
        }
        layout.add(badgesLayout);

        layout.add(new Hr());

        // 3. Administration (ADMIN only)
        if ("ADMIN".equals(userDetails.getRole())) {
            Button adminBtn = new Button("Administration", VaadinIcon.COG.create());
            adminBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            adminBtn.setWidthFull();
            adminBtn.getStyle().set("justify-content", "flex-start");
            adminBtn.getStyle().set("cursor", "pointer");
            adminBtn.addClickListener(_ -> {
                close();
                AdministrationDialog dialog = new AdministrationDialog(
                        userManagementApi, harvesterBatchApi, eventPublisher, companyMetadataApi);
                dialog.open();
            });
            layout.add(adminBtn);
            layout.add(new Hr());
        }

        // 4. Logout Link
        Anchor logoutLink = new Anchor("/logout", "Logout");
        logoutLink.getElement().setAttribute("router-ignore", "");
        logoutLink.getStyle().set("color", "var(--lumo-error-text-color)");
        logoutLink.getStyle().set("font-weight", "bold");
        logoutLink.getStyle().set("text-decoration", "none");
        logoutLink.getStyle().set("display", "flex");
        logoutLink.getStyle().set("align-items", "center");
        logoutLink.getStyle().set("gap", "var(--lumo-space-s)");
        logoutLink.getStyle().set("width", "100%");
        logoutLink.getStyle().set("padding", "var(--lumo-space-xs) 0");
        
        Span logoutIcon = new Span(VaadinIcon.SIGN_OUT.create());
        logoutIcon.getStyle().set("color", "var(--lumo-error-text-color)");
        
        logoutLink.add(logoutIcon);
        
        layout.add(logoutLink);

        add(layout);
    }
}
