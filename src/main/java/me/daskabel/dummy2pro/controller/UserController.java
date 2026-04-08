package me.daskabel.dummy2pro.controller;

import java.util.NoSuchElementException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;

import me.daskabel.dummy2pro.security.SecuritySessionKeys;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController
{
    public static class MessageResponse
    {
        private final String message;

        public MessageResponse(String message)
        {
            this.message = message;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    public static class ErrorResponse
    {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message)
        {
            this.error = error;
            this.message = message;
        }

        public String getError()
        {
            return this.error;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    public static class UserProfileResponse
    {
        private Long userId;
        private String username;
        private String avatar;

        public Long getUserId()
        {
            return this.userId;
        }

        public void setUserId(Long userId)
        {
            this.userId = userId;
        }

        public String getUsername()
        {
            return this.username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }

        public String getAvatar()
        {
            return this.avatar;
        }

        public void setAvatar(String avatar)
        {
            this.avatar = avatar;
        }
    }

    public static class AvatarUpdateRequest
    {
        private String avatar;

        public String getAvatar()
        {
            return this.avatar;
        }

        public void setAvatar(String avatar)
        {
            this.avatar = avatar;
        }
    }

    public static class UsernameUpdateRequest
    {
        private String username;

        public String getUsername()
        {
            return this.username;
        }

        public void setUsername(String username)
        {
            this.username = username;
        }
    }

    public static class PasswordUpdateRequest
    {
        private String currentPassword;
        private String newPassword;
        private String newPasswordConfirm;

        public String getCurrentPassword()
        {
            return this.currentPassword;
        }

        public void setCurrentPassword(String currentPassword)
        {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword()
        {
            return this.newPassword;
        }

        public void setNewPassword(String newPassword)
        {
            this.newPassword = newPassword;
        }

        public String getNewPasswordConfirm()
        {
            return this.newPasswordConfirm;
        }

        public void setNewPasswordConfirm(String newPasswordConfirm)
        {
            this.newPasswordConfirm = newPasswordConfirm;
        }
    }

    private final UserService userService;

    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication)
    {
        User user = this.userService.getUser(requireCurrentUserId(authentication));

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(
            Authentication authentication,
            @RequestBody AvatarUpdateRequest request)
    {
        User user = this.userService.updateAvatar(requireCurrentUserId(authentication), request.getAvatar());

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/username")
    public ResponseEntity<UserProfileResponse> updateUsername(
            Authentication authentication,
            @RequestBody UsernameUpdateRequest request)
    {
        User user = this.userService.updateUsername(requireCurrentUserId(authentication), request.getUsername());

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> updatePassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest request)
    {
        this.userService.updatePassword(
                requireCurrentUserId(authentication),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getNewPasswordConfirm()
        );

        return ResponseEntity.ok(new MessageResponse("Passwort erfolgreich geändert."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String confirmation)
    {
        if (!"CONFIRM".equals(confirmation))
        {
            return ResponseEntity.badRequest().body(new MessageResponse("Bestätigung erforderlich."));
        }

        Long userId = requireCurrentUserId(authentication);
        this.userService.deleteUser(userId);

        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return ResponseEntity.ok(new MessageResponse("Benutzer erfolgreich gelöscht."));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
    {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.ok(new MessageResponse("Erfolgreich ausgeloggt."));
    }

    private Long requireCurrentUserId(Authentication authentication)
    {
        return AuthController.extractUserId(authentication);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex)
    {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex)
    {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }
}