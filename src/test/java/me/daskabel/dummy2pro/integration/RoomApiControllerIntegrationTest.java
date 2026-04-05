package me.daskabel.dummy2pro.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        String body = mockMvc.perform(post("/api/session/new")
                        .param("userId", String.valueOf(user.getUserId())))
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
                        .param("userId", String.valueOf(user.getUserId())))
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

        mockMvc.perform(put("/api/session/{runId}/name", run.getRunId())
                        .param("userId", String.valueOf(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getRunId()))
                .andExpect(jsonPath("$.displayName").value("Mein Spielstand"));

        GameRun reloaded = gameRunRepository.findById(run.getRunId()).orElseThrow();
        assertEquals("Mein Spielstand", reloaded.getDisplayName());
    }

    @Test
    void prepareRoom_getRoom_answer_getStatus_andOverview_workTogether() throws Exception
    {
        User user = new User("jan" + System.nanoTime(), encoder.encode("SehrSicheresPass1!"), "duck.jpg");
        user = userRepository.save(user);

        SeededMcQuestion seeded = seedRoom1WithOneMcQuestion();

        String createBody = mockMvc.perform(post("/api/session/new")
                        .param("userId", String.valueOf(user.getUserId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(createBody).get("sessionId").asText();

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/prepare", sessionId, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.themeName").value("Thema 1"))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.answeredQuestions").value(0))
                .andExpect(jsonPath("$.openQuestions").value(1));

        mockMvc.perform(get("/api/session/{sessionId}/room/{roomId}", sessionId, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.roomId").value(1))
                .andExpect(jsonPath("$.firstQuestion.questionId").value(seeded.question().getQuestionId()))
                .andExpect(jsonPath("$.firstQuestion.questionType").value("MC"))
                .andExpect(jsonPath("$.questionSequence[0]").value(seeded.question().getQuestionId()));

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/advance", sessionId, 1))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Die aktuelle Frage muss zuerst beantwortet werden."));

        String answerJson = objectMapper.writeValueAsString(
                new AnswerRequest(seeded.question().getQuestionId(), List.of(seeded.correctAnswer().getAnswerId()))
        );

        mockMvc.perform(post("/api/session/{sessionId}/room/{roomId}/answer", sessionId, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsEarned").value(5))
                .andExpect(jsonPath("$.correctAnswerIds[0]").value(seeded.correctAnswer().getAnswerId()));

        mockMvc.perform(get("/api/session/{sessionId}/room/{roomId}/status", sessionId, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredQuestions").value(1))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.wrongAnswers").value(0))
                .andExpect(jsonPath("$.earnedPoints").value(5))
                .andExpect(jsonPath("$.medal").value("GOLD"));

        mockMvc.perform(get("/api/session/{sessionId}/overview", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnedPoints").value(5))
                .andExpect(jsonPath("$.totalCorrect").value(1))
                .andExpect(jsonPath("$.totalWrong").value(0))
                .andExpect(jsonPath("$.rooms[0].themeName").value("Thema 1"));
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

        mockMvc.perform(post("/api/session/load")
                        .param("userId", String.valueOf(stranger.getUserId()))
                        .param("runId", String.valueOf(run.getRunId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void overview_forUnknownSession_returnsNotFound() throws Exception
    {
        mockMvc.perform(get("/api/session/{sessionId}/overview", "nicht-vorhanden"))
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

    private record RenameRunRequest(String displayName) {}
    private record AnswerRequest(Long questionId, List<Long> selectedAnswerIds) {}
    private record SeededMcQuestion(Question question, AnswerOption correctAnswer) {}
}