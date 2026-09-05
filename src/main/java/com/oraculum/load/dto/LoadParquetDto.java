package com.oraculum.load.dto;

import lombok.Builder;

import java.util.function.Consumer;

@Builder
public record LoadParquetDto(String targetTableName,
                             String stagingTableName,
                             String parquetFilePath,
                             String loadSql,
                             boolean hasStatementData,
                             Consumer<String> preLoadAction) {

    public LoadParquetDto(String targetTableName,
                          String stagingTableName,
                          String parquetFilePath,
                          String loadSql,
                          boolean hasStatementData) {
        this(targetTableName, stagingTableName, parquetFilePath, loadSql, hasStatementData, null);
    }
}


