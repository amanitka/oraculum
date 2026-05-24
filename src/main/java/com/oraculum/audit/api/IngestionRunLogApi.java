package com.oraculum.audit.api;

import com.oraculum.audit.api.dto.IngestionRunLogDto;
import com.oraculum.audit.domain.IngestionRunLogEntity;

import java.util.List;

public interface IngestionRunLogApi {

    List<IngestionRunLogDto> getAllRunLogs();

    List<IngestionRunLogDto> getRunLogsByDataset(String dataset);

    IngestionRunLogDto getRunLogById(Integer id);

    boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum);

    IngestionRunLogEntity startRunLog(String dataset, String runId, String fileChecksum);

    void completeRunLog(IngestionRunLogEntity runLog, int loadedRows, int mergedRows, int durationMs);

    void failRunLog(IngestionRunLogEntity runLog, String errorText, int durationMs);
}
