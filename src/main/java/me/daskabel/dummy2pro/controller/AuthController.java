package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller für die Registrierung.
 * Stellt Endpunkte bereit, über die sich Benutzer registrieren können.
 * Aktuell wird nur validiert und ein Passwort-Hash erzeugt, da noch keine DB.
 */

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registriert einen neuen Benutzer.
     * Erwartet JSON:
     * {"username":"abc","password":"Abcdefghijk1!"}
     */

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return new RegisterResponse(user.getUsername(), "Registrierung erfolgreich");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
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

    // --- Kleine DTOs direkt im Controller nur zum Testen ---
    public static class RegisterRequest {
        private String username;
        private String password;

        public RegisterRequest() {}

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
}