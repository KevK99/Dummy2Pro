package me.daskabel.dummy2pro.integration;

import tools.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für Registrierung und Anmeldung über den {@code AuthController}.
 *
 * Die Tests laufen gegen den echten Spring-Kontext und prüfen das Zusammenspiel
 * aus Web-Schicht, Sicherheit, Session-Verwaltung, Passwort-Hashing und
 * Persistenz. Abgedeckt werden Erfolgs- und Fehlerfälle bei Registrierung und
 * Login sowie die Weiterverwendung der aufgebauten Sitzung bei geschützten
 * Endpunkten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest
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
    void register_success_createsUserAndInitialRun() throws Exception
    {
        String username = "user" + System.nanoTime();
        String password = "SehrSicheresPass1!";

        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.message").value("Registrierung erfolgreich"));

        User savedUser = userRepository.findByUsername(username).orElseThrow();

        assertNotNull(savedUser.getUserId());
        assertEquals(1, gameRunRepository.countByUser_UserId(savedUser.getUserId()));
    }

    @Test
    void register_duplicateUsername_returnsBadRequest() throws Exception
    {
        User existing = new User("jan", encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        userRepository.save(existing);

        String json = objectMapper.writeValueAsString(Map.of(
                "username", "jan",
                "password", "SehrSicheresPass1!"
        ));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void login_success_returnsDefaultAvatarWhenStoredAvatarIsBlank() throws Exception
    {
        String username = "jan" + System.nanoTime();
        String password = "SehrSicheresPass1!";

        User user = new User(username, encoder.encode(password), " ");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"))
                .andExpect(jsonPath("$.message").value("Login erfolgreich"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception
    {
        String username = "jan" + System.nanoTime();
        String password = "SehrSicheresPass1!";

        User user = new User(username, encoder.encode(password), "duck.jpg");
        userRepository.save(user);

        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "falsch"
        ));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void login_tooManyFailedAttempts_blocksFurtherAttemptsTemporarily() throws Exception
    {
        String username = "limit" + System.nanoTime();
        String password = "SehrSicheresPass1!";

        User user = new User(username, encoder.encode(password), "duck.jpg");
        userRepository.save(user);

        String wrongPasswordJson = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "falsch"
        ));

        for (int attempt = 0; attempt < 5; attempt++)
        {
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongPasswordJson))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Benutzername oder Passwort falsch."));
        }

        String correctPasswordJson = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPasswordJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Benutzername oder Passwort falsch."));
    }

    @Test
    void login_success_thenProtectedEndpointWorksWithReturnedSession() throws Exception
    {
        String username = "jan" + System.nanoTime();
        String password = "SehrSicheresPass1!";

        User user = new User(username, encoder.encode(password), "duck.jpg");
        user = userRepository.save(user);

        MockHttpSession session = loginAndReturnSession(username, password, null);

        mockMvc.perform(get("/api/user/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"));
    }

    @Test
    void login_onSameSession_switchesCurrentUser() throws Exception
    {
        String password = "SehrSicheresPass1!";

        User firstUser = new User("first" + System.nanoTime(), encoder.encode(password), "duck.jpg");
        User secondUser = new User("second" + System.nanoTime(), encoder.encode(password), "bee.jpg");
        firstUser = userRepository.save(firstUser);
        secondUser = userRepository.save(secondUser);

        MockHttpSession firstSession = loginAndReturnSession(firstUser.getUsername(), password, null);

        mockMvc.perform(get("/api/user/me").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(firstUser.getUserId()))
                .andExpect(jsonPath("$.username").value(firstUser.getUsername()));

        MockHttpSession secondSession = loginAndReturnSession(secondUser.getUsername(), password, firstSession);

        mockMvc.perform(get("/api/user/me").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(secondUser.getUserId()))
                .andExpect(jsonPath("$.username").value(secondUser.getUsername()))
                .andExpect(jsonPath("$.avatar").value("bee.jpg"));
    }

    private MockHttpSession loginAndReturnSession(String username, String password, MockHttpSession existingSession) throws Exception
    {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        var requestBuilder = post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        if (existingSession != null)
        {
            requestBuilder.session(existingSession);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
