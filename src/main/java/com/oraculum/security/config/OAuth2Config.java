package com.oraculum.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(OAuth2Properties.class)
public class OAuth2Config {

    private void registerKeycloak(List<ClientRegistration> registrations, OAuth2Properties properties) {
        if (properties.keycloak() != null && properties.keycloak().isConfigured()) {
            log.info("Registering Keycloak OAuth2 Client using custom environment variables.");
            ClientRegistration keycloak = ClientRegistrations.fromIssuerLocation(properties.keycloak().issuerUri())
                    .registrationId("keycloak")
                    .clientId(properties.keycloak().clientId())
                    .clientSecret(properties.keycloak().clientSecret())
                    .scope(properties.keycloak().scopes() != null ? properties.keycloak().scopes().toArray(new String[0]) : new String[]{"openid", "profile", "email"})
                    .build();
            registrations.add(keycloak);
        }
    }

    private void registerGoogle(List<ClientRegistration> registrations, OAuth2Properties properties) {
        if (properties.google() != null && properties.google().isConfigured()) {
            log.info("Registering Google OAuth2 Client using custom environment variables.");
            ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(properties.google().clientId())
                    .clientSecret(properties.google().clientSecret())
                    .scope(properties.google().scopes() != null ? properties.google().scopes().toArray(new String[0]) : new String[]{"openid", "profile", "email"})
                    .build();
            registrations.add(google);
        }
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2Properties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        registerKeycloak(registrations, properties);
        registerGoogle(registrations, properties);

        if (registrations.isEmpty()) {
            throw new IllegalStateException("No OAuth2 providers configured! You must set either Keycloak or Google environment variables to log into Oraculum.");
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }
}
