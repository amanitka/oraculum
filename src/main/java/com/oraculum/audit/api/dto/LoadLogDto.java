package com.oraculum.audit.api.dto;

import com.oraculum.audit.domain.LoadLogEntity;
import com.oraculum.audit.domain.LoadLogStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@NoArgsConstructor
@Getter
@Setter
public class LoadLogDto {
    private Long id;
    private String dataset;
    private String runId;
    private String fileChecksum;
    private LoadLogStatus status;
    private int loadedRows;
    private int mergedRows;
    private String errorText;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static LoadLogDto fromEntity(LoadLogEntity entity) {
        if (entity == null) return null;
        LoadLogDto dto = new LoadLogDto();
        dto.setId(entity.getId());
        dto.setDataset(entity.getDataset());
        dto.setRunId(entity.getRunId());
        dto.setFileChecksum(entity.getFileChecksum());
        dto.setStatus(entity.getStatus());
        dto.setLoadedRows(entity.getLoadedRows());
        dto.setMergedRows(entity.getMergedRows());
        dto.setErrorText(entity.getErrorText());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
