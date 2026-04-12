package me.daskabel.dummy2pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import me.daskabel.dummy2pro.model.Theme;

/**
 * Stellt Datenbankzugriffe für Themes bereit.
 *
 * Enthält Methoden zum Laden aller Themes in definierter Reihenfolge.
 */
public interface ThemeRepository extends JpaRepository<Theme, Long>
{
    List<Theme> findAllByOrderByThemeIdAsc();
}
