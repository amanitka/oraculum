package com.oraculum.audit.api;

import com.oraculum.audit.api.dto.IngestionRunLogDto;

import java.util.List;

public interface IngestionRunLogApi {

    List<IngestionRunLogDto> getRunLogsByDataset(String dataset);

    IngestionRunLogDto getRunLogById(Long id);

    boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum);

    IngestionRunLogDto startRunLog(String dataset, String runId, String fileChecksum);

    IngestionRunLogDto completeRunLog(IngestionRunLogDto runLog);

    IngestionRunLogDto failRunLog(IngestionRunLogDto runLog);
}
