package me.daskabel.dummy2pro.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Fehlversuche bei Anmeldungen und zeitlich begrenzte Sperren.
 *
 * Die Sperrung erfolgt schlüsselbezogen im Arbeitsspeicher und soll
 * wiederholte Fehlversuche in kurzer Zeit begrenzen.
 */
@Service
public class LoginAttemptService
{
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration OBSERVATION_WINDOW = Duration.ofMinutes(10);
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final Clock clock;
    private final Map<String, AttemptState> attemptsByKey = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock)
    {
        this.clock = clock;
    }

    /**
     * Prüft, ob ein Schlüssel aktuell gesperrt ist.
     */
    public boolean isBlocked(String key)
    {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null)
        {
            return false;
        }

        AttemptState state = attemptsByKey.get(normalizedKey);
        if (state == null)
        {
            return false;
        }

        Instant now = clock.instant();
        if (state.isLocked(now))
        {
            return true;
        }

        if (state.isExpired(now))
        {
            attemptsByKey.remove(normalizedKey);
        }

        return false;
    }

    /**
     * Erfasst einen fehlgeschlagenen Anmeldeversuch.
     *
     * Wird die maximale Anzahl erreicht, wird der Schlüssel
     * für eine begrenzte Zeit gesperrt.
     */
    public void registerFailure(String key)
    {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null)
        {
            return;
        }

        Instant now = clock.instant();
        AttemptState state = attemptsByKey.get(normalizedKey);

        if (state == null || state.isExpired(now))
        {
            attemptsByKey.put(normalizedKey, new AttemptState(1, now, null));
            return;
        }

        if (state.isLocked(now))
        {
            return;
        }

        int updatedFailedAttempts = state.failedAttempts + 1;
        Instant lockedUntil = updatedFailedAttempts >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;

        attemptsByKey.put(
                normalizedKey,
                new AttemptState(updatedFailedAttempts, state.firstFailedAt, lockedUntil)
        );

        if (lockedUntil != null)
        {
            log.warn(
                    "Login vorübergehend gesperrt bis {} nach {} Fehlversuchen.",
                    lockedUntil,
                    updatedFailedAttempts
            );
        }
    }

    /**
     * Entfernt gespeicherte Fehlversuche nach einer erfolgreichen Anmeldung.
     */
    public void registerSuccess(String key)
    {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null)
        {
            return;
        }

        attemptsByKey.remove(normalizedKey);
    }

    /**
     * Vereinheitlicht den Schlüssel für die interne Speicherung.
     */
    private String normalizeKey(String key)
    {
        if (key == null)
        {
            return null;
        }

        String normalized = key.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Interner Zustand für Fehlversuche und eine mögliche Sperre.
     */
    private record AttemptState(int failedAttempts, Instant firstFailedAt, Instant lockedUntil)
    {
        private boolean isLocked(Instant now)
        {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        private boolean isExpired(Instant now)
        {
            if (lockedUntil != null)
            {
                return !now.isBefore(lockedUntil);
            }

            return firstFailedAt.plus(OBSERVATION_WINDOW).isBefore(now);
        }
    }
}