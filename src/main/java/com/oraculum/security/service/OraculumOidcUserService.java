package com.oraculum.security.service;

import com.oraculum.user.api.UserAuthApi;
import com.oraculum.user.api.dto.OAuth2LoginRequest;
import com.oraculum.user.api.dto.OraculumUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OraculumOidcUserService extends OidcUserService {

    private final UserAuthApi userAuthApi;

    private OraculumUserDetails buildUserDetails(OraculumUserDetails userDetails, OidcUser oidcUser) {
        return new OraculumUserDetails(
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getDisplayName(),
                userDetails.getRole(),
                userDetails.getLimit(),
                userDetails.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    @Override
    @Transactional
    public @NonNull OidcUser loadUser(@NonNull OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();
        if (email == null) {
            log.error("OIDC authentication failed: email not provided by IdP {}", provider);
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Email not found from OIDC provider", ""));
        }

        OAuth2LoginRequest loginRequest = new OAuth2LoginRequest(email, firstName, lastName, provider, oidcUser.getAttributes());
        OraculumUserDetails userDetails = userAuthApi.processOAuth2Login(loginRequest);

        return buildUserDetails(userDetails, oidcUser);
    }
}
