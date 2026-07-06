package com.oraculum.ui.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Oraculum")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        // Add a very subtle gradient background to the entire page
        getStyle().set("background", "linear-gradient(135deg, var(--lumo-shade-5pct) 0%, var(--lumo-base-color) 100%)");

        // Main Login Card
        VerticalLayout loginCard = new VerticalLayout();
        loginCard.setWidth("400px");
        loginCard.setPadding(true);
        loginCard.setSpacing(true);
        loginCard.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // Glassmorphism styling for the card
        loginCard.getStyle().set("background", "var(--lumo-base-color)");
        loginCard.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        loginCard.getStyle().set("box-shadow", "0 10px 30px rgba(0,0,0,0.1), 0 1px 3px rgba(0,0,0,0.05)");
        loginCard.getStyle().set("padding", "var(--lumo-space-xl)");

        // Logo & Title
        com.vaadin.flow.component.html.Image logo = new com.vaadin.flow.component.html.Image("/images/logo.svg", "Oraculum Logo");
        logo.setHeight("48px");
        logo.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        H1 title = new H1("Oraculum");
        title.getStyle().set("margin", "0");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
        title.getStyle().set("font-weight", "800");

        H3 subtitle = new H3("Welcome back. Please log in.");
        subtitle.getStyle().set("margin-top", "var(--lumo-space-xs)");
        subtitle.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("font-weight", "400");
        subtitle.getStyle().set("font-size", "var(--lumo-font-size-m)");

        // Social Login Container
        com.vaadin.flow.component.orderedlayout.HorizontalLayout buttonContainer = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        buttonContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        buttonContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonContainer.setSpacing(false);
        buttonContainer.getStyle().set("gap", "var(--lumo-space-l)");
        buttonContainer.setWidthFull();

        // Google Button
        Anchor googleLogin = new Anchor("/oauth2/authorization/google");
        googleLogin.getElement().setAttribute("router-ignore", true);
        
        com.vaadin.flow.component.button.Button googleBtn = new com.vaadin.flow.component.button.Button();
        googleBtn.setWidth("56px");
        googleBtn.setHeight("56px");
        googleBtn.getStyle().set("border-radius", "50%");
        googleBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        googleBtn.getStyle().set("background", "var(--lumo-base-color)");
        googleBtn.getStyle().set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)");
        googleBtn.getStyle().set("cursor", "pointer");
        
        com.vaadin.flow.component.html.Image googleIcon = new com.vaadin.flow.component.html.Image("/images/google.svg", "Google");
        googleIcon.setHeight("28px");
        googleBtn.setIcon(googleIcon);
        googleLogin.add(googleBtn);

        // Keycloak Button
        Anchor keycloakLogin = new Anchor("/oauth2/authorization/keycloak");
        keycloakLogin.getElement().setAttribute("router-ignore", true);

        com.vaadin.flow.component.button.Button keycloakBtn = new com.vaadin.flow.component.button.Button();
        keycloakBtn.setWidth("56px");
        keycloakBtn.setHeight("56px");
        keycloakBtn.getStyle().set("border-radius", "50%");
        keycloakBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        keycloakBtn.getStyle().set("background", "var(--lumo-base-color)");
        keycloakBtn.getStyle().set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)");
        keycloakBtn.getStyle().set("cursor", "pointer");

        com.vaadin.flow.component.html.Image keycloakIcon = new com.vaadin.flow.component.html.Image("/images/keycloak.svg", "Keycloak");
        keycloakIcon.setHeight("28px");
        keycloakBtn.setIcon(keycloakIcon);
        keycloakLogin.add(keycloakBtn);

        buttonContainer.add(googleLogin, keycloakLogin);
        loginCard.add(logo, title, subtitle, buttonContainer);
        add(loginCard);
    }
}
