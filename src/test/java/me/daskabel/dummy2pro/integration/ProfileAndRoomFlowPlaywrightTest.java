package me.daskabel.dummy2pro.integration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.persistence.EntityManager;
import me.daskabel.dummy2pro.model.AnswerOption;
import me.daskabel.dummy2pro.model.Question;
import me.daskabel.dummy2pro.model.QuestionSet;
import me.daskabel.dummy2pro.model.QuestionType;
import me.daskabel.dummy2pro.model.Team;
import me.daskabel.dummy2pro.model.Theme;
import me.daskabel.dummy2pro.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileAndRoomFlowPlaywrightTest
{
    private static final Pattern DASHBOARD_URL_PATTERN = Pattern.compile(".*/dashboard(?:\\?.*)?$");

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    void launchBrowser()
    {
        boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));

        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(headless)
        );
    }

    @AfterAll
    void closeBrowser()
    {
        if (this.browser != null)
        {
            this.browser.close();
        }

        if (this.playwright != null)
        {
            this.playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage()
    {
        this.context = this.browser.newContext();
        this.page = this.context.newPage();
        this.page.setDefaultTimeout(15000);
        this.page.setDefaultNavigationTimeout(15000);
    }

    @AfterEach
    void closeContext()
    {
        if (this.context != null)
        {
            this.context.close();
        }
    }

    @Test
    void profilePage_updatesUsernameAvatarPassword_andRequiresRelogin() 
    {
        String username = uniqueUsername("profile");
        String password = "SehrSicheresPass1!";
        String renamedUsername = uniqueUsername("profilneu");
        String newPassword = "NochSichererPass2!";

        userService.register(username, password);
        login(username, password);

        this.page.navigate(baseUrl() + "/profile.html", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        this.page.waitForSelector("#currentNameInput");

        assertEquals(username, this.page.inputValue("#currentNameInput"));

        this.page.click("#button");
        this.page.fill("#newNameInput", renamedUsername);
        this.page.click("#newNameButton");
        this.page.waitForSelector("#dummy2pro-modal-root button[data-action='ok']");
        this.page.click("#dummy2pro-modal-root button[data-action='ok']");
        assertEquals(renamedUsername, this.page.inputValue("#currentNameInput"));

        this.page.click("#profilePicBtn");
        this.page.click("#profilePicGrid img[alt='flowers']");
        this.page.click("#saveNewProfilePic");
        this.page.waitForSelector("#dummy2pro-modal-root button[data-action='ok']");
        this.page.click("#dummy2pro-modal-root button[data-action='ok']");

        String avatarSrc = this.page.getAttribute("#currentProfileImage", "src");
        assertNotNull(avatarSrc);
        assertTrue(avatarSrc.contains("flowers.jpg"));

        this.page.click("#pwFormEnable");
        this.page.fill("#pw", password);
        this.page.fill("#newPw", newPassword);
        this.page.fill("#newPwConfirm", newPassword);
        this.page.click("#passwordForm button:has-text('Passwort ändern')");

        this.page.waitForFunction(
                "() => window.location.pathname === '/' || window.location.pathname === '/index.html'"
        );
        this.page.waitForSelector("#loginForm");
        this.page.waitForFunction(
                "() => (document.getElementById('loginSuccessMessage')?.textContent || '').includes('Passwort erfolgreich geändert')"
        );

        String loginSuccess = this.page.textContent("#loginSuccessMessage");
        assertNotNull(loginSuccess);
        assertTrue(loginSuccess.contains("Passwort erfolgreich geändert"));

        login(renamedUsername, newPassword);

        this.page.navigate(baseUrl() + "/profile.html", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        this.page.waitForSelector("#currentNameInput");
        this.page.waitForFunction(
                "expectedName => document.getElementById('currentNameInput')?.value === expectedName",
                renamedUsername
        );

        assertEquals(renamedUsername, this.page.inputValue("#currentNameInput"));

        String avatarAfterRelogin = this.page.getAttribute("#currentProfileImage", "src");
        assertNotNull(avatarAfterRelogin);
        assertTrue(avatarAfterRelogin.contains("flowers.jpg"));
    }

    @Test
    void dashboard_profile_and_roomFlow_coverRunCreationLogout_andQuestionAnswering() 
    {
        seedRoom1WithImageQuestion();

        String username = uniqueUsername("roomflow");
        String password = "SehrSicheresPass1!";
        userService.register(username, password);

        login(username, password);
        this.page.waitForFunction(
                "() => !!sessionStorage.getItem('sessionId') && !!sessionStorage.getItem('runId')"
        );

        this.page.navigate(baseUrl() + "/profile.html", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        this.page.waitForSelector("#runsContainer");
        this.page.waitForFunction("() => document.querySelectorAll('#runsContainer button').length >= 3");

        int initialSelectButtons = this.page.locator("button:has-text('Auswählen')").count();
        this.page.click("#createRunBtn");
        this.page.waitForFunction(
                "expected => Array.from(document.querySelectorAll('#runsContainer button')).filter(button => button.textContent.includes('Auswählen')).length >= expected",
                initialSelectButtons + 1
        );
        assertTrue(this.page.locator("button:has-text('Auswählen')").count() >= initialSelectButtons + 1);

        this.page.evaluate("() => { window.Dummy2ProUI.confirm = async () => true; }");
        this.page.locator("button:has-text('Ausloggen')").first().click();
        this.page.waitForFunction(
                "() => window.location.pathname === '/' || window.location.pathname === '/index.html'"
        );
        this.page.waitForSelector("#loginForm");

        login(username, password);
        this.page.waitForFunction("() => !!sessionStorage.getItem('sessionId') && !!sessionStorage.getItem('runId')");

        this.page.navigate(baseUrl() + "/room/1", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        this.page.waitForSelector("#welcomeContainer");
        this.page.waitForFunction("() => { const next = document.getElementById('dialogNextBtn'); const start = document.getElementById('startQuizBtn'); return (next && !next.classList.contains('hidden')) || (start && !start.classList.contains('hidden')); }");

        Locator nextDialogButton = this.page.locator("#dialogNextBtn");
        Locator startQuizButton = this.page.locator("#startQuizBtn");
        for (int i = 0; i < 10; i++)
        {
            if (nextDialogButton.isVisible())
            {
                nextDialogButton.click();
                continue;
            }

            if (startQuizButton.isVisible())
            {
                startQuizButton.click();
                break;
            }
        }

        this.page.waitForSelector(".quiz-answer-btn");
        assertEquals(1, this.page.locator("img[alt='Fragebild']").count());

        Response answerResponse = this.page.waitForResponse(
                response -> response.url().contains("/room/1/answer") && response.status() == 200,
                () -> {
                    this.page.locator(".quiz-answer-btn").first().click();
                    this.page.click("#submitAnswerBtn");
                }
        );
        assertEquals(200, answerResponse.status());

        assertTrue(this.page.textContent("#feedbackBox").contains("Richtig!"));
        assertTrue(this.page.textContent("#questionProgress").contains("1/1"));

        this.page.click("#nextQuestionBtn");
        this.page.waitForFunction("() => document.getElementById('questionText')?.textContent?.includes('Raum abgeschlossen.')");
        assertTrue(this.page.textContent("#feedbackBox").contains("Du hast alle Fragen dieses Raums beantwortet."));
    }

    private void login(String username, String password)
    {
        this.page.navigate(baseUrl() + "/");
        this.page.waitForSelector("#loginForm");
        this.page.fill("#username", username);
        this.page.fill("#password", password);

        Response response = this.page.waitForResponse(
                r -> r.url().endsWith("/api/login") && r.status() == 200,
                () -> this.page.click("#loginForm button[type='submit']")
        );

        assertEquals(200, response.status());

        this.page.waitForURL(DASHBOARD_URL_PATTERN);
        this.page.waitForFunction(
                "() => !!sessionStorage.getItem('userId')"
        );
    }

    private void seedRoom1WithImageQuestion()
    {
        new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
            Theme theme = entityManager.createQuery(
                            "select t from Theme t where t.name = :name order by t.themeId asc",
                            Theme.class
                    )
                    .setParameter("name", "Thema 1")
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElseGet(() -> {
                        Theme created = new Theme("Thema 1", "Raum 1 für E2E");
                        entityManager.persist(created);
                        return created;
                    });

            Team team = new Team("Playwright Team " + System.nanoTime());
            entityManager.persist(team);

            QuestionSet questionSet = new QuestionSet(team, "Playwright Set");
            entityManager.persist(questionSet);

            Question question = new Question(questionSet, QuestionType.MC, 5);
            question.setStartText("Welche Aussage ist korrekt?");
            question.setImageUrl("/images/duck.jpg");
            question.setAllowsMultiple(false);
            question.setThemes(java.util.List.of(theme));
            entityManager.persist(question);

            entityManager.persist(new AnswerOption(question, "Diese Antwort ist richtig", true, 1));
            entityManager.persist(new AnswerOption(question, "Diese Antwort ist falsch", false, 2));
            entityManager.flush();
        });
    }

    private String baseUrl()
    {
        return "http://127.0.0.1:" + this.port;
    }

    private String uniqueUsername(String prefix)
    {
        String value = prefix + "_" + Long.toString(System.nanoTime(), 36);
        return value.length() > 30 ? value.substring(0, 30) : value;
    }
}
