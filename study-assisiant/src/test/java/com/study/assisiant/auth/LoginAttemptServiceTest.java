package com.study.assisiant.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    void locksAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService("3", "10");
        String ip = "127.0.0.1";

        service.onFailure("alice", ip);
        service.onFailure("alice", ip);
        assertDoesNotThrow(() -> service.checkBlocked("alice", ip));

        service.onFailure("alice", ip);
        assertThrows(ResponseStatusException.class,
                () -> service.checkBlocked("alice", ip));
    }

    @Test
    void successResetsCounter() {
        LoginAttemptService service = new LoginAttemptService("3", "10");
        String ip = "10.0.0.1";

        service.onFailure("bob", ip);
        service.onFailure("bob", ip);
        service.onSuccess("bob", ip);
        service.onFailure("bob", ip);

        assertDoesNotThrow(() -> service.checkBlocked("bob", ip));
    }

    @Test
    void differentAccountIsNotAffected() {
        LoginAttemptService service = new LoginAttemptService("2", "10");
        String ip = "10.0.0.2";

        service.onFailure("carol", ip);
        service.onFailure("carol", ip);

        assertDoesNotThrow(() -> service.checkBlocked("dave", ip));
        assertThrows(ResponseStatusException.class,
                () -> service.checkBlocked("carol", ip));
    }
}
