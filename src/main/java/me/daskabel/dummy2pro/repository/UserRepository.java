package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
* Stellt Datenbankzugriffe für Benutzer bereit.
 *
 + Vereinfacht den Prozess mit den Usern. Suche geht schneller
 * Enthält Suchmethoden für Benutzer anhand ihres Namens.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
