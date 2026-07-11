package com.oraculum.company.domain;

import com.oraculum.company.api.domain.SyncExtractionStatus;
import com.oraculum.company.api.domain.SyncStatus;
import com.oraculum.company.api.domain.TickerDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_ticker_document_sync_status")
@IdClass(TickerDocumentSyncStatusEntity.TickerDocumentSyncStatusId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerDocumentSyncStatusEntity {

    @Id
    private String ticker;

    @Id
    private String market;

    @Id
    private String source;

    @Id
    @Column(name = "document_type")
    private String documentType;

    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    @Column(name = "extraction_status")
    @Enumerated(EnumType.STRING)
    private SyncExtractionStatus extractionStatus;

    private String message;

    @Column(name = "last_processed_file_date")
    private LocalDate lastProcessedFileDate;

    @Column(name = "last_refresh_at")
    private OffsetDateTime lastRefreshAt;

    @Column(name = "last_file_refresh_at")
    private OffsetDateTime lastFileRefreshAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Transient
    public TickerDocumentType getDocumentType() {
        return TickerDocumentType.fromString(this.documentType).orElse(null);
    }

    public void setDocumentType(TickerDocumentType type) {
        this.documentType = (type != null) ? type.getCode() : null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerDocumentSyncStatusId implements Serializable {
        private String ticker;
        private String market;
        private String source;
        private String documentType;
    }

}
