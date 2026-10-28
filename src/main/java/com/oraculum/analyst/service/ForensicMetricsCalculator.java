package com.oraculum.analyst.service;

import org.springframework.stereotype.Service;

@Service
public class ForensicMetricsCalculator {

    public Float calculateNonOperatingIncomeSharePretaxPct(Float pretaxIncome, Float operatingIncome) {
        if (pretaxIncome == null || operatingIncome == null || Math.abs(pretaxIncome) < 1e-6) {
            return null;
        }
        return ((pretaxIncome - operatingIncome) / Math.abs(pretaxIncome)) * 100.0f;
    }

    public Float calculateShareholderPayoutToFcfRatioPct(Float fcf, Float repurchaseOfEquity, Float dividendsPaid) {
        if (fcf == null || fcf <= 0) {
            return null;
        }
        float repurchase = repurchaseOfEquity != null ? Math.abs(repurchaseOfEquity) : 0.0f;
        float dividends = dividendsPaid != null ? Math.abs(dividendsPaid) : 0.0f;
        float totalPayout = repurchase + dividends;
        if (totalPayout <= 0) {
            return null;
        }
        return (totalPayout / fcf) * 100.0f;
    }

    public Float calculateNonCapexInvestingOutflow(Float netCashFlowInvesting, Float capex) {
        if (netCashFlowInvesting == null || capex == null) {
            return null;
        }
        return Math.abs(netCashFlowInvesting) - Math.abs(capex);
    }

    public Float calculateNonCapexInvestingToFcfRatioPct(Float nonCapexOutflow, Float fcf) {
        if (nonCapexOutflow == null || fcf == null || fcf <= 0) {
            return null;
        }
        return (nonCapexOutflow / fcf) * 100.0f;
    }
}
