package com.oraculum.company.api.dto;

import com.oraculum.company.domain.CompanyEntity;
import org.junit.jupiter.api.Test;

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
}
