package me.daskabel.dummy2pro.integration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.repository.GameRunRepository;
import me.daskabel.dummy2pro.repository.UserRepository;
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

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFrontendPlaywrightTest
{
    private static final Pattern DASHBOARD_URL_PATTERN = Pattern.compile(".*/dashboard(?:\\?.*)?$");

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

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
    void registerPage_success_createsUserAndInitialRun()
    {
        String username = uniqueUsername("reg");
        String password = "SehrSicheresPass1!";

        this.page.navigate(baseUrl() + "/register.html");
        this.page.waitForSelector("#registerForm");

        this.page.fill("#username", username);
        this.page.fill("#password", password);

        Response response = this.page.waitForResponse(
                r -> r.url().endsWith("/api/register") && r.status() == 200,
                () -> this.page.click("button[type='submit']")
        );

        assertEquals(200, response.status());

        // Die App leitet auf "/" weiter und lädt dort login.html dynamisch nach.
        // Nicht auf eine starre URL warten, sondern auf das echte Ergebnis.
        this.page.waitForSelector("#loginForm");
        this.page.waitForSelector("#loginSuccessMessage:not(.hidden)");

        assertTrue(isLoginUrl(this.page.url()));

        String successMessage = this.page.textContent("#loginSuccessMessage");
        assertNotNull(successMessage);
        assertTrue(successMessage.contains("Registrierung erfolgreich"));

        User savedUser = this.userRepository.findByUsername(username).orElseThrow();
        assertNotNull(savedUser.getUserId());
        assertEquals(1, this.gameRunRepository.countByUser_UserId(savedUser.getUserId()));
    }

    @Test
    void loginPage_success_redirectsToDashboard_andStoresSessionData()
    {
        String username = uniqueUsername("loginok");
        String password = "SehrSicheresPass1!";

        this.userService.register(username, password);

        openLoginPage();

        this.page.fill("#username", username);
        this.page.fill("#password", password);

        Response response = this.page.waitForResponse(
                r -> r.url().endsWith("/api/login") && r.status() == 200,
                () -> this.page.click("button[type='submit']")
        );

        assertEquals(200, response.status());

        this.page.waitForURL(
                DASHBOARD_URL_PATTERN,
                new Page.WaitForURLOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
        );

        this.page.waitForSelector(
                "#headlineTitle",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED)
        );
        this.page.waitForSelector(
                "#roomsGrid",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED)
        );

        assertTrue(DASHBOARD_URL_PATTERN.matcher(this.page.url()).matches());

        String headlineText = this.page.textContent("#headlineTitle");
        assertNotNull(headlineText);
        assertTrue(headlineText.contains("Willkommen zurück"));

        // Erst NACH abgeschlossener Navigation auf sessionStorage zugreifen.
        String storedUserId = (String) this.page.evaluate("() => sessionStorage.getItem('userId')");
        String storedUsername = (String) this.page.evaluate("() => sessionStorage.getItem('username')");
        String storedAvatar = (String) this.page.evaluate("() => sessionStorage.getItem('avatar')");

        assertNotNull(storedUserId);
        assertFalse(storedUserId.isBlank());
        assertEquals(username, storedUsername);
        assertEquals("duck.jpg", storedAvatar);
    }

    @Test
    void loginPage_wrongPassword_showsErrorMessage_andStaysOnLogin()
    {
        String username = uniqueUsername("loginbad");
        String password = "SehrSicheresPass1!";

        this.userService.register(username, password);

        openLoginPage();

        this.page.fill("#username", username);
        this.page.fill("#password", "falsch");

        Response response = this.page.waitForResponse(
                r -> r.url().endsWith("/api/login") && r.status() == 401,
                () -> this.page.click("button[type='submit']")
        );

        assertEquals(401, response.status());

        this.page.waitForSelector("#loginErrorMessage:not(.hidden)");

        String errorMessage = this.page.textContent("#loginErrorMessage");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Benutzername oder Passwort falsch."));
        assertTrue(isLoginUrl(this.page.url()));
    }

    private void openLoginPage()
    {
        this.page.navigate(baseUrl() + "/");
        this.page.waitForSelector("#loginForm");
        assertTrue(isLoginUrl(this.page.url()));
    }

    private boolean isLoginUrl(String url)
    {
        return url.equals(baseUrl() + "/") || url.endsWith("/index.html");
    }

    private String baseUrl()
    {
        return "http://127.0.0.1:" + this.port;
    }

    private String uniqueUsername(String prefix)
    {
        String suffix = Long.toString(System.nanoTime(), 36);
        String value = prefix + "_" + suffix;

        if (value.length() > 30)
        {
            return value.substring(0, 30);
        }

        return value;
    }
}