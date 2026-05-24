package com.oraculum.company.comain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_cash_flow_statement", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cash_flow_statement_composite_key", columnNames = {"composite_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowStatementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "composite_key", nullable = false, updatable = false)
    private String compositeKey;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "simfin_id", nullable = false)
    private int simfinId;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    private String variant;

    @Column(nullable = false)
    private String currency;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "fiscal_period", nullable = false)
    private String fiscalPeriod;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "publish_date", nullable = false)
    private LocalDate publishDate;

    @Column(name = "restated_date")
    private LocalDate restatedDate;

    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}