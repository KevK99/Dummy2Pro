package me.daskabel.dummy2pro.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.Theme;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für die API rund um Sitzungen, Räume und Spielstände.
 *
 * Die Tests laufen gegen den echten Spring-Kontext und prüfen das
 * Zusammenspiel von Anmeldung, Session-Verwaltung, Spielstandlogik,
 * Raumvorbereitung, Antwortverarbeitung und Übersichtsdaten.
 *
 * Abgedeckt werden sowohl Erfolgsfälle als auch typische Fehlersituationen
 * wie ungültige Raum-IDs, falsche Frage-IDs oder der Zugriff auf fremde
 * Spielstände.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoomApiControllerIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Test
    void createNewRun_andGetRunsForUser_workEndToEnd() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        seedRoom1WithOneMcQuestion();

        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        String body = mockMvc.perform(post("/api/session/new")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.runId").isNumber())
                .andExpect(jsonPath("$.activeRoomId").value(1))
                .andExpect(jsonPath("$.overview.username").value(user.getUsername()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        long runId = json.get("runId").asLong();

        mockMvc.perform(get("/api/session/runs")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(runId));
    }

    @Test
    void renameRun_success_trimsDisplayName() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        GameRun run = new GameRun();
        run.setUser(user);
        run.setStartedAt(LocalDateTime.now());
        run.setDisplayName(null);
        run = gameRunRepository.save(run);

        String json = objectMapper.writeValueAsString(new RenameRunRequest("   Mein Spielstand   "));
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(put("/api/session/{runId}/name", run.getRunId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getRunId()))
                .andExpect(jsonPath("$.displayName").value("Mein Spielstand"));

        GameRun reloaded = gameRunRepository.findById(run.getRunId()).orElseThrow();
        assertEquals("Mein Spielstand", reloaded.getDisplayName());
    }

    @Test
    void renameRun_emptyDisplayName_setsNull() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        GameRun run = new GameRun();
        run.setUser(user);
        run.setStartedAt(LocalDateTime.now());
        run.setDisplayName("Vorheriger Name");
        run = gameRunRepository.save(run);

        String json = objectMapper.writeValueAsString(new RenameRunRequest("   "));
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(put("/api/session/{runId}/name", run.getRunId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getRunId()))
                .andExpect(jsonPath("$.displayName").value(nullValue()));

        GameRun reloaded = gameRunRepository.findById(run.getRunId()).orElseThrow();
        assertEquals(null, reloaded.getDisplayName());
    }

    @Test
    void renameRun_tooLongDisplayName_returnsBadRequest() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        GameRun run = new GameRun();
        run.setUser(user);
        run.setStartedAt(LocalDateTime.now());
        run = gameRunRepository.save(run);

        String tooLongName = "a".repeat(41);
        String json = objectMapper.writeValueAsString(new RenameRunRequest(tooLongName));
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(put("/api/session/{runId}/name", run.getRunId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Der Spielstandname darf maximal 40 Zeichen lang sein."));
    }

    @Test
    void prepareRoom_getRoom_answer_getStatus_andOverview_workTogether() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        SeededMcQuestion seeded = seedRoom1WithOneMcQuestion();
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        String createBody = mockMvc.perform(post("/api/session/new")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(createBody).get("sessionId").asText();

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/prepare", sessionId, 1)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.themeName").value("Thema 1"))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.answeredQuestions").value(0))
                .andExpect(jsonPath("$.earnedPoints").value(0));

        String roomBody = mockMvc.perform(get("/api/session/{sessionId}/room/{roomId}", sessionId, 1)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.roomId").value(1))
                .andExpect(jsonPath("$.status.themeName").value("Thema 1"))
                .andExpect(jsonPath("$.firstQuestion.startText").isNotEmpty())
                .andExpect(jsonPath("$.firstQuestion.answerOptions.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode roomJson = objectMapper.readTree(roomBody);
        long currentQuestionId = roomJson.get("firstQuestion").get("questionId").asLong();
        long currentCorrectAnswerId = roomJson.get("firstQuestion")
                .get("answerOptions")
                .get(0)
                .get("answerId")
                .asLong();

        String answerJson = objectMapper.writeValueAsString(
                new AnswerRequest(currentQuestionId, List.of(currentCorrectAnswerId))
        );

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/answer", sessionId, 1)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsEarned").value(5))
                .andExpect(jsonPath("$.correctAnswerIds[0]").value(currentCorrectAnswerId));

        mockMvc.perform(get("/api/session/{sessionId}/room/{roomId}/status", sessionId, 1)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredQuestions").value(1))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.wrongAnswers").value(0))
                .andExpect(jsonPath("$.earnedPoints").value(5))
                .andExpect(jsonPath("$.medal").value("GOLD"));

        mockMvc.perform(get("/api/session/{sessionId}/overview", sessionId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnedPoints").value(5))
                .andExpect(jsonPath("$.totalCorrect").value(1))
                .andExpect(jsonPath("$.totalWrong").value(0))
                .andExpect(jsonPath("$.rooms[0].themeName").value("Thema 1"));
    }

    @Test
    void prepareRoom_invalidRoomId_returnsNotFound() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        seedRoom1WithOneMcQuestion();
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        String createBody = mockMvc.perform(post("/api/session/new")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(createBody).get("sessionId").asText();

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/prepare", sessionId, 999)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Raum 999 in Session nicht gefunden."));
    }

    @Test
    void answer_withWrongQuestionId_returnsBadRequest() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        SeededMcQuestion seeded = seedRoom1WithOneMcQuestion();
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        String createBody = mockMvc.perform(post("/api/session/new")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(createBody).get("sessionId").asText();

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/prepare", sessionId, 1)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        String answerJson = objectMapper.writeValueAsString(
                new AnswerRequest(999999L, List.of(seeded.correctAnswer().getAnswerId()))
        );

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/answer", sessionId, 1)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Es darf nur die aktuelle Frage beantwortet werden."));
    }

    @Test
    void loadRun_success_returnsExistingRunForCorrectUser() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        seedRoom1WithOneMcQuestion();

        String createBody = mockMvc.perform(post("/api/session/new")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createBody);
        long runId = created.get("runId").asLong();

        String loadedBody = mockMvc.perform(post("/api/session/load")
                        .session(session)
                        .with(csrf())
                        .param("runId", String.valueOf(runId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.overview.username").value(user.getUsername()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loaded = objectMapper.readTree(loadedBody);
        assertNotNull(loaded.get("sessionId").asText());
        assertEquals(runId, loaded.get("runId").asLong());
    }

    @Test
    void loadRun_forWrongUser_returnsNotFound() throws Exception
    {
        User owner = new User("owner" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        owner = userRepository.save(owner);

        User stranger = new User("stranger" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        stranger = userRepository.save(stranger);

        GameRun run = new GameRun();
        run.setUser(owner);
        run.setStartedAt(LocalDateTime.now());
        run = gameRunRepository.save(run);

        MockHttpSession strangerSession = loginAndReturnSession(stranger.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(post("/api/session/load")
                        .session(strangerSession)
                        .with(csrf())
                        .param("runId", String.valueOf(run.getRunId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Spielstand nicht gefunden."));
    }

    @Test
    void overview_forUnknownSession_returnsNotFound() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);
        MockHttpSession session = loginAndReturnSession(user.getUsername(), "SehrSicheresPass1!");

        mockMvc.perform(get("/api/session/{sessionId}/overview", "nicht-vorhanden")
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private SeededMcQuestion seedRoom1WithOneMcQuestion()
    {
        Team team = new Team("Team A");
        entityManager.persist(team);

        QuestionSet questionSet = new QuestionSet(team, "Set 1");
        entityManager.persist(questionSet);

        Theme theme = new Theme("Thema 1");
        entityManager.persist(theme);

        Question question = new Question(questionSet, QuestionType.MC, 5);
        question.setStartText("Was ist richtig?");
        question.setAllowsMultiple(false);
        question.setThemes(List.of(theme));
        entityManager.persist(question);

        AnswerOption correct = new AnswerOption(question, "Richtig", true, 1);
        entityManager.persist(correct);

        AnswerOption wrong = new AnswerOption(question, "Falsch", false, 2);
        entityManager.persist(wrong);

        question.setAnswerOptions(List.of(correct, wrong));

        entityManager.flush();
        entityManager.clear();

        return new SeededMcQuestion(question, correct);
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

    private record RenameRunRequest(String displayName) {}
    private record AnswerRequest(Long questionId, List<Long> selectedAnswerIds) {}
    private record SeededMcQuestion(Question question, AnswerOption correctAnswer) {}
}
