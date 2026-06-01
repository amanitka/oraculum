package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FactSheetAgentOutput;
import com.oraculum.analyst.agents.models.FinancialFactSheetData;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FactSheetAgent implements Agent<FactSheetAgentOutput> {

    private final AnalystProperties analystProperties;

    @Override
    public AgentType getName() {
        return AgentType.FACT_SHEET;
    }

    @Override
    public Class<FactSheetAgentOutput> getOutputModel() {
        return FactSheetAgentOutput.class;
    }

    @Override
    public AgentOutput<FactSheetAgentOutput> run(AgentContext ctx) {
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
        String recentNews = ctx.getTools().getRecentNews(ctx.getTicker(), 30, historyLimit);

        FinancialFactSheetData factSheet = new FinancialFactSheetData(
                tickerProfile,
                incomeStatementHistory,
                balanceSheetHistory,
                cashFlowHistory,
                derivedMetrics,
                sharePriceSignals,
                recentNews
        );

        FactSheetAgentOutput output = new FactSheetAgentOutput(factSheet);
        return new AgentOutput<>(output, 0);
    }
}