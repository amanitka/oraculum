package com.oraculum.loader.dto;

import lombok.Builder;

@Builder
public record LoadParquetDto(String targetTableName, String stagingTableName, String parquetFilePath, String loadSql) {
}
