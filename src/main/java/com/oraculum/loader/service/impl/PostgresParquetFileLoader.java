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

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresParquetFileLoader {

    private final OraculumProperties properties;

    private void createSecret(Statement stmt) throws SQLException {
        var database = properties.getDatabase();
        // SQL Injection Prevention: Escape single quotes in the password.
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
     * Executes a bulk SQL command using a DuckDB in-memory instance that is configured
     * to communicate with the project's primary PostgreSQL database.
     * <p>
     * This method handles the boilerplate of:
     * 1. Creating an in-memory DuckDB instance.
     * 2. Initializing the DuckDB-Postgres integration.
     * 3. Creating a temporary secret for the DB connection.
     * 4. Attaching the PostgreSQL database to DuckDB's execution path.
     * 5. Executing the provided SQL.
     * 6. Handling exceptions and logging.
     *
     * @param operationDescription A description of the operation for logging purposes.
     * @param bulkSql              The complete SQL string to execute for the bulk operation.
     */
    public void executeBulkSql(String operationDescription, String bulkSql) {
        log.info("Starting bulk operation: {}", operationDescription);
        // Open an isolated, fast in-memory DuckDB virtual instance
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:"); Statement stmt = conn.createStatement()) {
            log.debug("Initializing PostgreSQL extension for DuckDB.");
            stmt.execute("INSTALL postgres; LOAD postgres;");
            log.debug("Creating database secret for Postgres connection.");
            createSecret(stmt);
            log.debug("Attaching PostgreSQL database to DuckDB execution path.");
            stmt.execute("ATTACH '' AS pg (TYPE POSTGRES, SECRET pg_secret);");
            log.info("Executing bulk SQL for: {}", operationDescription);
            int rowsAffected = stmt.executeUpdate(bulkSql);
            log.info("Bulk operation '{}' completed successfully. {} rows affected.", operationDescription,
                    rowsAffected);
        } catch (SQLException e) {
            log.error("Failed to execute bulk operation '{}' due to a SQL error.", operationDescription, e);
            throw new RuntimeException("Failed to execute bulk operation: " + operationDescription, e);
        }
    }

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
        // Simple check for SQL injection characters. Consider a more robust validation library if needed.
        if (normalizedPath.contains("'") || normalizedPath.contains(";") || normalizedPath.contains("--")) {
            throw new IllegalArgumentException("Parquet file path contains potentially unsafe characters: " + parquetFilePath);
        }
        return normalizedPath;
    }
}
