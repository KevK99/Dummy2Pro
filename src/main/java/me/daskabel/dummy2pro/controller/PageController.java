package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Objects;

/**
 * Liefert die zentralen Seiten der Anwendung aus.
 *
 * Der Controller ist für Seitenaufrufe zuständig und bereitet
 * die nötigen Model-Daten für Dashboard, Räume, Endscreen und Review vor.
 */
@Controller
public class PageController
{
    /**
     * Theme 17 ist der reine Übungsraum und gehört nicht zur normalen Raumfolge.
     */
    private static final long PRACTICE_THEME_ID = 17L;

    private final ThemeRepository themeRepository;

    public PageController(ThemeRepository themeRepository)
    {
        this.themeRepository = themeRepository;
    }

    /**
     * Lädt die regulär spielbaren Themes in aufsteigender Reihenfolge.
     *
     * Der Übungsraum wird bewusst ausgefiltert und nicht als normaler Raum
     * im Fortschritt behandelt.
     */
    private List<Theme> getPlayableThemes()
    {
        return this.themeRepository.findAllByOrderByThemeIdAsc().stream()
                .filter(theme -> !Objects.equals(theme.getThemeId(), PRACTICE_THEME_ID))
                .limit(15)
                .toList();
    }

    /**
     * Liefert das Dashboard.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model)
    {
        model.addAttribute("username", "NutzerName");
        model.addAttribute("answeredQuestions", 0);
        model.addAttribute("totalQuestions", 0);
        model.addAttribute("avatarUrl", "/images/duck.jpg");

        return "dashboard";
    }

    /**
     * Liefert einen normalen Raum anhand seiner Nummer.
     *
     * Raum 16 ist der Abschlussraum und wird gesondert behandelt.
     * Alle übrigen Räume werden aus den spielbaren Themes abgeleitet.
     */
    @GetMapping("/room/{id}")
    public String room(@PathVariable int id, Model model)
    {
        if (id == 16)
        {
            model.addAttribute("roomId", 16);
            model.addAttribute("themeName", "Abschluss");
            return "room16";
        }

        var themes = getPlayableThemes();
        int roomCount = themes.size();

        if (id < 1 || id > roomCount)
        {
            return "redirect:/dashboard";
        }

        var theme = themes.get(id - 1);

        model.addAttribute("roomId", id);
        model.addAttribute("themeName", theme.getName());

        return "room" + id;
    }

    /**
     * Liefert den wiederholbaren Übungsraum für Abkürzungen.
     *
     * Dieser Raum ist unabhängig vom normalen Spielfortschritt erreichbar.
     */
    @GetMapping("/room17")
    public String room17(Model model)
    {
        String themeName = this.themeRepository.findById(17L)
                .map(theme -> theme.getName())
                .orElse("Abkürzungen");

        model.addAttribute("roomId", 17);
        model.addAttribute("themeName", themeName);
        model.addAttribute("avatarUrl", "/images/duck.jpg");

        return "room17";
    }

    /**
     * Liefert den Endscreen.
     */
    @GetMapping("/endscreen")
    public String endscreen()
    {
        return "endscreen";
    }

    /**
     * Liefert die Übersichtsseite für die Nachbetrachtung.
     */
    @GetMapping("/review")
    public String review(Model model)
    {
        model.addAttribute("avatarUrl", "/images/duck.jpg");
        return "review";
    }
}