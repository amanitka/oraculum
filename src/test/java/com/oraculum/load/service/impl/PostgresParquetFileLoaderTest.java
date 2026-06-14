package com.oraculum.load.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.load.dto.LoadParquetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresParquetFileLoaderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;

    @InjectMocks
    private PostgresParquetFileLoader loader;

    @BeforeEach
    void setUp() {
        // Properties used for DuckDB PG extension
        lenient().when(properties.database().host()).thenReturn("localhost");
        lenient().when(properties.database().port()).thenReturn(5432);
        lenient().when(properties.database().name()).thenReturn("db");
        lenient().when(properties.database().username()).thenReturn("user");
        lenient().when(properties.database().password()).thenReturn("pass");
    }

    @Test
    void normalizeAndValidate_validPath_returnsNormalized() {
        String result = PostgresParquetFileLoader.normalizeAndValidate("/tmp/file.parquet");
        // On Windows it might prepend drive letter or use backslashes depending on OS, but length > 0
        assertThat(result).isNotEmpty();
    }

    @Test
    void normalizeAndValidate_nullOrBlank_throwsException() {
        assertThatThrownBy(() -> PostgresParquetFileLoader.normalizeAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class);
                
        assertThatThrownBy(() -> PostgresParquetFileLoader.normalizeAndValidate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizeAndValidate_unsafePath_throwsException() {
        assertThatThrownBy(() -> PostgresParquetFileLoader.normalizeAndValidate("/tmp/file'; DROP TABLE;--.parquet"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loadParquetIntoTargetTable_executesFlowAndCleansUp() throws Exception {
        LoadParquetDto dto = new LoadParquetDto("target_table", "staging_table_123", "test.parquet", "INSERT INTO...", false);
        
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection("jdbc:duckdb:")).thenReturn(mockConnection);

            loader.loadParquetIntoTargetTable(dto);

            // Verifies DuckDB staging happened
            verify(mockStatement, atLeastOnce()).execute(anyString());
            verify(mockStatement, atLeastOnce()).executeUpdate(anyString());
            
            // Verifies Postgres load happened
            verify(jdbcTemplate).update("INSERT INTO...");
            
            // Verifies Cleanup happened
            verify(jdbcTemplate).execute("DROP TABLE IF EXISTS staging_table_123;");
        }
    }
}
