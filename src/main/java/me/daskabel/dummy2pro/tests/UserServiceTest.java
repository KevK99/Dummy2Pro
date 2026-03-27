package me.daskabel.dummy2pro.tests;


import me.daskabel.dummy2pro.service.UserService ;
import me.daskabel.dummy2pro.* ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;  // Die zu testende Klasse

    @Test
    void register_ShouldSaveUser_WhenInputIsValid() {
        public User register(String username, String password) {
            validateUsername(username);  // ← TEST: Username-Validierung
            validatePassword(password);   // ← TEST: Passwort-Validierung

            if (userRepository.existsByUsername(username)) {  // ← TEST: Doppelter Username
                throw new IllegalArgumentException("Username ist bereits vergeben.");
            }

            String passwordHash = encoder.encode(password);
            User user = new User(username, passwordHash);

            return userRepository.save(user);
        }
        public User register(String username, String password) {
            validateUsername(username);  // ← TEST: Username-Validierung
            validatePassword(password);   // ← TEST: Passwort-Validierung

            if (userRepository.existsByUsername(username)) {  // ← TEST: Doppelter Username
                throw new IllegalArgumentException("Username ist bereits vergeben.");
            }

            String passwordHash = encoder.encode(password);  // ← TEST: Hashing (mit Mock)
            User user = new User(username, passwordHash);

            return userRepository.save(user);  // ← TEST: Save mit Mock prüfen
        }
        public boolean login(String username, String password) {
            validateLoginInput(username, password);  // ← TEST: Input-Validierung

            return userRepository.findByUsername(username)  // ← TEST: User existiert?
                    .map(u -> encoder.matches(password, u.getPasswordHash()))  // ← TEST: Passwort-Match?
                    .orElse(false);  // ← TEST: Unbekannter User → false
        }
        public User authenticate(String username, String password) {
            validateLoginInput(username, password);  // ← TEST: Input-Validierung

            User user = userRepository.findByUsername(username)  // ← TEST: User existiert?
                    .orElseThrow(() -> new IllegalArgumentException("Benutzername oder Passwort falsch."));

            if (!encoder.matches(password, user.getPasswordHash())) {  // ← TEST: Falsches Passwort
                throw new IllegalArgumentException("Benutzername oder Passwort falsch.");
            } }

            return user;

            private void validateUsername(String username) {
                // ← TEST: null, leer
                if (username == null || username.isBlank()) { ... }
                // ← TEST: zu kurz (<3)
                if (username.length() < 3) { ... }
                // ← TEST: zu lang (>30)
                if (username.length() > 30) { ... }
            }


    }
}
