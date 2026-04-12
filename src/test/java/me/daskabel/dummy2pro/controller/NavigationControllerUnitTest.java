package me.daskabel.dummy2pro.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

/**
 * Unittests für einfache Navigations- und Seitenauswahl im Web-Layer.
 *
 * Getestet werden hier die Rückgabewerte der Controller für Startseite,
 * Dashboard, Räume sowie Endscreen und Review. Die Tests prüfen bewusst nur
 * die Controllerlogik und Modellbefüllung, nicht Rendering oder Routing im
 * Browser.
 */
@ExtendWith(MockitoExtension.class)
class NavigationControllerUnitTest
{
    @Mock
    private ThemeRepository themeRepository;

    @Test
    void home_redirectsToIndex()
    {
        HomeController controller = new HomeController();

        assertEquals("redirect:/index.html", controller.home());
    }

    @Test
    void dashboard_setsExpectedDefaultModelAttributes()
    {
        PageController controller = new PageController(themeRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("dashboard", controller.dashboard(model));
        assertEquals("NutzerName", model.getAttribute("username"));
        assertEquals(0, model.getAttribute("answeredQuestions"));
        assertEquals(0, model.getAttribute("totalQuestions"));
        assertEquals("/images/duck.jpg", model.getAttribute("avatarUrl"));
    }

    @Test
    void room16_usesSpecialEndRoomWithoutRepositoryAccess()
    {
        PageController controller = new PageController(themeRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("room16", controller.room(16, model));
        assertEquals(16, model.getAttribute("roomId"));
        assertEquals("Abschluss", model.getAttribute("themeName"));
    }

    @Test
    void room_withInvalidId_redirectsToDashboard()
    {
        PageController controller = new PageController(themeRepository);
        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(new Theme("Thema 1"), new Theme("Thema 2")));

        assertEquals("redirect:/dashboard", controller.room(0, new ExtendedModelMap()));
        assertEquals("redirect:/dashboard", controller.room(3, new ExtendedModelMap()));
    }

    @Test
    void room_withValidId_usesThemeNameByPosition()
    {
        Theme first = new Theme("Recht");
        Theme second = new Theme("Wirtschaft");
        PageController controller = new PageController(themeRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        when(themeRepository.findAllByOrderByThemeIdAsc()).thenReturn(List.of(first, second));

        assertEquals("room2", controller.room(2, model));
        assertEquals(2, model.getAttribute("roomId"));
        assertEquals("Wirtschaft", model.getAttribute("themeName"));
    }

    @Test
    void endscreenAndReview_returnExpectedViews()
    {
        PageController controller = new PageController(themeRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("endscreen", controller.endscreen());
        assertEquals("review", controller.review(model));
        assertEquals("/images/duck.jpg", model.getAttribute("avatarUrl"));
    }
}
