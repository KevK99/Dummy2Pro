package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.daskabel.dummy2pro.security.SecuritySessionKeys;
import me.daskabel.dummy2pro.security.AuthenticatedUser;

/**
 * Controller für Authentifizierung.
 *
 * Stellt HTTP-Endpunkte für Registrierung und Login bereit. Für die
 * Kommunikation zwischen Frontend und {@link me.daskabel.dummy2pro.service.UserService}.</p>
 *
 *   Registrierung: Validiert Eingaben, erstellt einen neuen User
 *       und speichert ihn mit BCrypt-Hash in der Tabelle {@code users}.</li>
 *   Login: Prüft Benutzername/Passwort
 *
 * Delegiert für Passwort- und Hashlogik an den Service
 */

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return new RegisterResponse(user.getUsername(), "Registrierung erfolgreich");
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest)
    {
        try {
            User user = userService.authenticate(request.getUsername(), request.getPassword());

            HttpSession existingSession = httpRequest.getSession(false);
            if (existingSession != null)
            {
                existingSession.invalidate();
            }

            HttpSession session = httpRequest.getSession(true);

            session.setAttribute(SecuritySessionKeys.USER_ID, user.getUserId());
            session.setAttribute(SecuritySessionKeys.USERNAME, user.getUsername());

            AuthenticatedUser principal = new AuthenticatedUser(user.getUserId(), user.getUsername());

            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
                    );

            org.springframework.security.core.context.SecurityContext context =
                    org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);

            session.setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            return new LoginResponse(
                    user.getUserId(),
                    user.getUsername(),
                    userService.resolveAvatarFilename(user),
                    "Login erfolgreich"
            );
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    public static Long extractUserId(org.springframework.security.core.Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser))
        {
            throw new org.springframework.security.access.AccessDeniedException("Nicht eingeloggt.");
        }

        return authenticatedUser.userId();
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(UnauthorizedException ex) {
        return new ErrorResponse("UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterResponse {
        private String username;
        private String message;

        public RegisterResponse(String username, String message) {
            this.username = username;
            this.message = message;
        }

        public String getUsername() { return username; }
        public String getMessage() { return message; }
    }

    public static class LoginResponse {
        private Long userId;
        private String username;
        private String avatar;
        private String message;

        public LoginResponse(Long userId, String username, String avatar, String message) {
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.message = message;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getAvatar() { return avatar; }
        public String getMessage() { return message; }
    }
}
