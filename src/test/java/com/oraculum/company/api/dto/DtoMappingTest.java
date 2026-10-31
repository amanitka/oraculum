package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.domain.CompanyInstitutionalFlowEntity;
import com.oraculum.company.domain.SectorInstitutionalFlowEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void companyDto_fromEntity_mapsCorrectly() {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(1);
        entity.setTicker("AAPL");
        entity.setMarket("US");
        entity.setCompanyName("Apple Inc.");
        entity.setIndustryId("IND1");
        entity.setIndustryName("Tech");
        entity.setSectorName("IT");
        entity.setIsin("US0378331005");
        entity.setDescription("Maker of iPhones.");
        entity.setEmployeeCount(150000L);
        entity.setCurrency("USD");
        entity.setCik("0000320193");
        OffsetDateTime now = OffsetDateTime.now();
        entity.setExtractedAt(now);

        CompanyDto dto = CompanyDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.ticker()).isEqualTo("AAPL");
        assertThat(dto.market()).isEqualTo("US");
        assertThat(dto.companyName()).isEqualTo("Apple Inc.");
        assertThat(dto.industryId()).isEqualTo("IND1");
        assertThat(dto.industryName()).isEqualTo("Tech");
        assertThat(dto.sectorName()).isEqualTo("IT");
        assertThat(dto.isin()).isEqualTo("US0378331005");
        assertThat(dto.description()).isEqualTo("Maker of iPhones.");
        assertThat(dto.employeeCount()).isEqualTo(150000L);
        assertThat(dto.currency()).isEqualTo("USD");
        assertThat(dto.cik()).isEqualTo("0000320193");
        assertThat(dto.extractedAt()).isEqualTo(now);
    }

    @Test
    void companyDto_fromEntity_nullHandling() {
        assertThat(CompanyDto.fromEntity(null)).isNull();
    }

    @Test
    void companyInstitutionalFlowDto_fromEntity_mapsAllFieldsCorrectly() {
        CompanyInstitutionalFlowEntity entity = new CompanyInstitutionalFlowEntity();
        entity.setCompanyId(1);
        entity.setTicker("NVDA");
        entity.setCompanyName("NVIDIA Corp");
        entity.setMarket("US");
        entity.setSectorName("Technology");
        entity.setIndustryName("Semiconductors");
        entity.setCusip("67066G104");
        entity.setReportPeriod(LocalDate.of(2026, 6, 30));
        entity.setQuarterIndex(1L);
        entity.setTotalInstValueUsd(1_500_000_000_000L);
        entity.setTotalInstShares(12_000_000_000L);
        entity.setTotalHoldersCount(3500);
        entity.setTopHoldersNames(new String[]{"Vanguard", "BlackRock"});
        entity.setTop5HoldersValueUsd(600_000_000_000L);
        entity.setTop5HoldersPct(40.0f);
        entity.setPutValueUsd(50_000_000_000L);
        entity.setCallValueUsd(80_000_000_000L);
        entity.setPutCallRatio(0.62f);
        entity.setNetValueChangeUsd(120_000_000_000L);
        entity.setNetSharesChange(400_000_000L);
        entity.setSharesChangePct(3.45f);
        entity.setCapitalFlowUsd(50_000_000_000L);
        entity.setPriceValuationEffectUsd(70_000_000_000L);
        entity.setFlagIncompleteFilerSample(true);
        entity.setFundsBuyingCount(1200);
        entity.setFundsSellingCount(800);
        entity.setFundsNewCount(150);
        entity.setFundsClosedCount(50);
        entity.setFundsUnchangedCount(1300);
        entity.setTier1ValueUsd(200_000_000_000L);
        entity.setTier1Shares(1_600_000_000L);
        entity.setTier1HoldersCount(15);
        entity.setTier1NetValueChangeUsd(20_000_000_000L);
        entity.setTier1NetSharesChange(80_000_000L);
        entity.setTier1BuyersCount(8);
        entity.setTier1SellersCount(3);
        entity.setTier1NewCount(2);
        entity.setTier1ClosedCount(1);
        entity.setTier1BuyerNames(new String[]{"Berkshire", "Duquesne"});
        entity.setTier1SellerNames(new String[]{"Appaloosa"});

        CompanyInstitutionalFlowDto dto = CompanyInstitutionalFlowDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.ticker()).isEqualTo("NVDA");
        assertThat(dto.capitalFlowUsd()).isEqualTo(50_000_000_000L);
        assertThat(dto.priceValuationEffectUsd()).isEqualTo(70_000_000_000L);
        assertThat(dto.flagIncompleteFilerSample()).isTrue();
        assertThat(dto.topHoldersNames()).containsExactly("Vanguard", "BlackRock");
        assertThat(dto.tier1BuyerNames()).containsExactly("Berkshire", "Duquesne");
        assertThat(dto.tier1SellerNames()).containsExactly("Appaloosa");
        assertThat(CompanyInstitutionalFlowDto.fromEntity(null)).isNull();
    }

    @Test
    void sectorInstitutionalFlowTrendDto_fromEntity_andFlowPct() {
        LocalDate period = LocalDate.of(2026, 6, 30);
        SectorInstitutionalFlowEntity entity = new SectorInstitutionalFlowEntity(
                "Technology", period, 1_000_000_000_000L, 50_000_000_000L, 20_000_000_000L, 30_000_000_000L,
                10_000_000_000L, 200_000_000L, 120L
        );

        SectorInstitutionalFlowTrendDto dto = SectorInstitutionalFlowTrendDto.fromEntity(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.sectorName()).isEqualTo("Technology");
        assertThat(dto.capitalFlowUsd()).isEqualTo(20_000_000_000L);
        assertThat(dto.priceValuationEffectUsd()).isEqualTo(30_000_000_000L);
        // flowPct prioritizes capitalFlowUsd (20B / 1000B * 100 = 2.0%)
        assertThat(dto.flowPct()).isEqualTo(2.0f);

        // Fallback when capitalFlowUsd is null
        SectorInstitutionalFlowTrendDto fallbackDto = new SectorInstitutionalFlowTrendDto(
                "Energy", period, 500_000_000_000L, 25_000_000_000L, null, null,
                5_000_000_000L, 50_000_000L, 40L
        );
        assertThat(fallbackDto.flowPct()).isEqualTo(5.0f);

        // Zero / null handling
        SectorInstitutionalFlowTrendDto zeroDto = new SectorInstitutionalFlowTrendDto(
                "Utilities", period, 0L, 0L, null, null, 0L, 0L, 10L
        );
        assertThat(zeroDto.flowPct()).isEqualTo(0.0f);
        assertThat(SectorInstitutionalFlowTrendDto.fromEntity(null)).isNull();
    }
}
