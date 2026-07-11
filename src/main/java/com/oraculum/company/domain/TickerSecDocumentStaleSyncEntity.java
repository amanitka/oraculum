package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "v_ticker_sec_document_stale_sync")
@IdClass(TickerSecDocumentStaleSyncEntity.TickerSecDocumentStaleSyncId.class)
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerSecDocumentStaleSyncEntity {

    @Id
    private String ticker;

    @Id
    private String market;

    @Id
    @Column(name = "document_type")
    private String documentType;

    @Column(name = "last_processed_file_date")
    private LocalDate lastProcessedFileDate;

    @Column(name = "last_file_refresh_at")
    private OffsetDateTime lastFileRefreshAt;

    @Column(name = "last_refresh_at")
    private OffsetDateTime lastRefreshAt;

    @Transient
    public TickerDocumentType getDocumentType() {
        return TickerDocumentType.fromString(this.documentType).orElse(null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerSecDocumentStaleSyncId implements Serializable {
        private String ticker;
        private String market;
        private String documentType;
    }
}
