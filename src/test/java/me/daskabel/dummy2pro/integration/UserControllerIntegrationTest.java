package me.daskabel.dummy2pro.integration;

import tools.jackson.databind.ObjectMapper;
import me.daskabel.dummy2pro.model.User;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Test
    void getUserProfile_success_returnsCurrentUserFromSession() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password, " ");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        mockMvc.perform(get("/api/user/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"));
    }

    @Test
    void getUserProfile_withoutLogin_returnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAvatar_success_updatesCurrentUser() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password, "duck.jpg");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        String json = objectMapper.writeValueAsString(Map.of("avatar", "bee.jpg"));

        mockMvc.perform(put("/api/user/me/avatar")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.avatar").value("bee.jpg"));

        assertTrue(userRepository.findById(user.getUserId()).orElseThrow().getAvatar().equals("bee.jpg"));
    }

    @Test
    void updateUsername_success_updatesCurrentUser() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("alt" + System.nanoTime(), password, "duck.jpg");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        String newUsername = "neu" + System.nanoTime();
        String json = objectMapper.writeValueAsString(Map.of("username", newUsername));

        mockMvc.perform(put("/api/user/me/username")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(newUsername));

        assertTrue(userRepository.findById(user.getUserId()).orElseThrow().getUsername().equals(newUsername));
    }

    @Test
    void updatePassword_confirmationMismatch_returnsBadRequest() throws Exception
    {
        String password = "AktuellesPasswort1!";
        User user = createUser("jan" + System.nanoTime(), password, "duck.jpg");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        String json = objectMapper.writeValueAsString(Map.of(
                "currentPassword", password,
                "newPassword", "NeuesPasswort123!",
                "newPasswordConfirm", "AnderesPasswort123!"
        ));

        mockMvc.perform(put("/api/user/me/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Die neuen Passwörter stimmen nicht überein."));
    }

    @Test
    void logout_success_thenProtectedEndpointIsUnauthorized() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password, "duck.jpg");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        mockMvc.perform(post("/api/user/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Erfolgreich ausgeloggt."));

        mockMvc.perform(get("/api/user/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_withConfirmation_deletesUser_andSessionCannotReadProfileAfterwards() throws Exception
    {
        String password = "SehrSicheresPass1!";
        User user = createUser("jan" + System.nanoTime(), password, "duck.jpg");

        MockHttpSession session = loginAndReturnSession(user.getUsername(), password);

        mockMvc.perform(delete("/api/user/me")
                        .session(session)
                        .with(csrf())
                        .param("confirmation", "CONFIRM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Benutzer erfolgreich gelöscht."));

        assertFalse(userRepository.findById(user.getUserId()).isPresent());

        mockMvc.perform(get("/api/user/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String username, String plainPassword, String avatar)
    {
        return userRepository.save(new User(username, encoder.encode(plainPassword), avatar));
    }

    private MockHttpSession loginAndReturnSession(String username, String password) throws Exception
    {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}