package me.daskabel.dummy2pro.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.security.AuthenticatedUser;
import me.daskabel.dummy2pro.security.LoginAttemptService;
import me.daskabel.dummy2pro.security.SecuritySessionKeys;
import me.daskabel.dummy2pro.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * Stellt die Endpunkte für Registrierung und Login bereit.
 *
 * Die eigentliche Fachlogik liegt im UserService. Der Controller nimmt
 * Anfragen entgegen, ruft die passenden Services auf und baut die
 * Antworten für das Frontend.
 */
@RestController
@RequestMapping("/api")
public class AuthController
{
    /**
     * Einheitliche Fehlermeldung für fehlgeschlagene Anmeldungen.
     *
     */
    private static final String GENERIC_LOGIN_ERROR = "Benutzername oder Passwort falsch.";

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UserService userService, LoginAttemptService loginAttemptService)
    {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Registriert einen neuen Benutzer.
     *
     * @param request Benutzername und Passwort
     * @return Bestätigung der erfolgreichen Registrierung
     */
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request)
    {
        User user = userService.register(request.getUsername(), request.getPassword());
        return new RegisterResponse(user.getUsername(), "Registrierung erfolgreich");
    }

    /**
     * Meldet einen Benutzer an und legt die nötigen Sitzungsdaten an.
     *
     * @param request     Benutzername und Passwort
     * @param httpRequest aktuelle HTTP-Anfrage
     * @return Benutzerdaten für das Frontend
     */
    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest)
    {
        String username = request.getUsername();

        if (loginAttemptService.isBlocked(username))
        {
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }

        try
        {
            User user = userService.authenticate(username, request.getPassword());
            loginAttemptService.registerSuccess(username);

            HttpSession existingSession = httpRequest.getSession(false);
            if (existingSession != null)
            {
                existingSession.invalidate();
            }

            HttpSession session = httpRequest.getSession(true);

            session.setAttribute(SecuritySessionKeys.USER_ID, user.getUserId());
            session.setAttribute(SecuritySessionKeys.USERNAME, user.getUsername());

            AuthenticatedUser principal = new AuthenticatedUser(user.getUserId(), user.getUsername());

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_USER")
                    );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Security-Kontext zusätzlich in der Sitzung ablegen,
            // damit Spring Security den Benutzer über weitere Requests erkennt.
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            return new LoginResponse(
                    user.getUserId(),
                    user.getUsername(),
                    userService.resolveAvatarFilename(user),
                    "Login erfolgreich"
            );
        }
        catch (IllegalArgumentException ex)
        {
            loginAttemptService.registerFailure(username);
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
    }

    /**
     * Liest die Benutzer-ID aus der aktuellen Anmeldung aus.
     *
     * @param authentication aktuelle Anmeldung
     * @return Benutzer-ID des eingeloggten Benutzers
     */
    public static Long extractUserId(Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser))
        {
            throw new AccessDeniedException("Nicht eingeloggt.");
        }

        return authenticatedUser.userId();
    }

    /**
     * Wandelt Anmeldefehler in eine 401-Antwort um.
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(UnauthorizedException ex)
    {
        return new ErrorResponse("UNAUTHORIZED", ex.getMessage());
    }

    /**
     * Wandelt ungültige Eingaben in eine 400-Antwort um.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex)
    {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    /**
     * Eigene Ausnahme für fehlgeschlagene oder gesperrte Anmeldungen.
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException
    {
        public UnauthorizedException(String message)
        {
            super(message);
        }
    }

    /**
     * Einheitliches Fehlerobjekt für API-Antworten.
     */
    public static class ErrorResponse
    {
        private String error;
        private String message;

        public ErrorResponse(String error, String message)
        {
            this.error = error;
            this.message = message;
        }

        public String getError()
        {
            return error;
        }

        public String getMessage()
        {
            return message;
        }
    }

    /**
     * Anfragedaten für die Registrierung.
     */
    public static class RegisterRequest
    {
        private String username;
        private String password;

        public RegisterRequest() {}

        public String getUsername()
        {
            return username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }
    }

    /**
     * Anfragedaten für die Anmeldung.
     */
    public static class LoginRequest
    {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername()
        {
            return username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }
    }

    /**
     * Antwort nach erfolgreicher Registrierung.
     */
    public static class RegisterResponse
    {
        private String username;
        private String message;

        public RegisterResponse(String username, String message)
        {
            this.username = username;
            this.message = message;
        }

        public String getUsername()
        {
            return username;
        }

        public String getMessage()
        {
            return message;
        }
    }

    /**
     * Antwort nach erfolgreicher Anmeldung.
     */
    public static class LoginResponse
    {
        private Long userId;
        private String username;
        private String avatar;
        private String message;

        public LoginResponse(Long userId, String username, String avatar, String message)
        {
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.message = message;
        }

        public Long getUserId()
        {
            return userId;
        }

        public String getUsername()
        {
            return username;
        }

        public String getAvatar()
        {
            return avatar;
        }

        public String getMessage()
        {
            return message;
        }
    }
}
