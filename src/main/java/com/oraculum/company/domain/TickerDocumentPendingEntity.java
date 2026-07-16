package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.domain.TickerDocumentProcessingStatus;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "v_ticker_document_pending")
@IdClass(TickerDocumentPendingEntity.TickerDocumentPendingId.class)
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerDocumentPendingEntity {

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

    private String content;

    @Enumerated(EnumType.STRING)
    private TickerDocumentProcessingStatus status;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "market_capitalization")
    private Double marketCapitalization;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "document_priority")
    private Integer documentPriority;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerDocumentPendingId implements Serializable {
        private String id;
        private LocalDate reportPeriod;
    }
}
