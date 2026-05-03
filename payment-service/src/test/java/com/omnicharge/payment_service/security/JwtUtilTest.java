package com.omnicharge.payment_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "OmniChargeSecretKeyForJWTTokenGenerationAndValidation2024");
    }

    @Test
    @DisplayName("validateToken - returns false for invalid token")
    void validateToken_invalidToken() {
        boolean isValid = jwtUtil.validateToken("invalid.token.string");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("extractUsername - throws exception on invalid token")
    void extractUsername_invalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("invalid.token"))
                .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("extractRole - throws exception on invalid token")
    void extractRole_invalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractRole("invalid.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("extractUsername and Role - Success path")
    void extract_success() {
        String token = io.jsonwebtoken.Jwts.builder()
                .setSubject("alice")
                .claim("role", "ROLE_USER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("OmniChargeSecretKeyForJWTTokenGenerationAndValidation2024".getBytes()))
                .compact();

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
    }
}
