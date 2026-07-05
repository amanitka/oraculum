package com.oraculum.analyst.api.dto;

public record UserAnalysisUsage(long usedAnalyses, Long limitCount, String period) {
    public boolean isLimited() {
        return limitCount != null;
    }
    
    public boolean isExceeded() {
        return isLimited() && usedAnalyses >= limitCount;
    }
}
