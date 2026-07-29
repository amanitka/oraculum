package com.oraculum.company.domain;

import com.oraculum.company.api.domain.TickerDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.YesNoConverter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "v_ticker_sec_document_sync")
@IdClass(TickerSecDocumentSyncEntity.TickerSecDocumentSyncId.class)
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerSecDocumentSyncEntity {

    @Column(name = "is_stale")
    @Convert(converter = YesNoConverter.class)
    private Boolean isStale;

    @Id
    private String ticker;

    @Id
    private String market;

    @Id
    @Column(name = "document_type")
    private String documentType;

    private String cik;

    @Column(name = "last_processed_file_date")
    private LocalDate lastProcessedFileDate;

    @Column(name = "last_file_refresh_at")
    private OffsetDateTime lastFileRefreshAt;

    @Column(name = "last_refresh_at")
    private OffsetDateTime lastRefreshAt;

    @Transient
    public TickerDocumentType getDocumentTypeEnum() {
        return TickerDocumentType.fromString(getDocumentType()).orElse(null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerSecDocumentSyncId implements Serializable {
        private String ticker;
        private String market;
        private String documentType;
    }
}
