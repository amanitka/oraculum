package com.oraculum.load.dto;

import lombok.Builder;

@Builder
public record LoadParquetDto(String targetTableName, String stagingTableName, String parquetFilePath, String loadSql, boolean hasPayload) {
}