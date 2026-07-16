package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "v_ticker_document")
@IdClass(TickerDocumentViewEntity.TickerDocumentViewId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerDocumentViewEntity {

    @Id
    private String id;

    private String ticker;

    private String market;

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    private TickerDocumentType documentType;

    @Column(name = "document_subtype")
    @Enumerated(EnumType.STRING)
    private TickerDocumentSubtype documentSubtype;

    @Id
    @Column(name = "report_period")
    private LocalDate reportPeriod;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "accession_number")
    private String accessionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String summary;

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerDocumentViewId implements Serializable {
        private String id;
        private LocalDate reportPeriod;
    }
}
