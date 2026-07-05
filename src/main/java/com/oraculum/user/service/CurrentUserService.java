package com.oraculum.user.service;

import com.oraculum.user.api.CurrentUserApi;
import com.oraculum.user.api.dto.OraculumUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class CurrentUserService implements CurrentUserApi {

    @Override
    public Optional<OraculumUserDetails> getCurrentUser() {
        try {
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = context.getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof OraculumUserDetails userDetails) {
                return Optional.of(userDetails);
            }
        } catch (Exception e) {
            log.warn("Exception occurred while retrieving current user from SecurityContext", e);
        }
        return Optional.empty();
    }
}
