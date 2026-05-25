package com.oraculum.audit.api.dto;

import com.oraculum.audit.domain.IngestionRunLogEntity;
import com.oraculum.audit.domain.IngestionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@NoArgsConstructor
@Getter
@Setter
public class IngestionRunLogDto {
    private Long id;
    private String dataset;
    private String runId;
    private String fileChecksum;
    private IngestionStatus status;
    private int loadedRows;
    private int mergedRows;
    private int durationMs;
    private String errorText;
    private OffsetDateTime createdAt;

    public static IngestionRunLogDto fromEntity(IngestionRunLogEntity entity) {
        if (entity == null) return null;
        IngestionRunLogDto dto = new IngestionRunLogDto();
        dto.setId(entity.getId());
        dto.setDataset(entity.getDataset());
        dto.setRunId(entity.getRunId());
        dto.setFileChecksum(entity.getFileChecksum());
        dto.setStatus(entity.getStatus());
        dto.setLoadedRows(entity.getLoadedRows());
        dto.setMergedRows(entity.getMergedRows());
        dto.setDurationMs(entity.getDurationMs());
        dto.setErrorText(entity.getErrorText());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
