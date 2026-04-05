package me.daskabel.dummy2pro.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}