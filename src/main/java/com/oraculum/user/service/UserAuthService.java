package com.oraculum.user.service;

import com.oraculum.user.api.UserAuthApi;
import com.oraculum.user.api.dto.OAuth2LoginRequest;
import com.oraculum.user.api.dto.OraculumUserDetails;
import com.oraculum.user.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService implements UserAuthApi {

    private final UserService userService;

    private OraculumUserDetails createBootstrapAdminUser(OAuth2LoginRequest request) {
        UserEntity adminUser = userService.registerBootstrapAdmin(request.email(), request.firstName(), request.lastName(), request.provider());
        return new OraculumUserDetails(adminUser.getId(), adminUser.getEmail(), adminUser.getDisplayName(), adminUser.getRole().name(), adminUser.getParsedAnalysisLimit(), request.attributes());
    }
    
    @Override
    @Transactional
    public OraculumUserDetails processOAuth2Login(OAuth2LoginRequest request) {
        if (request.email() == null) {
            log.error("OAuth2 authentication failed: email not provided by IdP {}", request.provider());
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Email not found from OAuth2 provider", ""));
        }

        Optional<UserEntity> userOpt = userService.findByEmail(request.email());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (!user.isEnabled()) {
                log.warn("Access denied for disabled user: {}", request.email());
                throw new OAuth2AuthenticationException(new OAuth2Error("access_denied", "User account is disabled", ""));
            }
            user = userService.updateLoginDetails(user, request.firstName(), request.lastName(), request.provider());
            return new OraculumUserDetails(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name(), user.getParsedAnalysisLimit(), request.attributes());
        } else if (userService.getUserCount() == 0) {
            return createBootstrapAdminUser(request);
        } else {
            log.warn("Access denied for unregistered user: {} via {}", request.email(), request.provider());
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied", "User not registered in Oraculum", ""));
        }
    }    
}
