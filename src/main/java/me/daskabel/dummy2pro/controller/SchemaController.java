package me.daskabel.dummy2pro.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Stellt Hilfsendpunkte für Einblicke in die aktuelle Datenbank bereit.
 *
 * Der Controller ist nur im Profil "dev" aktiv und dient vor allem
 * zum Prüfen von Tabellen, Spalten, Datensätzen und Beispielinhalten.
 */
@Profile("dev")
@RestController
@RequestMapping("/schema")
public class SchemaController
{
    private final JdbcTemplate jdbc;

    public SchemaController(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /**
     * Liefert alle Tabellen der aktuell verbundenen Datenbank.
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> tables()
    {
        return jdbc.queryForList("SHOW TABLES");
    }

    /**
     * Liefert Spalteninformationen aller Tabellen der aktuellen Datenbank.
     */
    @GetMapping("/columns")
    public List<Map<String, Object>> columns()
    {
        return jdbc.queryForList("""
            SELECT table_name, column_name, data_type, is_nullable, column_key
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
            ORDER BY table_name, ordinal_position
        """);
    }

    /**
     * Liefert die Anzahl der Datensätze einer Tabelle.
     *
     * Der Tabellenname wird vor dem Einbau in die Abfrage geprüft.
     */
    @GetMapping("/count/{table}")
    public List<Map<String, Object>> count(@PathVariable String table)
    {
        // Nur einfache Tabellennamen ohne Sonderzeichen zulassen.
        if (!table.matches("^[A-Za-z0-9_]+$"))
        {
            throw new IllegalArgumentException("invalid table name");
        }

        return jdbc.queryForList("SELECT COUNT(*) AS cnt FROM " + table);
    }

    /**
     * Liefert Beispielzeilen einer Tabelle.
     *
     * Die Anzahl wird auf einen sinnvollen Bereich begrenzt.
     */
    @GetMapping("/sample/{table}")
    public List<Map<String, Object>> sample(
            @PathVariable String table,
            @RequestParam(defaultValue = "10") int limit)
    {
        if (!table.matches("^[A-Za-z0-9_]+$"))
        {
            throw new IllegalArgumentException("invalid table name");
        }

        limit = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("SELECT * FROM " + table + " LIMIT " + limit);
    }

    /**
     * Liefert eine einfache Übersicht über Fragen und ihren Fragensatz.
     */
    @GetMapping("/questions")
    public List<Map<String, Object>> questions(@RequestParam(defaultValue = "50") int limit)
    {
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
