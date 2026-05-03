package com.omnicharge.recharge_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Recharge JwtUtil Unit Tests")
class JwtUtilTest {

    private static final String SECRET = "OmniChargeSecretKeyForJWTTokenGenerationAndValidation2024";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    /**
     * Helper: generate a token with email claim (simulating what user-service does).
     */
    private String generateTestToken(String username, String role, String email) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("validateToken - returns false for invalid token")
    void validateToken_invalidToken() {
        boolean isValid = jwtUtil.validateToken("invalid.token.string");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - returns true for valid token")
    void validateToken_validToken() {
        String token = generateTestToken("alice", "ROLE_USER", "alice@test.com");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("extractUsername - returns correct username from valid token")
    void extractUsername_validToken() {
        String token = generateTestToken("alice", "ROLE_USER", "alice@test.com");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("extractUsername - throws exception on invalid token")
    void extractUsername_invalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("invalid.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("extractRole - returns correct role from valid token")
    void extractRole_validToken() {
        String token = generateTestToken("alice", "ROLE_USER", "alice@test.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("extractEmail - returns correct email from valid token")
    void extractEmail_validToken() {
        String token = generateTestToken("alice", "ROLE_USER", "alice@test.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("extractEmail - returns null when email claim is missing")
    void extractEmail_missingClaim() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String tokenWithoutEmail = Jwts.builder()
                .setSubject("alice")
                .claim("role", "ROLE_USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        assertThat(jwtUtil.extractEmail(tokenWithoutEmail)).isNull();
    }
}
