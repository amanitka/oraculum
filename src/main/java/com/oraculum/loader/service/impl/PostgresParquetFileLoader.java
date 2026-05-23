package com.oraculum.loader.service.impl;

import com.oraculum.common.config.OraculumProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private void createSecret(Statement stmt) throws SQLException {
        var database = properties.getDatabase();
        String escapedPassword = database.getPassword().replace("'", "''");
        var createSecret = """
                CREATE OR REPLACE SECRET pg_secret (
                    TYPE POSTGRES,
                    HOST '%s',
                    PORT %d,
                    DATABASE '%s',
                    USER '%s',
                    PASSWORD '%s'
                );
                """.formatted(database.getHost(), database.getPort(), database.getName(), database.getUsername(),
                escapedPassword);
        stmt.execute(createSecret);
    }

    /**
     * Uses DuckDB to perform a high-speed load of a Parquet file into a new,
     * temporary staging table in the primary PostgreSQL database.
     *
     * @param parquetFilePath The path to the Parquet file.
     * @param baseTableName   The base name for the target table (e.g., "t_share_price").
     * @return The name of the created staging table (unqualified).
     * @throws SQLException if a database access error occurs during the DuckDB process.
     */
    public String loadParquetIntoStaging(String parquetFilePath, String baseTableName) throws SQLException {
        String stagingTableName = "staging_" + baseTableName + "_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Starting high-speed load from '{}' into new staging table '{}'", parquetFilePath, stagingTableName);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:"); Statement stmt = conn.createStatement()) {
            log.debug("Initializing PostgreSQL extension for DuckDB.");
            stmt.execute("INSTALL postgres; LOAD postgres;");
            log.debug("Creating database secret for Postgres connection.");
            createSecret(stmt);
            log.debug("Attaching PostgreSQL database to DuckDB execution path.");
            stmt.execute("ATTACH '' AS pg (TYPE POSTGRES, SECRET pg_secret);");

            // 1. Create the staging table structure in Postgres
            log.info("Step 1/2: Creating staging table '{}' in PostgreSQL.", stagingTableName);
            String createStagingTableSql = """
                    CREATE TABLE pg.%s AS
                    SELECT *, now() AS created_at, now() AS updated_at
                    FROM read_parquet('%s')
                    LIMIT 0;
                    """.formatted(stagingTableName, parquetFilePath);
            stmt.execute(createStagingTableSql);

            // 2. Fast-append data from Parquet into the staging table using COPY
            log.info("Step 2/2: Appending data to staging table '{}' using high-speed COPY.", stagingTableName);
            String copyToStagingSql = """
                    INSERT INTO pg.%s
                    SELECT *, now(), now()
                    FROM read_parquet('%s');
                    """.formatted(stagingTableName, parquetFilePath);
            int rows = stmt.executeUpdate(copyToStagingSql);
            log.info("Successfully loaded {} rows into staging table '{}'.", rows, stagingTableName);
        }
        return stagingTableName;
    }
}
