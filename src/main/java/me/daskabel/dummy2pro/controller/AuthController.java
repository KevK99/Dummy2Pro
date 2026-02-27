package me.daskabel.dummy2pro.controller;

import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;

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
    public LoginResponse login(@RequestBody LoginRequest request) {
        boolean ok = userService.login(request.getUsername(), request.getPassword()); // <-- HIER

        if (!ok) {
            throw new UnauthorizedException("Benutzername oder Passwort falsch.");
        }

        return new LoginResponse(request.getUsername(), "Login erfolgreich");
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
        private String username;
        private String message;

        public LoginResponse(String username, String message) {
            this.username = username;
            this.message = message;
        }

        public String getUsername() { return username; }
        public String getMessage() { return message; }
    }
}