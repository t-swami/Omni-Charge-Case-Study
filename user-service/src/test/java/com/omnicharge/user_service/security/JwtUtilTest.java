package com.omnicharge.user_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "OmniChargeSecretKeyForJWTTokenGenerationAndValidation2024");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    @DisplayName("generateToken() creates a non-null token")
    void generateToken_returnsNonNull() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("extractUsername() returns the correct username")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("alice");
    }

    @Test
    @DisplayName("extractRole() returns the correct role")
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        String role = jwtUtil.extractRole(token);
        assertThat(role).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("extractRole() returns ROLE_ADMIN for admin token")
    void extractRole_returnsAdmin() {
        String token = jwtUtil.generateToken("admin", "ROLE_ADMIN", "admin@example.com");
        String role = jwtUtil.extractRole(token);
        assertThat(role).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("extractEmail() returns the correct email")
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        String email = jwtUtil.extractEmail(token);
        assertThat(email).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("validateToken() returns true for valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() returns false for tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        String tampered = token + "tampered";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken() returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret",
                "OmniChargeSecretKeyForJWTTokenGenerationAndValidation2024");
        ReflectionTestUtils.setField(expiredJwtUtil, "expiration", 0L);

        String token = expiredJwtUtil.generateToken("alice", "ROLE_USER", "alice@example.com");
        assertThat(expiredJwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken() returns false for garbage string")
    void validateToken_garbageString_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.token")).isFalse();
    }
}
