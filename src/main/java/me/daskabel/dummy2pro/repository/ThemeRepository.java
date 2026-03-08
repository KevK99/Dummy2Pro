package me.daskabel.dummy2pro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import me.daskabel.dummy2pro.model.Theme;

import java.util.List;

/**
 * Datenbankzugriff für Themes (Themen/Räume). Die 7 Themes entsprechen den 7
 * Räumen — fest in der DB definiert.
 */
public interface ThemeRepository extends JpaRepository<Theme, Long>
{
    List<Theme> findAllByOrderByThemeIdAsc();
}