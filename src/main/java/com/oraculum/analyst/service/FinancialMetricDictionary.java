package com.oraculum.analyst.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FinancialMetricDictionary {

    private static final Map<String, List<String>> METRIC_MAP = createMetricMap();

    public List<String> getCandidateProperties(String context) {
        if (context == null || context.isBlank()) {
            return Collections.emptyList();
        }
        String lowerContext = context.toLowerCase();
        List<String> properties = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : METRIC_MAP.entrySet()) {
            if (containsTerm(lowerContext, entry.getKey())) {
                properties.addAll(entry.getValue());
            }
        }
        return properties;
    }

    private boolean containsTerm(String text, String term) {
        int idx = text.indexOf(term);
        if (idx == -1) return false;
        boolean startOk = (idx == 0) || !Character.isLetterOrDigit(text.charAt(idx - 1));
        int endIdx = idx + term.length();
        boolean endOk = (endIdx == text.length()) || !Character.isLetterOrDigit(text.charAt(endIdx));
        return startOk && endOk;
    }

    private static Map<String, List<String>> createMetricMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        // Working Capital & Efficiency
        map.put("dso", List.of("daysSalesOutstanding", "dso"));
        map.put("days sales outstanding", List.of("daysSalesOutstanding", "dso"));
        map.put("dio", List.of("daysInventoryOutstanding", "dio"));
        map.put("days inventory outstanding", List.of("daysInventoryOutstanding", "dio"));
        map.put("ccc", List.of("cashConversionCycle"));
        map.put("cash conversion cycle", List.of("cashConversionCycle"));

        // Cash Flow Statement (Alpha Vantage, SimFin, FMP schemas)
        map.put("operating cash flow", List.of("Net Cash from Operating Activities", "operatingCashFlow", "ocf", "netOperatingCashFlow"));
        map.put("operating cash", List.of("Net Cash from Operating Activities", "operatingCashFlow", "ocf"));
        map.put("cash from operations", List.of("Net Cash from Operating Activities", "operatingCashFlow", "ocf"));
        map.put("ocf", List.of("Net Cash from Operating Activities", "operatingCashFlow", "ocf"));
        map.put("capex", List.of("Change in Fixed Assets & Intangibles", "capitalExpenditures", "capex"));
        map.put("capital expenditures", List.of("Change in Fixed Assets & Intangibles", "capitalExpenditures", "capex"));
        map.put("capital spending", List.of("Change in Fixed Assets & Intangibles", "capitalExpenditures", "capex"));
        map.put("free cash flow", List.of("freeCashFlow", "fcf"));
        map.put("fcf", List.of("freeCashFlow", "fcf"));
        map.put("working capital", List.of("Change in Working Capital", "workingCapital"));
        map.put("dividends paid", List.of("Dividends Paid", "dividendsPaid"));
        map.put("dividends", List.of("Dividends Paid", "dividendsPaid"));
        map.put("share repurchases", List.of("Cash from (Repurchase of) Equity", "repurchaseOfEquity", "shareRepurchase"));
        map.put("buybacks", List.of("Cash from (Repurchase of) Equity", "repurchaseOfEquity", "shareRepurchase"));

        // Ratios & Balance Sheet
        map.put("current ratio", List.of("currentRatio"));
        map.put("quick ratio", List.of("quickRatio"));
        map.put("debt to equity", List.of("debtToEquity", "debtEquityRatio"));
        map.put("d/e", List.of("debtToEquity", "debtEquityRatio"));
        map.put("interest coverage", List.of("interestCoverageRatio", "interestCoverage"));
        map.put("gross margin", List.of("grossMargin", "gross_margin", "gross_margin_pct"));
        map.put("operating margin", List.of("operatingMargin", "operating_margin", "operating_margin_pct"));
        map.put("net margin", List.of("netMargin", "net_margin", "profitMargin", "net_margin_pct"));
        map.put("fcf margin", List.of("fcfMargin", "fcf_margin"));
        map.put("ebitda margin", List.of("ebitdaMargin", "ebitda_margin"));
        map.put("margin", List.of("grossMargin", "operatingMargin", "netMargin", "profitMargin", "gross_margin", "operating_margin", "net_margin"));
        map.put("p/e", List.of("peRatio", "priceEarningsRatio", "forwardPE", "trailingPE"));
        map.put("pe ratio", List.of("peRatio", "priceEarningsRatio", "forwardPE", "trailingPE"));
        map.put("price to earnings", List.of("peRatio", "priceEarningsRatio", "forwardPE", "trailingPE"));
        map.put("p/s", List.of("psRatio", "priceToSales"));
        map.put("p/b", List.of("pbRatio", "priceToBook"));
        map.put("roe", List.of("returnOnEquity", "roe"));
        map.put("return on equity", List.of("returnOnEquity", "roe"));
        map.put("roa", List.of("returnOnAssets", "roa"));
        map.put("return on assets", List.of("returnOnAssets", "roa"));
        map.put("roic", List.of("roic", "returnOnInvestedCapital"));
        map.put("earnings quality", List.of("earningsQualityRatio"));
        map.put("capex to revenue", List.of("capexToRevenue"));
        map.put("asset turnover", List.of("assetTurnover"));

        // Growth metrics
        map.put("revenue growth", List.of("revenueYoyGrowth", "revenue_growth_yoy_pct", "growth_yoy_pct"));
        map.put("net income growth", List.of("netIncomeYoyGrowth"));
        map.put("ebitda growth", List.of("ebitdaYoyGrowth"));
        map.put("fcf growth", List.of("fcfYoyGrowth"));

        // Consensus & Revisions
        map.put("revisions up", List.of("eps_estimate_revision_up_trailing_30_days", "revenue_estimate_revision_up_trailing_30_days"));
        map.put("revised up", List.of("eps_estimate_revision_up_trailing_30_days", "revenue_estimate_revision_up_trailing_30_days"));
        map.put("revisions down", List.of("eps_estimate_revision_down_trailing_30_days", "revenue_estimate_revision_down_trailing_30_days"));
        map.put("revised down", List.of("eps_estimate_revision_down_trailing_30_days", "revenue_estimate_revision_down_trailing_30_days"));
        map.put("revisions", List.of("eps_estimate_revision_up_trailing_30_days", "eps_estimate_revision_down_trailing_30_days"));
        map.put("eps estimate", List.of("eps_estimate_average", "eps_estimate_high", "eps_estimate_low"));
        map.put("eps consensus", List.of("eps_estimate_average"));
        map.put("eps", List.of("eps", "earningsPerShare", "dilutedEps", "basicEps", "eps_estimate_average"));
        map.put("revenue consensus", List.of("revenue_estimate_average"));
        map.put("revenue", List.of("revenue", "totalRevenue", "sales", "revenue_estimate_average"));

        // Insider Trading & 13F Institutional
        map.put("insider sells", List.of("sellsValue3m", "sellsValue6m", "sellsShares3m", "sellsCount3m"));
        map.put("insider sales", List.of("sellsValue3m", "sellsValue6m", "sellsShares3m", "sellsCount3m"));
        map.put("insider buys", List.of("buysValue3m", "buysValue6m", "buysShares3m", "buysCount3m"));
        map.put("institutional shares", List.of("totalInstShares"));
        map.put("institutional value", List.of("totalInstValueUsd"));
        map.put("holders", List.of("totalHoldersCount"));
        map.put("top 5", List.of("top5HoldersPct", "top5HoldersValueUsd"));
        map.put("funds buying", List.of("fundsBuyingCount"));
        map.put("funds selling", List.of("fundsSellingCount"));

        // Macro (FRED)
        List<String> commonFredProps = List.of("latestValue", "value1yAgo", "yoyChangePct", "yoyChangeBps", "value", "previous_value");
        map.put("gdp", combine(List.of("GDPC1", "gdp", "gross domestic product"), commonFredProps));
        map.put("durable goods", combine(List.of("DGORDER", "durable goods"), commonFredProps));
        map.put("m2", combine(List.of("M2SL", "m2 money supply", "m2"), commonFredProps));
        map.put("fed funds", combine(List.of("FEDFUNDS", "fed funds"), commonFredProps));
        map.put("treasury", combine(List.of("GS10", "DGS10", "treasury"), commonFredProps));
        map.put("wti", combine(List.of("DCOILWTICO", "wti", "crude oil"), commonFredProps));
        map.put("crude oil", combine(List.of("DCOILWTICO", "wti", "crude oil"), commonFredProps));
        map.put("industrial production", combine(List.of("INDPRO", "industrial production"), commonFredProps));
        map.put("consumer sentiment", combine(List.of("UMCSENT", "consumer sentiment"), commonFredProps));
        map.put("housing starts", combine(List.of("HOUST", "housing starts"), commonFredProps));
        map.put("unemployment", combine(List.of("UNRATE", "unemployment"), commonFredProps));
        map.put("ppi", combine(List.of("PPIACO", "producer price", "ppi"), commonFredProps));
        map.put("retail sales", combine(List.of("RSAFS", "retail sales"), commonFredProps));
        map.put("mortgage", combine(List.of("MORTGAGE30US", "mortgage"), commonFredProps));

        return Collections.unmodifiableMap(map);
    }

    private static List<String> combine(List<String> list1, List<String> list2) {
        List<String> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }
}
