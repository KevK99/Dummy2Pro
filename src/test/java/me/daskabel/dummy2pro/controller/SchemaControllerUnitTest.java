package me.daskabel.dummy2pro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SchemaControllerUnitTest
{
    @Mock
    private JdbcTemplate jdbcTemplate;

    private SchemaController controller;

    @BeforeEach
    void setUp()
    {
        controller = new SchemaController(jdbcTemplate);
    }

    @Test
    void tables_andColumns_delegateToJdbcTemplate()
    {
        List<Map<String, Object>> tables = List.of(Map.of("TABLE_NAME", "users"));
        List<Map<String, Object>> columns = List.of(Map.of("column_name", "user_id"));

        when(jdbcTemplate.queryForList("SHOW TABLES")).thenReturn(tables);
        when(jdbcTemplate.queryForList(argThat(sql -> sql.contains("information_schema.columns")))).thenReturn(columns);

        assertEquals(tables, controller.tables());
        assertEquals(columns, controller.columns());
    }

    @Test
    void count_allowsSafeTableNamesOnly()
    {
        List<Map<String, Object>> rows = List.of(Map.of("cnt", 5));
        when(jdbcTemplate.queryForList("SELECT COUNT(*) AS cnt FROM users_2026")).thenReturn(rows);

        assertEquals(rows, controller.count("users_2026"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> controller.count("users;DROP"));
        assertEquals("invalid table name", ex.getMessage());
    }

    @Test
    void sample_clampsLimitToMinimumAndMaximum()
    {
        List<Map<String, Object>> lowRows = List.of(Map.of("id", 1));
        List<Map<String, Object>> highRows = List.of(Map.of("id", 2));

        when(jdbcTemplate.queryForList("SELECT * FROM team LIMIT 1")).thenReturn(lowRows);
        when(jdbcTemplate.queryForList("SELECT * FROM team LIMIT 100")).thenReturn(highRows);

        assertEquals(lowRows, controller.sample("team", 0));
        assertEquals(highRows, controller.sample("team", 999));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> controller.sample("team-1", 5));
        assertEquals("invalid table name", ex.getMessage());
    }

    @Test
    void questions_clampsRequestedLimit()
    {
        List<Map<String, Object>> minRows = List.of(Map.of("question_id", 1));
        List<Map<String, Object>> maxRows = List.of(Map.of("question_id", 2));

        when(jdbcTemplate.queryForList(
                argThat(sql -> sql != null && sql.contains("FROM question q") && sql.contains("LIMIT ?")),
                eq(1)
        )).thenReturn(minRows);

        when(jdbcTemplate.queryForList(
                argThat(sql -> sql != null && sql.contains("FROM question q") && sql.contains("LIMIT ?")),
                eq(200)
        )).thenReturn(maxRows);

        assertEquals(minRows, controller.questions(0));
        assertEquals(maxRows, controller.questions(999));

        verify(jdbcTemplate).queryForList(
                argThat(sql -> sql != null && sql.contains("JOIN question_set qs")),
                eq(1)
        );
        verify(jdbcTemplate).queryForList(
                argThat(sql -> sql != null && sql.contains("JOIN question_set qs")),
                eq(200)
        );
    }
}