package me.daskabel.dummy2pro.integration;

import tools.jackson.databind.ObjectMapper;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für das Löschen von Spielständen über den
 * {@code GameController}.
 *
 * Die Tests prüfen den Endpunkt gegen die echte Spring-Konfiguration
 * inklusive Sicherheit, Session, Repository-Zugriff und Transaktionen.
 * Abgedeckt werden Erfolgsfall, letzter verbleibender Spielstand,
 * fehlende Anmeldung und der Zugriff auf fremde Spielstände.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameControllerIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Test
    void deleteGameRun_success_deletesSelectedRun_whenUserHasMultipleRuns() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password);

        GameRun firstRun = createRun(user, "Run A");
        GameRun secondRun = createRun(user, "Run B");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        mockMvc.perform(delete("/api/game/{runId}", firstRun.getRunId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Spielstand erfolgreich gelöscht."));

        assertFalse(gameRunRepository.findById(firstRun.getRunId()).isPresent());
        assertTrue(gameRunRepository.findById(secondRun.getRunId()).isPresent());
    }

    @Test
    void deleteGameRun_lastRemainingRun_returnsBadRequest() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password);

        GameRun onlyRun = createRun(user, "Einziger Run");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        mockMvc.perform(delete("/api/game/{runId}", onlyRun.getRunId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Der letzte Spielstand kann nicht gelöscht werden."));

        assertTrue(gameRunRepository.findById(onlyRun.getRunId()).isPresent());
    }

    @Test
    void deleteGameRun_withoutLogin_returnsUnauthorized() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password);
        GameRun run = createRun(user, "Run");

        mockMvc.perform(delete("/api/game/{runId}", run.getRunId())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteGameRun_foreignRun_returnsNotFound() throws Exception
    {
        String password = "SehrSicheresPass1!";

        User owner = createUser("owner" + System.nanoTime(), password);
        User otherUser = createUser("other" + System.nanoTime(), password);

        GameRun ownerRunA = createRun(owner, "Owner Run A");
        createRun(owner, "Owner Run B");

        MockHttpSession otherSession = loginAndReturnSession(otherUser.getUsername(), password);

        mockMvc.perform(delete("/api/game/{runId}", ownerRunA.getRunId())
                        .session(otherSession)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Spielstand nicht gefunden"));

        assertTrue(gameRunRepository.findById(ownerRunA.getRunId()).isPresent());
    }

    private User createUser(String username, String plainPassword)
    {
        return userRepository.save(new User(username, encoder.encode(plainPassword), "duck.jpg"));
    }

    private GameRun createRun(User user, String displayName)
    {
        GameRun run = new GameRun();
        run.setUser(user);
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        run.setDisplayName(displayName);
        return gameRunRepository.save(run);
    }

    private MockHttpSession loginAndReturnSession(String username, String password) throws Exception
    {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/login")
                        .header("Origin", "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
