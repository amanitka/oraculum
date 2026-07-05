package com.oraculum.security.config;

import com.oraculum.security.service.OraculumOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OraculumOAuth2UserService oauth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        // OAuth2 specific configuration
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(oauth2UserService)
                )
        );
        
        http.logout(logout -> logout
                .logoutSuccessUrl("/")
        );

        // Apply Vaadin security
        http.with(com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .loginView("/login")
        );

        return http.build();
    }
}
