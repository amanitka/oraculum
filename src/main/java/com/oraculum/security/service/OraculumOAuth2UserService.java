package com.oraculum.security.service;

import com.oraculum.user.api.UserAuthApi;
import com.oraculum.user.api.dto.OAuth2LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Slf4j
@Service
@RequiredArgsConstructor
public class OraculumOAuth2UserService extends DefaultOAuth2UserService {

    private final UserAuthApi userAuthApi;

    @Override
    @Transactional
    public @NonNull OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (email == null) {
            log.error("OAuth2 authentication failed: email not provided by IdP {}", provider);
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Email not found from OAuth2 provider", ""));
        }

        OAuth2LoginRequest loginRequest = new OAuth2LoginRequest(email, firstName, lastName, provider, oAuth2User.getAttributes());
        return userAuthApi.processOAuth2Login(loginRequest);
    }
}
