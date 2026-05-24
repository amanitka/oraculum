package com.oraculum.audit.api.dto;

import com.oraculum.audit.domain.IngestionRunLogEntity;
import com.oraculum.audit.domain.IngestionStatus;

import java.time.OffsetDateTime;

public record IngestionRunLogDto(
        Integer id,
        String dataset,
        String runId,
        String fileChecksum,
        IngestionStatus status,
        int loadedRows,
        int mergedRows,
        int durationMs,
        String errorText,
        OffsetDateTime createdAt
) {
    public static IngestionRunLogDto fromEntity(IngestionRunLogEntity entity) {
        if (entity == null) return null;
        return new IngestionRunLogDto(
                entity.getId(),
                entity.getDataset(),
                entity.getRunId(),
                entity.getFileChecksum(),
                entity.getStatus(),
                entity.getLoadedRows(),
                entity.getMergedRows(),
                entity.getDurationMs(),
                entity.getErrorText(),
                entity.getCreatedAt()
        );
    }
}
