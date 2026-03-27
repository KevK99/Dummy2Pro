package me.daskabel.dummy2pro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.daskabel.dummy2pro.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController
{
    private final UserService userService;

    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId, @RequestParam String confirmation)
    {
        if (!"CONFIRM".equals(confirmation))
        {
            return ResponseEntity.badRequest().body("Bestätigung erforderlich.");
        }

        userService.deleteUser(userId);
        return ResponseEntity.ok("Benutzer erfolgreich gelöscht.");
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<String> logout(@PathVariable Long userId)
    {
        // Speichere den aktuellen Spielstand
        userService.saveCurrentGameProgress(userId);

        // Optional: Hier könntest du auch die Session invalidieren, wenn du Sessions verwendest.

        return ResponseEntity.ok("Erfolgreich ausgeloggt.");
    }
}
