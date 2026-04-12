package me.daskabel.dummy2pro.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.daskabel.dummy2pro.model.User;
import me.daskabel.dummy2pro.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Stellt Endpunkte für Profil- und Kontoverwaltung bereit.
 *
 * Der Controller liefert Profildaten, übernimmt Änderungen an Name,
 * Passwort und Avatar und ermöglicht Logout sowie das Löschen des
 * Benutzerkontos.
 */
@RestController
@RequestMapping("/api/user")
public class UserController
{
    /**
     * Einfache Standardantwort für erfolgreiche oder erwartbare Meldungen.
     */
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

    /**
     * Einheitliches Fehlerobjekt für API-Antworten.
     */
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

    /**
     * Antwortformat für Profildaten des aktuellen Benutzers.
     */
    public static class UserProfileResponse
    {
        private Long userId;
        private String username;
        private String avatar;
        private String avatarShape;
        private String selectedAvatarFrame;

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

        public String getAvatarShape()
        {
            return avatarShape;
        }

        public void setAvatarShape(String avatarShape)
        {
            this.avatarShape = avatarShape;
        }

        public String getSelectedAvatarFrame()
        {
            return selectedAvatarFrame;
        }

        public void setSelectedAvatarFrame(String selectedAvatarFrame)
        {
            this.selectedAvatarFrame = selectedAvatarFrame;
        }
    }

    /**
     * Anfragedaten zum Ändern des Avatars.
     */
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

    /**
     * Anfragedaten zum Ändern von Form und Rahmen des Avatars.
     */
    public static class AvatarStyleUpdateRequest
    {
        private String avatarShape;
        private String selectedAvatarFrame;

        public String getAvatarShape()
        {
            return avatarShape;
        }

        public void setAvatarShape(String avatarShape)
        {
            this.avatarShape = avatarShape;
        }

        public String getSelectedAvatarFrame()
        {
            return selectedAvatarFrame;
        }

        public void setSelectedAvatarFrame(String selectedAvatarFrame)
        {
            this.selectedAvatarFrame = selectedAvatarFrame;
        }
    }

    /**
     * Anfragedaten zum Ändern des Benutzernamens.
     */
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

    /**
     * Anfragedaten zum Ändern des Passworts.
     */
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

    /**
     * Liefert das Profil des aktuell angemeldeten Benutzers.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication)
    {
        User user = this.userService.getUser(requireCurrentUserId(authentication));

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));
        response.setAvatarShape(user.getAvatarShape());
        response.setSelectedAvatarFrame(user.getSelectedAvatarFrame());

        return ResponseEntity.ok(response);
    }

    /**
     * Ändert den Avatar des aktuellen Benutzers.
     */
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
        response.setAvatarShape(user.getAvatarShape());
        response.setSelectedAvatarFrame(user.getSelectedAvatarFrame());

        return ResponseEntity.ok(response);
    }

    /**
     * Ändert Form und Rahmen des Avatars.
     */
    @PutMapping("/me/avatar-style")
    public ResponseEntity<UserProfileResponse> updateAvatarStyle(
            Authentication authentication,
            @RequestBody AvatarStyleUpdateRequest request)
    {
        User user = this.userService.updateAvatarStyle(
                requireCurrentUserId(authentication),
                request.getAvatarShape(),
                request.getSelectedAvatarFrame()
        );

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setAvatar(this.userService.resolveAvatarFilename(user));
        response.setAvatarShape(user.getAvatarShape());
        response.setSelectedAvatarFrame(user.getSelectedAvatarFrame());

        return ResponseEntity.ok(response);
    }

    /**
     * Ändert den Benutzernamen des aktuellen Benutzers.
     */
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

    /**
     * Ändert das Passwort des aktuellen Benutzers.
     */
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

    /**
     * Löscht das Benutzerkonto nach ausdrücklicher Bestätigung.
     */
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

    /**
     * Meldet den aktuellen Benutzer ab.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
    {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.ok(new MessageResponse("Erfolgreich ausgeloggt."));
    }

    /**
     * Liest die Benutzer-ID des aktuellen Benutzers aus der Anmeldung aus.
     */
    private Long requireCurrentUserId(Authentication authentication)
    {
        return AuthController.extractUserId(authentication);
    }

    /**
     * Wandelt ungültige Eingaben in eine 400-Antwort um.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex)
    {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * Wandelt nicht gefundene Daten in eine 404-Antwort um.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex)
    {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * Wandelt unzulässige Zugriffe in eine 403-Antwort um.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex)
    {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }
}
