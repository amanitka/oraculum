package com.oraculum.user.api;

import com.oraculum.user.api.dto.OAuth2LoginRequest;
import com.oraculum.user.api.dto.OraculumUserDetails;

public interface UserAuthApi {
    
    /**
     * Processes an OAuth2 login. Creates the user if they are the first user in the system.
     * Otherwise, validates the user exists and is enabled, updating their details.
     * 
     * @param request the login request containing OAuth2 profile data
     * @return the populated OraculumUserDetails
     */
    OraculumUserDetails processOAuth2Login(OAuth2LoginRequest request);
}
