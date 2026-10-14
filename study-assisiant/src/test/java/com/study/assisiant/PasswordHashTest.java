package com.study.assisiant;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    @Test
    void bcryptRoundTrip() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("Study123456");

        assertTrue(encoder.matches("Study123456", hash));
        assertFalse(encoder.matches("wrong-password", hash));
    }
}
