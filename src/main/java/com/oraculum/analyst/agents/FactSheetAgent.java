package com.oraculum.analyst.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FinancialFactSheet;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.TickerDto;
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
        TickerDto tickerProfileDto = ctx.getTools().getTicker(ctx.getTicker(), ctx.getMarket());
        Map<String, String> tickerProfile = new HashMap<>();

        if (tickerProfileDto != null) {
            tickerProfile.put("ticker", tickerProfileDto.ticker() != null ? tickerProfileDto.ticker() : "");
            tickerProfile.put("name",
                    tickerProfileDto.companyName() != null ? tickerProfileDto.companyName() : "Unknown");
            tickerProfile.put("industry",
                    tickerProfileDto.industryName() != null ? tickerProfileDto.industryName() : "Unknown");
            tickerProfile.put("sector",
                    tickerProfileDto.sectorName() != null ? tickerProfileDto.sectorName() : "Unknown");
            tickerProfile.put("industry_id",
                    tickerProfileDto.industryId() != null ? tickerProfileDto.industryId() : "");
        }

        StatementVariant variant = ctx.getDefaultVariant();
        int historyLimit = analystProperties.factSheet().historyLimit();

        String incomeStatementHistory = ctx.getTools()
                .getIncomeStatementHistory(ctx.getTicker(), variant, historyLimit);
        String balanceSheetHistory = ctx.getTools().getBalanceSheetHistory(ctx.getTicker(), variant, historyLimit);
        String cashFlowHistory = ctx.getTools().getCashFlowHistory(ctx.getTicker(), variant, historyLimit);
        String derivedMetrics = ctx.getTools().getDerivedMetrics(ctx.getTicker(), variant, historyLimit);
        String sharePriceSignals = ctx.getTools().getSharePriceSignals(ctx.getTicker(), ctx.getMarket(), ctx.getAsOf());
        String recentNews = ctx.getTools().getRecentNews(ctx.getTicker(), 30, historyLimit);

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