package com.oraculum.analyst.api;

import com.oraculum.analyst.api.dto.UserAnalysisUsage;
import com.oraculum.user.api.dto.AnalysisLimit;

public interface AnalysisUsageApi {
    
    /**
     * Checks if the given user has exceeded their analysis limit.
     * Throws an exception if the limit is exceeded.
     */
    void checkLimit(Long userId, AnalysisLimit limit);

    /**
     * Gets the current usage for a user based on their limit.
     */
    UserAnalysisUsage getUsage(Long userId, AnalysisLimit limit);
}
