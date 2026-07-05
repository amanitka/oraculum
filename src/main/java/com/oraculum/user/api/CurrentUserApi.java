package com.oraculum.user.api;

import com.oraculum.user.api.dto.OraculumUserDetails;
import java.util.Optional;

public interface CurrentUserApi {
    
    /**
     * Retrieves the currently authenticated user details safely.
     * Handles potential nulls and casting exceptions.
     *
     * @return an Optional containing the OraculumUserDetails if authenticated, or empty otherwise.
     */
    Optional<OraculumUserDetails> getCurrentUser();
    
    /**
     * Retrieves the currently authenticated user details or throws an exception.
     * Useful for backend services where the user is guaranteed to be authenticated by outer layers.
     *
     * @return the OraculumUserDetails
     * @throws IllegalStateException if the user is not authenticated
     */
    default OraculumUserDetails getCurrentUserOrThrow() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }
}
