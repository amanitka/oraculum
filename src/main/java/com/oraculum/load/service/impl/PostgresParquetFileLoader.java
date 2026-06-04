package com.oraculum.load.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.load.dto.LoadParquetDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresParquetFileLoader {

    private final OraculumProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public static String normalizeAndValidate(String parquetFilePath) {
        if (parquetFilePath == null || parquetFilePath.isBlank()) {
            throw new IllegalArgumentException("Parquet file path must not be null or blank");
        }
        String normalizedPath;
        try {
            normalizedPath = Path.of(parquetFilePath).normalize().toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid parquet file path: " + parquetFilePath, e);
        }
        if (normalizedPath.contains("'") || normalizedPath.contains(";") || normalizedPath.contains("--")) {
            throw new IllegalArgumentException("Parquet file path contains potentially unsafe characters: " + parquetFilePath);
        }
        return normalizedPath;
    }

    public static String getStagingTableName(String targetTableName) {
        return "staging_%s_%s".formatted(targetTableName, UUID.randomUUID().toString().replace("-", ""));
    }

    private String getSecret() {
        var database = properties.database();
        String escapedPassword = database.password().replace("'", "''");
        return """
                CREATE OR REPLACE SECRET pg_secret (
                    TYPE POSTGRES,
                    HOST '%s',
                    PORT %d,
                    DATABASE '%s',
                    USER '%s',
                    PASSWORD '%s'
                );
                """.formatted(database.host(), database.port(), database.name(), database.username(), escapedPassword);
    }

    private String getStagingTableDdl(LoadParquetDto loadParquetDto) {
        if (loadParquetDto.hasStatementData()) {
            return """
                    CREATE TABLE pg.%s AS
                    SELECT * EXCLUDE (statement_data), CAST(statement_data AS JSON) AS statement_data, now() AS created_at, now() AS updated_at
                    FROM read_parquet('%s')
                    LIMIT 0;
                    """.formatted(loadParquetDto.stagingTableName(), loadParquetDto.parquetFilePath());
        }
        return """
                CREATE TABLE pg.%s AS
                SELECT *, now() AS created_at, now() AS updated_at
                FROM read_parquet('%s')
                LIMIT 0;
                """.formatted(loadParquetDto.stagingTableName(), loadParquetDto.parquetFilePath());
    }

    private String getCopyToStagingSql(LoadParquetDto loadParquetDto) {
        if (loadParquetDto.hasStatementData()) {
            return """
                    INSERT INTO pg.%s
                    SELECT
                        * EXCLUDE (statement_data),
                        CAST(statement_data AS JSON) AS statement_data,
                        now(),
                        now()
                    FROM read_parquet('%s');
                    """.formatted(loadParquetDto.stagingTableName(), loadParquetDto.parquetFilePath());
        }
        return """
                INSERT INTO pg.%s
                SELECT
                    *,
                    now(),
                    now()
                FROM read_parquet('%s');
                """.formatted(loadParquetDto.stagingTableName(), loadParquetDto.parquetFilePath());
    }

    private void loadParquetIntoStaging(LoadParquetDto loadParquetDto) throws SQLException {
        log.info("Starting high-speed load from '{}' into new staging table '{}'",
                loadParquetDto.parquetFilePath(),
                loadParquetDto.stagingTableName());
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:"); Statement stmt = conn.createStatement()) {
            log.info("Step 1/3: Initializing PostgreSQL extension and connection for DuckDB.");
            stmt.execute("INSTALL postgres; LOAD postgres;");
            stmt.execute(getSecret());
            stmt.execute("ATTACH '' AS pg (TYPE POSTGRES, SECRET pg_secret);");
            log.info("Step 2/3: Creating staging table '{}' in PostgreSQL.", loadParquetDto.stagingTableName());
            stmt.execute(getStagingTableDdl(loadParquetDto));
            log.info("Step 3/3: Appending data to staging table '{}' using high-speed COPY.",
                    loadParquetDto.stagingTableName());
            int rows = stmt.executeUpdate(getCopyToStagingSql(loadParquetDto));
            log.info("Successfully loaded {} rows into staging table '{}'.", rows, loadParquetDto.stagingTableName());
        }
    }

    private void loadFromStagingTable(LoadParquetDto loadParquetDto) {
        log.info("Executing native load from staging table '{}' to '{}'",
                loadParquetDto.stagingTableName(),
                loadParquetDto.targetTableName());
        int rowsAffected = jdbcTemplate.update(loadParquetDto.loadSql());
        log.info("Native load completed successfully. {} rows affected or updated.", rowsAffected);
    }

    private void dropStagingTable(LoadParquetDto loadParquetDto) {
        var stagingTableName = loadParquetDto.stagingTableName();
        if (stagingTableName != null) {
            try {
                log.info("Dropping staging table '{}'.", stagingTableName);
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + stagingTableName + ";");
                log.info("Successfully dropped staging table '{}'.", stagingTableName);
            } catch (Exception e) {
                log.error("Failed to drop staging table '{}'. Manual cleanup required.", stagingTableName, e);
            }
        }
    }

    public void loadParquetIntoTargetTable(LoadParquetDto loadParquetDto) {
        try {
            loadParquetIntoStaging(loadParquetDto);
            loadFromStagingTable(loadParquetDto);
        } catch (SQLException e) {
            log.error("Failed during DuckDB staging process for file: {}", loadParquetDto, e);
            throw new RuntimeException("Merge process failed during staging", e);
        } catch (Exception e) {
            log.error("Failed during native Postgres UPSERT process for file: {}", loadParquetDto, e);
            throw new RuntimeException("Merge process failed during upsert", e);
        } finally {
            dropStagingTable(loadParquetDto);
        }
    }
}