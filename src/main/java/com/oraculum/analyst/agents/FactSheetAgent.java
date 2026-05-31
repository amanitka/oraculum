package com.oraculum.analyst.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FinancialFactSheet;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FactSheetAgent implements Agent<FactSheetAgent.FactSheetOutput> {

    private final AnalystProperties analystProperties;

    public FactSheetAgent(AnalystProperties analystProperties) {
        this.analystProperties = analystProperties;
    }

    @Override
    public String getName() {
        return "FactSheet";
    }

    @Override
    public Class<FactSheetOutput> getOutputModel() {
        return FactSheetOutput.class;
    }

    @Override
    public AgentOutput<FactSheetOutput> run(AgentContext ctx) {
        CompanyDto companyProfileDto = ctx.getTools().getCompany(ctx.getTicker(), ctx.getMarket());
        Map<String, String> tickerProfile = new HashMap<>();

        if (companyProfileDto != null) {
            tickerProfile.put("ticker", companyProfileDto.ticker() != null ? companyProfileDto.ticker() : "");
            tickerProfile.put("name",
                    companyProfileDto.companyName() != null ? companyProfileDto.companyName() : "Unknown");
            tickerProfile.put("industry",
                    companyProfileDto.industryName() != null ? companyProfileDto.industryName() : "Unknown");
            tickerProfile.put("sector",
                    companyProfileDto.sectorName() != null ? companyProfileDto.sectorName() : "Unknown");
            tickerProfile.put("industry_id",
                    companyProfileDto.industryId() != null ? companyProfileDto.industryId() : "");
        }

        StatementVariant variant = ctx.getDefaultVariant();
        int historyLimit = analystProperties.factSheet().historyLimit();

        String incomeStatementHistory = ctx.getTools()
                .getIncomeStatementHistory(ctx.getCompanyId(), variant, historyLimit);
        String balanceSheetHistory = ctx.getTools().getBalanceSheetHistory(ctx.getCompanyId(), variant, historyLimit);
        String cashFlowHistory = ctx.getTools().getCashFlowHistory(ctx.getCompanyId(), variant, historyLimit);
        String derivedMetrics = ctx.getTools().getDerivedMetrics(ctx.getCompanyId(), variant, historyLimit);
        String sharePriceSignals = ctx.getTools().getSharePriceSignals(ctx.getCompanyId(), ctx.getAsOf());
        String recentNews = ctx.getTools().getRecentNews(ctx.getTicker(), ctx.getMarket(), 30, historyLimit);

        FinancialFactSheet factSheet = FinancialFactSheet.builder()
                .tickerProfile(tickerProfile)
                .incomeStatementHistory(incomeStatementHistory)
                .balanceSheetHistory(balanceSheetHistory)
                .cashFlowHistory(cashFlowHistory)
                .derivedMetrics(derivedMetrics)
                .sharePriceSignals(sharePriceSignals)
                .recentNews(recentNews)
                .build();

        FactSheetOutput output = FactSheetOutput.builder().factSheet(factSheet).build();
        return new AgentOutput<>(output, 0);
    }

    @Builder
    public static class FactSheetOutput {
        @JsonProperty("fact_sheet")
        FinancialFactSheet factSheet;
    }
}