package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "t_ticker_document_raw")
@IdClass(TickerDocumentRawEntity.TickerDocumentRawId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerDocumentRawEntity {

    @Id
    private String id;

    private String ticker;

    private String market;

    private String source;

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    private TickerDocumentType documentType;

    @Column(name = "document_subtype")
    @Enumerated(EnumType.STRING)
    private TickerDocumentSubtype documentSubtype;

    @Column(name = "accession_number")
    private String accessionNumber;

    @Column(name = "source_url")
    private String sourceUrl;

    @Id
    @Column(name = "report_period")
    private LocalDate reportPeriod;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    private String content;

    private String status;

    @Column(name = "extracted_at")
    private Instant extractedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerDocumentRawId implements Serializable {
        private String id;
        private LocalDate reportPeriod;
    }
}
