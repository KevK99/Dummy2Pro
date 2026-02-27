package me.daskabel.dummy2pro.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Hilfscontroller zum schnellen Anzeigen der Datenbankstruktur und -inhalte.
 *
 * Funktionen:
 *
 *   /schema/tables – listet alle Tabellen auf
 *   /schema/columns – listet Spalten auf (zu welcher Tabelle sie gehören und Datentyp)
 *   /schema/count/{table} – liefert die Anzahl Datensätze je Tabelle
 *   /schema/sample/{table} – liefert Beispielzeilen (LIMIT sinnvoll, also ?limit=10 anhängen)
 *   /schema/questions – Ansicht der Fragen inkl. Questionset-Titel
 *
 */

@RestController
@RequestMapping("/schema")
public class SchemaController {

    private final JdbcTemplate jdbc;

    public SchemaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> tables() {
        return jdbc.queryForList("SHOW TABLES");
    }

    @GetMapping("/columns")
    public List<Map<String, Object>> columns() {
        return jdbc.queryForList("""
            SELECT table_name, column_name, data_type, is_nullable, column_key
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
            ORDER BY table_name, ordinal_position
        """);
    }

    @GetMapping("/count/{table}")
    public List<Map<String, Object>> count(@PathVariable String table) {
        // Minimaler Schutz gegen SQL Injection
        if (!table.matches("^[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("invalid table name");
        }
        return jdbc.queryForList("SELECT COUNT(*) AS cnt FROM " + table);
    }

    @GetMapping("/sample/{table}")
    public List<Map<String, Object>> sample(@PathVariable String table,
                                            @RequestParam(defaultValue = "10") int limit) {
        if (!table.matches("^[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("invalid table name");
        }
        limit = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("SELECT * FROM " + table + " LIMIT " + limit);
    }

    @GetMapping("/questions")
    public List<Map<String, Object>> questions(@RequestParam(defaultValue = "50") int limit) {
        limit = Math.max(1, Math.min(limit, 200));
        return jdbc.queryForList("""
            SELECT q.question_id,
                   q.question_type,
                   q.points,
                   q.start_text,
                   qs.title AS question_set
            FROM question q
            JOIN question_set qs ON qs.question_set_id = q.question_set_id
            ORDER BY q.question_id
            LIMIT ?
        """, limit);
    }
}