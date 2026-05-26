package com.oraculum.audit.api;

import com.oraculum.audit.api.dto.LoadLogDto;

import java.util.List;

public interface LoadLogApi {

    List<LoadLogDto> getRunLogsByDataset(String dataset);

    LoadLogDto getRunLogById(Long id);

    boolean isAlreadyProcessed(String dataset, String runId, String fileChecksum);

    LoadLogDto startRunLog(String dataset, String runId, String fileChecksum);

    LoadLogDto completeRunLog(LoadLogDto runLog);

    LoadLogDto failRunLog(LoadLogDto runLog);
}
