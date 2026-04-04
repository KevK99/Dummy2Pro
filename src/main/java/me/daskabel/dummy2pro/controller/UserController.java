package me.daskabel.dummy2pro.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId)
    {
        User user = this.userService.getUser(userId);

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(
            @PathVariable Long userId,
            @RequestBody AvatarUpdateRequest request)
    {
        User user = this.userService.updateAvatar(userId, request.getAvatar());

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/username")
    public ResponseEntity<UserProfileResponse> updateUsername(
            @PathVariable Long userId,
            @RequestBody UsernameUpdateRequest request)
    {
        User user = this.userService.updateUsername(userId, request.getUsername());

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<MessageResponse> updatePassword(
            @PathVariable Long userId,
            @RequestBody PasswordUpdateRequest request)
    {
        this.userService.updatePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getNewPasswordConfirm()
        );

        return ResponseEntity.ok(new MessageResponse("Passwort erfolgreich geändert."));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long userId, @RequestParam String confirmation)
    {
        if (!"CONFIRM".equals(confirmation))
        {
            return ResponseEntity.badRequest().body(new MessageResponse("Bestätigung erforderlich."));
        }

        this.userService.deleteUser(userId);
        return ResponseEntity.ok(new MessageResponse("Benutzer erfolgreich gelöscht."));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<MessageResponse> logout(@PathVariable Long userId)
    {
        this.userService.saveCurrentGameProgress(userId);
        return ResponseEntity.ok(new MessageResponse("Erfolgreich ausgeloggt."));
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
}