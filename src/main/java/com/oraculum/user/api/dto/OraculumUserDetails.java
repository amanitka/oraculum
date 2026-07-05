package com.oraculum.user.api.dto;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class OraculumUserDetails implements OAuth2User {

    private final Long id;
    private final String email;
    private final String displayName;
    private final String role;
    @Getter
    private final AnalysisLimit limit;
    private final Map<String, Object> attributes;

    public OraculumUserDetails(Long id, String email, String displayName, String role, AnalysisLimit limit, Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.limit = limit;
        this.attributes = attributes;
    }

    public @NonNull Long getId() {
        return id;
    }

    public @NonNull String getDisplayName() {
        return displayName;
    }

    public @NonNull String getRole() {
        return role;
    }

    @Override
    public @NonNull Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public @NonNull String getName() {
        return email;
    }
}
