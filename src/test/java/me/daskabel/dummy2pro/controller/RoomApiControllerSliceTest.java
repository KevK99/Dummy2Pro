package me.daskabel.dummy2pro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import me.daskabel.dummy2pro.dto.RoomDtos.RoomStartDto;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.QuestionProgressRepository;
import me.daskabel.dummy2pro.repository.QuestionRepository;
import me.daskabel.dummy2pro.repository.ThemeRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
import me.daskabel.dummy2pro.security.AuthenticatedUser;
import me.daskabel.dummy2pro.session.QuizSession;
import me.daskabel.dummy2pro.session.QuizSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice-Test für den {@link RoomApiController}.
 *
 * Getestet wird nur die Web-Schicht des Controllers mit gemockten
 * Abhängigkeiten. Der Fokus liegt hier auf Request-Mapping, Validierung,
 * Principal-Auswertung und den JSON-Antworten der Endpunkte.
 */
@WebMvcTest(RoomApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomApiControllerSliceTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizSessionManager sessionManager;

    @MockitoBean
    private GameRunRepository gameRunRepository;

    @MockitoBean
    private ThemeRepository themeRepository;

    @MockitoBean
    private QuestionRepository questionRepository;

    @MockitoBean
    private QuestionProgressRepository questionProgressRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void renameRun_shouldTrimDisplayName_andReturnSavedRun() throws Exception
    {
        GameRun run = new GameRun();
        run.setRunId(77L);
        run.setDisplayName("Alt");

        when(gameRunRepository.findByRunIdAndUser_UserId(77L, 4L)).thenReturn(Optional.of(run));
        when(gameRunRepository.save(any(GameRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/session/77/name")
                        .principal(auth(4L, "jan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Neuer Name  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(77))
                .andExpect(jsonPath("$.displayName").value("Neuer Name"));

        // Geprüft wird nicht nur die Antwort, sondern auch, dass der gespeicherte
        // Spielstandname tatsächlich bereits getrimmt im Repository ankommt.
        verify(gameRunRepository).save(argThat(savedRun -> "Neuer Name".equals(savedRun.getDisplayName())));
    }

    @Test
    void renameRun_shouldReturnBadRequest_whenDisplayNameIsTooLong() throws Exception
    {
        GameRun run = new GameRun();
        run.setRunId(77L);

        when(gameRunRepository.findByRunIdAndUser_UserId(77L, 4L)).thenReturn(Optional.of(run));

        String tooLong = "x".repeat(41);

        mockMvc.perform(put("/api/session/77/name")
                        .principal(auth(4L, "jan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Der Spielstandname darf maximal 40 Zeichen lang sein."));
    }

    @Test
    void startSession_shouldReturnSessionId_andFirstRoom() throws Exception
    {
        QuizSession session = new QuizSession(4L, 88L);
        session.addRoom(new QuizSession.RoomSession(1, "Recht", java.util.List.of(), java.util.Map.of(), 0));

        RoomStartDto firstRoom = new RoomStartDto();

        when(sessionManager.createNewRunSession(4L)).thenReturn(session);
        when(sessionManager.getRoomState(session.getSessionId(), 1)).thenReturn(firstRoom);

        mockMvc.perform(post("/api/session/start")
                        .principal(auth(4L, "jan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()))
                .andExpect(jsonPath("$.firstRoom").exists());
    }

    private Authentication auth(Long userId, String username)
    {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username);

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
    }
}
