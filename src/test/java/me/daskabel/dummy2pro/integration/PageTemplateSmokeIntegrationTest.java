package me.daskabel.dummy2pro.integration;

import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke-Tests für die serverseitig gerenderten Seiten und Raumtemplates.
 *
 * Die Tests prüfen nicht die komplette Fachlogik der Seiten, sondern nur,
 * ob die Templates mit realem Spring-Kontext, echter Session und den nötigen
 * Modeldaten ohne Templatefehler rendern. Damit fallen kaputte Includes,
 * fehlerhafte Fragmente oder ungültige Template-Ausdrücke schnell auf.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PageTemplateSmokeIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Test
    void dashboard_review_endscreen_and_profile_renderWithoutTemplateCrash() throws Exception
    {
        seedThemes(15);
        User user = userRepository.save(new User(uniqueUsername("smoke-pages"), encoder.encode("SehrSicheresPass1!"), "duck.jpg"));
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"roomsGrid\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("headlineTitle")));

        mockMvc.perform(get("/review").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"reviewRoomsContainer\"")));

        mockMvc.perform(get("/endscreen").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"themeTableBody\"")));

        mockMvc.perform(get("/profile.html").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"runsContainer\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"currentNameInput\"")));
    }

    @Test
    void allRoomTemplates_renderWithoutTemplateCrash() throws Exception
    {
        seedThemes(15);
        User user = userRepository.save(new User(uniqueUsername("smoke-rooms"), encoder.encode("SehrSicheresPass1!"), "duck.jpg"));
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        int highestPlayableRoomId = (int) themeRepository.findAllByOrderByThemeIdAsc().stream()
                .filter(theme -> !Objects.equals(theme.getThemeId(), 17L))
                .limit(15)
                .count();

        IntStream.rangeClosed(1, highestPlayableRoomId).forEach(roomId -> {
            try
            {
                mockMvc.perform(get("/room/{id}", roomId).session(session))
                        .andExpect(status().isOk())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"questionPanel\"")))
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"statusPanel\"")))
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"submitAnswerBtn\"")));
            }
            catch (Exception exception)
            {
                throw new RuntimeException("Room smoke failed for room " + roomId, exception);
            }
        });

        // Raum 16 ist als Sonderfall zusätzlich fest abgesichert.
        mockMvc.perform(get("/room/16").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"questionPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"statusPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"submitAnswerBtn\"")));
    }

    private int getPlayableRoomCount()
    {
        return (int) themeRepository.findAllByOrderByThemeIdAsc().stream()
                .filter(theme -> !Objects.equals(theme.getThemeId(), 17L))
                .limit(15)
                .count();
    }

    private void seedThemes(int count)
    {
        long playableThemeCount = themeRepository.findAllByOrderByThemeIdAsc().stream()
                .filter(theme -> !Objects.equals(theme.getThemeId(), 17L))
                .limit(15)
                .count();

        if (playableThemeCount >= count)
        {
            return;
        }

        IntStream.rangeClosed((int) playableThemeCount + 1, count)
                .mapToObj(index -> new Theme("Thema Smoke " + index, "Beschreibung " + index))
                .forEach(themeRepository::save);
    }

    private MockHttpSession loginAndReturnSession(String username, String password) throws Exception
    {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/login")
                        .header("Origin", "http://localhost")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String uniqueUsername(String prefix)
    {
        String value = prefix + "_" + Long.toString(System.nanoTime(), 36);
        return value.length() > 30 ? value.substring(0, 30) : value;
    }
}
