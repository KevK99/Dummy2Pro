package me.daskabel.dummy2pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.daskabel.dummy2pro.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * JPA-Slice-Test für das {@link UserRepository}.
 *
 * Geprüft werden die projektrelevanten Such- und Existenzabfragen anhand
 * des Benutzernamens.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositorySliceTest
{
    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_shouldReturnSavedUser()
    {
        User user = new User("slice-user", "hash");
        userRepository.save(user);

        User loaded = userRepository.findByUsername("slice-user").orElseThrow();

        assertEquals("slice-user", loaded.getUsername());
        assertEquals("hash", loaded.getPasswordHash());
    }

    @Test
    void existsByUsername_shouldBeTrue_afterSavingUser()
    {
        userRepository.save(new User("already-there", "hash"));

        assertTrue(userRepository.existsByUsername("already-there"));
    }
}
