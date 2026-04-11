package me.daskabel.dummy2pro.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService
{
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration OBSERVATION_WINDOW = Duration.ofMinutes(10);
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final Clock clock;
    private final Map<String, AttemptState> attemptsByUsername = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock)
    {
        this.clock = clock;
    }

    public boolean isBlocked(String username)
    {
        String key = normalizeKey(username);
        if (key == null)
        {
            return false;
        }

        AttemptState state = attemptsByUsername.get(key);
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
            attemptsByUsername.remove(key);
        }

        return false;
    }

    public void registerFailure(String username)
    {
        String key = normalizeKey(username);
        if (key == null)
        {
            return;
        }

        Instant now = clock.instant();
        AttemptState state = attemptsByUsername.get(key);

        if (state == null || state.isExpired(now))
        {
            attemptsByUsername.put(key, new AttemptState(1, now, null));
            return;
        }

        if (state.isLocked(now))
        {
            return;
        }

        int updatedFailedAttempts = state.failedAttempts + 1;
        Instant lockedUntil = updatedFailedAttempts >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;

        attemptsByUsername.put(key, new AttemptState(updatedFailedAttempts, state.firstFailedAt, lockedUntil));

        if (lockedUntil != null)
        {
            log.warn(
                    "Login für Benutzer '{}' bis {} gesperrt nach {} Fehlversuchen.",
                    key,
                    lockedUntil,
                    updatedFailedAttempts
            );
        }
    }

    public void registerSuccess(String username)
    {
        String key = normalizeKey(username);
        if (key == null)
        {
            return;
        }

        attemptsByUsername.remove(key);
    }

    private String normalizeKey(String username)
    {
        if (username == null)
        {
            return null;
        }

        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }

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