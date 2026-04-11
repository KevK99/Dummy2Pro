package me.daskabel.dummy2pro.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceUnitTest
{
    private MutableClock clock;
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp()
    {
        this.clock = new MutableClock(Instant.parse("2026-04-10T10:00:00Z"));
        this.loginAttemptService = new LoginAttemptService(clock);
    }

    @Test
    void fiveFailures_blockUser()
    {
        for (int i = 0; i < 5; i++)
        {
            loginAttemptService.registerFailure("jan");
        }

        assertTrue(loginAttemptService.isBlocked("jan"));
    }

    @Test
    void successfulLogin_clearsPreviousFailures()
    {
        loginAttemptService.registerFailure("jan");
        loginAttemptService.registerFailure("jan");

        loginAttemptService.registerSuccess("jan");

        assertFalse(loginAttemptService.isBlocked("jan"));

        loginAttemptService.registerFailure("jan");
        loginAttemptService.registerFailure("jan");
        loginAttemptService.registerFailure("jan");
        loginAttemptService.registerFailure("jan");

        assertFalse(loginAttemptService.isBlocked("jan"));
    }

    @Test
    void observationWindowExpiry_resetsFailureCounter()
    {
        for (int i = 0; i < 4; i++)
        {
            loginAttemptService.registerFailure("jan");
        }

        clock.advanceSeconds(601);
        loginAttemptService.registerFailure("jan");

        assertFalse(loginAttemptService.isBlocked("jan"));
    }

    @Test
    void lockExpiresAfterConfiguredDuration()
    {
        for (int i = 0; i < 5; i++)
        {
            loginAttemptService.registerFailure("jan");
        }

        assertTrue(loginAttemptService.isBlocked("jan"));

        clock.advanceSeconds(601);

        assertFalse(loginAttemptService.isBlocked("jan"));
    }

    private static class MutableClock extends Clock
    {
        private Instant currentInstant;

        private MutableClock(Instant currentInstant)
        {
            this.currentInstant = currentInstant;
        }

        @Override
        public ZoneId getZone()
        {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return currentInstant;
        }

        private void advanceSeconds(long seconds)
        {
            currentInstant = currentInstant.plusSeconds(seconds);
        }
    }
}