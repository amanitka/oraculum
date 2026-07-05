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

        H1 title = new H1("Oraculum");
        H3 subtitle = new H3("Please log in to continue");

        Anchor keycloakLogin = new Anchor("/oauth2/authorization/keycloak", "Login with Keycloak (Admin)");
        keycloakLogin.getElement().setAttribute("router-ignore", true);
        keycloakLogin.getElement().getThemeList().add("button");
        keycloakLogin.getElement().getThemeList().add("primary");
        keycloakLogin.getStyle().set("margin-top", "20px");
        keycloakLogin.getStyle().set("width", "250px");
        keycloakLogin.getStyle().set("text-align", "center");

        Anchor googleLogin = new Anchor("/oauth2/authorization/google", "Login with Google");
        googleLogin.getElement().setAttribute("router-ignore", true);
        googleLogin.getElement().getThemeList().add("button");
        googleLogin.getStyle().set("margin-top", "10px");
        googleLogin.getStyle().set("width", "250px");
        googleLogin.getStyle().set("text-align", "center");

        add(title, subtitle, keycloakLogin, googleLogin);
    }
}
