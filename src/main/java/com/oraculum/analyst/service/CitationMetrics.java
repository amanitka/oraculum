package com.oraculum.analyst.service;

import lombok.Getter;

@Getter
public class CitationMetrics {
    private int totalCitations = 0;
    private int validCitations = 0;
    private int unverifiedCitations = 0;
    private int brokenCitations = 0;

    public void incrementValid() {
        totalCitations++;
        validCitations++;
    }

    public void incrementUnverified() {
        totalCitations++;
        unverifiedCitations++;
    }

    public void incrementBroken() {
        totalCitations++;
        brokenCitations++;
    }

    public String getSummary() {
        if (totalCitations == 0) {
            return "No citations found.";
        }
        int flagRate = (unverifiedCitations + brokenCitations) * 100 / totalCitations;
        return String.format("%d%% valid (%d/%d), %d unverified, %d broken (Flag rate: %d%%)", 
                (validCitations * 100 / totalCitations), validCitations, totalCitations, unverifiedCitations, brokenCitations, flagRate);
    }
}
