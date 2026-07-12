package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "t_ticker_document")
@IdClass(TickerDocumentEntity.TickerDocumentId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerDocumentEntity {

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

    private String summary;

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerDocumentId implements Serializable {
        private String id;
        private LocalDate reportPeriod;
    }
}
