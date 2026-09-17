package com.oraculum.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_sec_holding")
@IdClass(SecHoldingEntity.SecHoldingId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecHoldingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "report_period", nullable = false)
    private LocalDate reportPeriod;

    @Column(nullable = false, length = 10)
    private String cik;

    @Column(name = "accession_number", nullable = false, length = 25)
    private String accessionNumber;

    @Column(name = "filing_date", nullable = false)
    private LocalDate filingDate;

    @Column(name = "manager_name", nullable = false)
    private String managerName;

    @Column(name = "issuer_name", nullable = false)
    private String issuerName;

    @Column(name = "class_title", length = 100)
    private String classTitle;

    @Column(nullable = false, length = 9)
    private String cusip;

    @Column(name = "value_usd", nullable = false)
    private Long valueUsd;

    @Column(name = "shares_or_prn_amount", nullable = false)
    private Long sharesOrPrnAmount;

    @Column(name = "shares_or_prn_type", length = 4)
    private String sharesOrPrnType;

    @Column(name = "option_type", length = 4)
    private String optionType;

    @Column(name = "investment_discretion", length = 4)
    private String investmentDiscretion;

    @Column(name = "voting_auth_sole", nullable = false)
    private Long votingAuthSole;

    @Column(name = "voting_auth_shared", nullable = false)
    private Long votingAuthShared;

    @Column(name = "voting_auth_none", nullable = false)
    private Long votingAuthNone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SecHoldingId implements Serializable {
        private Long id;
        private LocalDate reportPeriod;
    }
}
