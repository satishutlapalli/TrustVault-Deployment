
package com.trustvault.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION =
            Duration.ofMinutes(15);

    private final ConcurrentMap<String, AttemptRecord> attempts =
            new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        String key = normalize(username);
        AttemptRecord record = attempts.get(key);

        if (record == null) {
            return false;
        }

        synchronized (record) {
            if (record.lockedUntil == null) {
                return false;
            }

            if (Instant.now().isBefore(record.lockedUntil)) {
                return true;
            }

            attempts.remove(key, record);
            return false;
        }
    }

    public void loginFailed(String username) {
        String key = normalize(username);

        AttemptRecord record = attempts.computeIfAbsent(
                key, ignored -> new AttemptRecord());

        synchronized (record) {
            if (record.lockedUntil != null
                    && Instant.now().isBefore(record.lockedUntil)) {
                return;
            }

            if (record.lockedUntil != null) {
                record.failedAttempts = 0;
                record.lockedUntil = null;
            }

            record.failedAttempts++;

            if (record.failedAttempts >= MAX_ATTEMPTS) {
                record.lockedUntil =
                        Instant.now().plus(LOCKOUT_DURATION);
            }
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(normalize(username));
    }

    // Diagnostic method; do not expose through a public API.
    public int getFailedAttempts(String username) {
        AttemptRecord record = attempts.get(normalize(username));

        if (record == null) {
            return 0;
        }

        synchronized (record) {
            return record.failedAttempts;
        }
    }

    private String normalize(String username) {
        return username == null
                ? ""
                : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptRecord {
        private int failedAttempts;
        private Instant lockedUntil;
    }
}
    