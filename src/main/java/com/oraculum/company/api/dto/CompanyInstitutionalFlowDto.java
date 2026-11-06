package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CompanyInstitutionalFlowEntity;

import java.time.LocalDate;
import java.util.List;

public record CompanyInstitutionalFlowDto(
        int companyId,
        String ticker,
        String companyName,
        String market,
        String sectorName,
        String industryName,
        String cusip,
        LocalDate reportPeriod,
        long quarterIndex,
        Long totalInstValueUsd,
        Long totalInstShares,
        Integer totalHoldersCount,
        List<String> topHoldersNames,
        Long top5HoldersValueUsd,
        Float top5HoldersPct,
        Long putValueUsd,
        Long callValueUsd,
        Float putCallRatio,
        Long netValueChangeUsd,
        Long netSharesChange,
        Float sharesChangePct,
        Long capitalFlowUsd,
        Long priceValuationEffectUsd,
        Boolean flagIncompleteFilerSample,
        Integer fundsBuyingCount,
        Integer fundsSellingCount,
        Integer fundsNewCount,
        Integer fundsClosedCount,
        Integer fundsUnchangedCount,
        Long tier1ValueUsd,
        Long tier1Shares,
        Integer tier1HoldersCount,
        Long tier1NetValueChangeUsd,
        Long tier1NetSharesChange,
        Integer tier1BuyersCount,
        Integer tier1SellersCount,
        Integer tier1NewCount,
        Integer tier1ClosedCount,
        List<String> tier1BuyerNames,
        List<String> tier1SellerNames
) {
    public static CompanyInstitutionalFlowDto fromEntity(CompanyInstitutionalFlowEntity entity) {
        if (entity == null) return null;
        return new CompanyInstitutionalFlowDto(
                entity.getCompanyId(),
                entity.getTicker(),
                entity.getCompanyName(),
                entity.getMarket(),
                entity.getSectorName(),
                entity.getIndustryName(),
                entity.getCusip(),
                entity.getReportPeriod(),
                entity.getQuarterIndex(),
                entity.getTotalInstValueUsd(),
                entity.getTotalInstShares(),
                entity.getTotalHoldersCount(),
                entity.getTopHoldersNames() != null ? List.of(entity.getTopHoldersNames()) : List.of(),
                entity.getTop5HoldersValueUsd(),
                entity.getTop5HoldersPct(),
                entity.getPutValueUsd(),
                entity.getCallValueUsd(),
                entity.getPutCallRatio(),
                entity.getNetValueChangeUsd(),
                entity.getNetSharesChange(),
                entity.getSharesChangePct(),
                entity.getCapitalFlowUsd(),
                entity.getPriceValuationEffectUsd(),
                entity.getFlagIncompleteFilerSample(),
                entity.getFundsBuyingCount(),
                entity.getFundsSellingCount(),
                entity.getFundsNewCount(),
                entity.getFundsClosedCount(),
                entity.getFundsUnchangedCount(),
                entity.getTier1ValueUsd(),
                entity.getTier1Shares(),
                entity.getTier1HoldersCount(),
                entity.getTier1NetValueChangeUsd(),
                entity.getTier1NetSharesChange(),
                entity.getTier1BuyersCount(),
                entity.getTier1SellersCount(),
                entity.getTier1NewCount(),
                entity.getTier1ClosedCount(),
                entity.getTier1BuyerNames() != null ? List.of(entity.getTier1BuyerNames()) : List.of(),
                entity.getTier1SellerNames() != null ? List.of(entity.getTier1SellerNames()) : List.of()
        );
    }

    public CompanyInstitutionalFlowDto withFlagIncompleteFilerSample(Boolean flag) {
        return new CompanyInstitutionalFlowDto(
                companyId, ticker, companyName, market, sectorName, industryName, cusip,
                reportPeriod, quarterIndex, totalInstValueUsd, totalInstShares, totalHoldersCount,
                topHoldersNames, top5HoldersValueUsd, top5HoldersPct, putValueUsd, callValueUsd,
                putCallRatio, netValueChangeUsd, netSharesChange, sharesChangePct, capitalFlowUsd,
                priceValuationEffectUsd, flag, fundsBuyingCount, fundsSellingCount, fundsNewCount,
                fundsClosedCount, fundsUnchangedCount, tier1ValueUsd, tier1Shares, tier1HoldersCount,
                tier1NetValueChangeUsd, tier1NetSharesChange, tier1BuyersCount, tier1SellersCount,
                tier1NewCount, tier1ClosedCount, tier1BuyerNames, tier1SellerNames
        );
    }
}
