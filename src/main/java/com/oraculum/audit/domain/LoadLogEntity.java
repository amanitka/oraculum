package com.oraculum.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "t_load_log", uniqueConstraints = {@UniqueConstraint(name = "uq_load_log_idempotency", columnNames = {
        "dataset", "run_id", "file_checksum"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dataset;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "file_checksum", nullable = false)
    private String fileChecksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoadLogStatus status;

    @Column(name = "loaded_rows", nullable = false)
    private int loadedRows;

    @Column(name = "merged_rows", nullable = false)
    private int mergedRows;

    @Column(name = "error_text", columnDefinition = "TEXT")
    private String errorText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
