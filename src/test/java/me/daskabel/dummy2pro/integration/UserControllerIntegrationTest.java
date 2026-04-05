package me.daskabel.dummy2pro.integration;

import tools.jackson.databind.ObjectMapper;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void getUserProfile_success_returnsDefaultAvatarForBlankStoredAvatar() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), " ");
        user = userRepository.save(user);

        mockMvc.perform(get("/api/user/{userId}", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"));
    }

    @Test
    void getUserProfile_unknownUser_returnsNotFound() throws Exception
    {
        mockMvc.perform(get("/api/user/{userId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Benutzer nicht gefunden"));
    }

    @Test
    void updateAvatar_success_updatesAvatar() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(new AvatarUpdateRequest("bee.jpg"));

        mockMvc.perform(put("/api/user/{userId}/avatar", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.avatar").value("bee.jpg"));

        User reloaded = userRepository.findById(user.getUserId()).orElseThrow();
        assertEquals("bee.jpg", reloaded.getAvatar());
    }

    @Test
    void updateAvatar_blankValue_resetsToDefaultAvatar() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "bee.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(new AvatarUpdateRequest(" "));

        mockMvc.perform(put("/api/user/{userId}/avatar", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"));

        User reloaded = userRepository.findById(user.getUserId()).orElseThrow();
        assertEquals("duck.jpg", reloaded.getAvatar());
    }

    @Test
    void updateAvatar_invalidAvatar_returnsBadRequest() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(new AvatarUpdateRequest("verboten.png"));

        mockMvc.perform(put("/api/user/{userId}/avatar", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Ungültiger Avatar."));
    }

    @Test
    void updateUsername_success_updatesUsername() throws Exception
    {
        User user = new User("alt" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        String newUsername = "neu" + System.nanoTime();
        String json = objectMapper.writeValueAsString(new UsernameUpdateRequest(newUsername));

        mockMvc.perform(put("/api/user/{userId}/username", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.username").value(newUsername))
                .andExpect(jsonPath("$.avatar").value("duck.jpg"));

        User reloaded = userRepository.findById(user.getUserId()).orElseThrow();
        assertEquals(newUsername, reloaded.getUsername());
    }

    @Test
    void updateUsername_duplicateUsername_returnsBadRequest() throws Exception
    {
        User first = new User("first" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        User second = new User("second" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");

        first = userRepository.save(first);
        second = userRepository.save(second);

        String json = objectMapper.writeValueAsString(new UsernameUpdateRequest(first.getUsername()));

        mockMvc.perform(put("/api/user/{userId}/username", second.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Username ist bereits vergeben."));
    }

    @Test
    void updatePassword_success_updatesStoredPasswordHash() throws Exception
    {
        String oldPassword = "AktuellesPasswort1!";
        String newPassword = "NeuesPasswort123!";

        User user = new User("jan" + System.nanoTime(), encoder.encode(oldPassword), "duck.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(
                new PasswordUpdateRequest(oldPassword, newPassword, newPassword)
        );

        mockMvc.perform(put("/api/user/{userId}/password", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Passwort erfolgreich geändert."));

        User reloaded = userRepository.findById(user.getUserId()).orElseThrow();
        assertTrue(encoder.matches(newPassword, reloaded.getPasswordHash()));
        assertFalse(encoder.matches(oldPassword, reloaded.getPasswordHash()));
    }

    @Test
    void updatePassword_confirmationMismatch_returnsBadRequest() throws Exception
    {
        String oldPassword = "AktuellesPasswort1!";

        User user = new User("jan" + System.nanoTime(), encoder.encode(oldPassword), "duck.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(
                new PasswordUpdateRequest(oldPassword, "NeuesPasswort123!", "AnderesPasswort123!")
        );

        mockMvc.perform(put("/api/user/{userId}/password", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Die neuen Passwörter stimmen nicht überein."));
    }

    @Test
    void updatePassword_wrongCurrentPassword_returnsBadRequest() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("AktuellesPasswort1!"), "duck.jpg");
        user = userRepository.save(user);

        String json = objectMapper.writeValueAsString(
                new PasswordUpdateRequest("Falsch123!", "NeuesPasswort123!", "NeuesPasswort123!")
        );

        mockMvc.perform(put("/api/user/{userId}/password", user.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Das aktuelle Passwort ist falsch."));
    }

    @Test
    void deleteUser_withoutConfirmation_returnsBadRequest() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        mockMvc.perform(delete("/api/user/{userId}", user.getUserId())
                        .param("confirmation", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bestätigung erforderlich."));

        assertTrue(userRepository.findById(user.getUserId()).isPresent());
    }

    @Test
    void deleteUser_withConfirmation_deletesUser() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        mockMvc.perform(delete("/api/user/{userId}", user.getUserId())
                        .param("confirmation", "CONFIRM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Benutzer erfolgreich gelöscht."));

        assertTrue(userRepository.findById(user.getUserId()).isEmpty());
    }

    private record AvatarUpdateRequest(String avatar) {}
    private record UsernameUpdateRequest(String username) {}
    private record PasswordUpdateRequest(String currentPassword, String newPassword, String newPasswordConfirm) {}
}