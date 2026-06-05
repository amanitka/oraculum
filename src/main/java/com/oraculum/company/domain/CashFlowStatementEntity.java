package com.oraculum.company.domain;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_cash_flow_statement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowStatementEntity {

    @Id
    private String id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(nullable = false, length = 10)
    private String market;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementTemplate template;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementVariant variant;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "statement_data", columnDefinition = "jsonb", nullable = false)
    private String statementData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}