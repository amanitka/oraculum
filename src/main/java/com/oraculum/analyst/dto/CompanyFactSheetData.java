package com.oraculum.analyst.dto;

import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
public class CompanyFactSheetData {
    private final ObjectMapper objectMapper;
    private final CompanyDto company;
    private final Map<StatementVariant, List<IncomeStatementDto>> incomeStatements;
    private final Map<StatementVariant, List<BalanceSheetDto>> balanceSheets;
    private final Map<StatementVariant, List<CashFlowStatementDto>> cashFlowStatements;
    private final Map<StatementVariant, List<CompanyFinancialRatiosDto>> companyFinancialRatios;
    private final List<SharePriceSignalDto> dailySharePriceSignals;
    private final List<SharePriceSignalDto> monthlySharePriceSignals;
    private final List<NewsTickerDto> recentNews;
    private final TickerNewsSentimentDto newsSentimentAggregate;

    public String getCompanyProfile() {
        return JsonUtils.toJson(objectMapper, company, "{}");
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        List<IncomeStatementDto> stmts = incomeStatements.get(variant);
        if (stmts == null || stmts.isEmpty()) {
            return "[]";
        }
        return "[" + stmts.stream().map(IncomeStatementDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        List<BalanceSheetDto> stmts = balanceSheets.get(variant);
        if (stmts == null || stmts.isEmpty()) {
            return "[]";
        }
        return "[" + stmts.stream().map(BalanceSheetDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getCashFlowHistory(StatementVariant variant) {
        List<CashFlowStatementDto> stmts = cashFlowStatements.get(variant);
        if (stmts == null || stmts.isEmpty())
            return "[]";
        return "[" + stmts.stream().map(CashFlowStatementDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getCompanyFinancialRatios(StatementVariant variant) {
        List<CompanyFinancialRatiosDto> dtos = companyFinancialRatios.get(variant);
        if (dtos == null || dtos.isEmpty()) {
            return "[]";
        }
        List<CompanyFinancialRatiosSlim> slimDtos = dtos.stream()
                .map(CompanyFinancialRatiosSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(objectMapper, slimDtos, "[]");
    }

    public String getDailySharePriceSignals() {
        if (dailySharePriceSignals == null) return "[]";
        List<SharePriceSignalSlim> slim = dailySharePriceSignals.stream()
                .map(SharePriceSignalSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(objectMapper, slim, "[]");
    }

    public String getDailySharePriceSignalsForPlanner() {
        if (dailySharePriceSignals == null || dailySharePriceSignals.isEmpty()) {
            return "[]";
        }
        List<PlannerSharePriceInput> slim = dailySharePriceSignals.stream()
                .map(PlannerSharePriceInput::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(objectMapper, slim, "[]");
    }

    public String getMonthlySharePriceSignals() {
        if (monthlySharePriceSignals == null || monthlySharePriceSignals.isEmpty()) {
            return "[]";
        }
        List<SharePriceSignalSlim> slim = monthlySharePriceSignals.stream()
                .map(SharePriceSignalSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(objectMapper, slim, "[]");
    }

    public String getRecentNews() {
        return JsonUtils.toJson(objectMapper, recentNews, "[]");
    }

    public List<NewsTickerDto> getRecentNewsList() {
        return recentNews;
    }

    public List<SharePriceSignalDto> getDailySharePriceSignalsList() {
        return dailySharePriceSignals;
    }

    public String getNewsSentimentAggregate() {
        return JsonUtils.toJson(objectMapper, newsSentimentAggregate, "{}");
    }

    public String getAlgorithmicBaselineJson() {
        if (companyFinancialRatios == null || companyFinancialRatios.isEmpty()) {
            return "{}";
        }
        Map<String, AlgorithmicBaselineDto.TimeframeScores> timeframeMap = new HashMap<>();
        for (var entry : companyFinancialRatios.entrySet()) {
            entry.getValue().stream().max(CompanyFinancialRatiosDto.getComparator())
                    .ifPresent(companyFinancialRatios ->
                            timeframeMap.put(entry.getKey().name(), new AlgorithmicBaselineDto.TimeframeScores(
                                    companyFinancialRatios.qualityScore(),
                                    companyFinancialRatios.piotroskiFScore()
                            )));
        }

        AlgorithmicBaselineDto baseline = new AlgorithmicBaselineDto(timeframeMap);
        return JsonUtils.toJson(objectMapper, baseline, "{}");
    }
}
