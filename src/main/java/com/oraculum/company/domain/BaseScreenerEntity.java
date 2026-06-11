package com.oraculum.company.domain;

import com.oraculum.company.api.domain.CompanySize;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@MappedSuperclass
@IdClass(BaseScreenerEntity.ScreenerId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseScreenerEntity {

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Id
    @Column(name = "company_id")
    private int companyId;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "market")
    private String market;

    @Column(name = "currency")
    private String currency;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "description")
    private String description;

    @Column(name = "sector_name")
    private String sectorName;

    @Column(name = "industry_name")
    private String industryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size")
    private CompanySize companySize;

    @Column(name = "market_capitalization")
    private Float marketCapitalization;

    @Column(name = "share_price")
    private Float sharePrice;

    @Column(name = "volume_velocity")
    private Float volumeVelocity;

    @Column(name = "pe_ratio")
    private Float peRatio;

    @Column(name = "earnings_yield")
    private Float earningsYield;

    @Column(name = "piotroski_f_score")
    private Integer piotroskiFScore;

    @Column(name = "quality_score")
    private Float qualityScore;

    @Column(name = "composite_signal")
    private String compositeSignal;

    public static class ScreenerId implements Serializable {
        private LocalDate tradeDate;
        private int companyId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScreenerId that = (ScreenerId) o;
            return companyId == that.companyId && Objects.equals(tradeDate, that.tradeDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tradeDate, companyId);
        }
    }
}
