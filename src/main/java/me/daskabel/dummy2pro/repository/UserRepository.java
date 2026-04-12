package me.daskabel.dummy2pro.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.daskabel.dummy2pro.model.User;

/**
 * Stellt Datenbankzugriffe für Benutzer bereit.
 *
 * Enthält die projektrelevanten Suchmethoden für Benutzer anhand des
 * Benutzernamens sowie die Prüfung, ob ein Benutzername bereits vergeben ist.
 */
public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
