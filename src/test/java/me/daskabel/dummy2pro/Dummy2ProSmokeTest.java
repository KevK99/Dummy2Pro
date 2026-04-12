package me.daskabel.dummy2pro;

import me.daskabel.dummy2pro.controller.AuthController;
import me.daskabel.dummy2pro.controller.RoomApiController;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import me.daskabel.dummy2pro.service.RoomService;
import me.daskabel.dummy2pro.service.UserService;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Einfache Smoke-Tests für den grundlegenden Start der Anwendung.
 *
 * Die Klasse prüft nur die gröbsten Basisannahmen:
 * - Spring-Kontext fährt hoch
 * - zentrale Beans und Repositories sind vorhanden
 * - öffentliche und geschützte API-Endpunkte reagieren sicherheitstechnisch wie erwartet
 *
 * Ziel ist kein fachlicher Tiefentest, sondern ein schneller Hinweis darauf,
 * ob die Anwendung in einer Testumgebung grundsätzlich lauffähig ist.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Dummy2ProSmokeTest
{
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private QuizSessionManager quizSessionManager;

    @Autowired
    private AuthController authController;

    @Autowired
    private RoomApiController roomApiController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private QuestionProgressRepository questionProgressRepository;

    @Test
    void contextLoads_withCoreBeansAndRepositories()
    {
        assertNotNull(applicationContext);

        assertNotNull(userService);
        assertNotNull(roomService);
        assertNotNull(quizSessionManager);

        assertNotNull(authController);
        assertNotNull(roomApiController);

        assertNotNull(userRepository);
        assertNotNull(gameRunRepository);
        assertNotNull(questionRepository);
        assertNotNull(themeRepository);
        assertNotNull(questionProgressRepository);
    }

    @Test
    void protectedAndPublicApiEndpoints_haveExpectedSecurityBehavior() throws Exception
    {
        mockMvc.perform(get("/api/session/runs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "niemand",
                              "password": "falsch"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }
}
