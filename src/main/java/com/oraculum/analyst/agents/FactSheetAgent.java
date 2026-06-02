package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.CompanyFactSheetData;
import com.oraculum.analyst.agents.models.FactSheetAgentOutput;
import com.oraculum.analyst.agents.tools.DataTools;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FactSheetAgent implements Agent<FactSheetAgentOutput> {

    private final DataTools dataTools;
    private final AnalystProperties analystProperties;

    private static @NonNull Map<String, String> getCompanyProfile(CompanyDto company) {
        Map<String, String> tickerProfile = new HashMap<>();
        tickerProfile.put("ticker", company.ticker() != null ? company.ticker() : "");
        tickerProfile.put("market", company.market() != null ? company.market() : "");
        tickerProfile.put("company_name", company.companyName() != null ? company.companyName() : "Unknown");
        tickerProfile.put("industry_name", company.industryName() != null ? company.industryName() : "Unknown");
        tickerProfile.put("sector_name", company.sectorName() != null ? company.sectorName() : "Unknown");
        tickerProfile.put("isin", company.isin() != null ? company.isin() : "");
        tickerProfile.put("description", company.description() != null ? company.description() : "");
        tickerProfile.put("employee_count",
                company.employeeCount() != null ? String.valueOf(company.employeeCount()) : "");
        tickerProfile.put("currency", company.currency() != null ? company.currency() : "");
        tickerProfile.put("cik", company.cik() != null ? company.cik() : "");
        return tickerProfile;
    }

    private CompanyFactSheetData createFinancialFactSheetData(AgentContext ctx) {
        int historyLimit = analystProperties.factSheet().historyLimit();
        Map<String, String> companyProfile = getCompanyProfile(ctx.company());
        String incomeStatementHistory = dataTools.getIncomeStatementHistory(ctx.companyId(),
                ctx.defaultVariant(),
                historyLimit);
        String balanceSheetHistory = dataTools.getBalanceSheetHistory(ctx.companyId(),
                ctx.defaultVariant(),
                historyLimit);
        String cashFlowHistory = dataTools.getCashFlowHistory(ctx.companyId(), ctx.defaultVariant(), historyLimit);
        String derivedMetrics = dataTools.getDerivedMetrics(ctx.companyId(), ctx.defaultVariant(), historyLimit);
        String sharePriceSignals = dataTools.getSharePriceSignals(ctx.companyId(), ctx.requestDate());
        String recentNews = dataTools.getRecentNews(ctx.ticker(), 30, historyLimit);

        return new CompanyFactSheetData(companyProfile,
                incomeStatementHistory,
                balanceSheetHistory,
                cashFlowHistory,
                derivedMetrics,
                sharePriceSignals,
                recentNews);
    }

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
        CompanyFactSheetData companyFactSheetData = createFinancialFactSheetData(ctx);
        FactSheetAgentOutput output = new FactSheetAgentOutput(companyFactSheetData);

        return new AgentOutput<>(output, 0);
    }
}