package me.daskabel.dummy2pro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Anwendung Dummy2Pro.
 *
 * Diese Klasse startet die Spring-Boot-Anwendung und initialisiert den
 * kompletten Anwendungskontext. Dabei werden unter anderem Konfigurationen,
 * Controller, Services, Repositories und Security-Komponenten geladen.
 *
 * Verantwortlichkeiten:
 * - Start der Webanwendung
 * - Aktivierung des automatischen Komponentenscans
 * - Initialisierung der Spring-Boot-Infrastruktur
 *
 * Hinweis:
 * In dieser Klasse befindet sich bewusst keine Fachlogik.
 * Logik gehört in die jeweils zuständigen
 * Controller-, Service- oder Repository-Klassen.
 */
@SpringBootApplication
public class Dummy2proApplication
{
    /**
     * Startet die Anwendung über Spring Boot.
     *
     * @param args optionale Startparameter aus der Kommandozeile
     */
    public static void main(String[] args)
    {
        // Start an Spring Boot übergeben
        SpringApplication.run(Dummy2proApplication.class, args);
    }
}