package com.omnicharge.notification_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Notification JwtUtil Unit Tests")
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
}
