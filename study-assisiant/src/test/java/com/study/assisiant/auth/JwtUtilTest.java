package com.study.assisiant.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET =
            "UnitTestSecretKeyForJwtSecurity2026!!";

    @Test
    void generateAndParseToken() {
        new JwtUtil().setSecret(SECRET);

        String token = JwtUtil.generateToken(42L, "tester", "USER");
        Claims claims = JwtUtil.parseToken(token);

        assertEquals("42", claims.getSubject());
        assertEquals("tester", claims.get("username"));
        assertEquals("USER", claims.get("role"));
    }

    @Test
    void parseInvalidTokenThrows() {
        new JwtUtil().setSecret(SECRET);

        assertThrows(Exception.class, () ->
                JwtUtil.parseToken("bad.token.value"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        new JwtUtil().setSecret(SECRET);
        String token = JwtUtil.generateToken(1L, "tester", "USER");

        new JwtUtil().setSecret("AnotherSecretKeyForJwtSecurity2026!!!");

        assertThrows(Exception.class, () -> JwtUtil.parseToken(token));

        new JwtUtil().setSecret(SECRET);
    }
}
