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

    private static @NonNull Map<String, String> getStringStringMap(CompanyDto companyProfileDto) {
        Map<String, String> tickerProfile = new HashMap<>();
        tickerProfile.put("ticker", companyProfileDto.ticker() != null ? companyProfileDto.ticker() : "");
        tickerProfile.put("market", companyProfileDto.market() != null ? companyProfileDto.market() : "");
        tickerProfile.put("company_name",
                companyProfileDto.companyName() != null ? companyProfileDto.companyName() : "Unknown");
        tickerProfile.put("industry_name",
                companyProfileDto.industryName() != null ? companyProfileDto.industryName() : "Unknown");
        tickerProfile.put("sector_name",
                companyProfileDto.sectorName() != null ? companyProfileDto.sectorName() : "Unknown");
        tickerProfile.put("isin", companyProfileDto.isin() != null ? companyProfileDto.isin() : "");
        tickerProfile.put("description",
                companyProfileDto.description() != null ? companyProfileDto.description() : "");
        tickerProfile.put("employee_count",
                companyProfileDto.employeeCount() != null ? String.valueOf(companyProfileDto.employeeCount()) : "");
        tickerProfile.put("currency", companyProfileDto.currency() != null ? companyProfileDto.currency() : "");
        tickerProfile.put("cik", companyProfileDto.cik() != null ? companyProfileDto.cik() : "");
        return tickerProfile;
    }

    private CompanyFactSheetData createFinancialFactSheetData(AgentContext ctx) {
        int historyLimit = analystProperties.factSheet().historyLimit();
        Map<String, String> tickerProfile = getStringStringMap(ctx.company());
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

        return new CompanyFactSheetData(tickerProfile,
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