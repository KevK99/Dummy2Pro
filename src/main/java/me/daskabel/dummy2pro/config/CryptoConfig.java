package me.daskabel.dummy2pro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Stellt sicherheitsrelevante Hilfsklassen der Anwendung bereit.
 *
 */
@Configuration
public class CryptoConfig
{
    /**
     * Stellt den Passwort-Encoder für Registrierung und Login bereit.
     *
     * @return BCrypt-Encoder für Passwort-Hashing
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
