package com.omnicharge.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Use a sufficiently long base64 compliant string or just 256+ bit key string
    private final String testSecret = "mySuperSecretKeyForTestingJwtWhichIsVeryLongIndeed";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
    }

    private String generateValidToken() {
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject("testuser")
                .claim("role", "ROLE_USER")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken() {
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject("testuser")
                .claim("role", "ROLE_USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // Expired 1 hour ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("validateToken() - returns true for valid token")
    void validateToken_validToken_returnsTrue() {
        String token = generateValidToken();
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken() - returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        String token = generateExpiredToken();
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken() - returns false for malformed token")
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }

    @Test
    @DisplayName("extractUsername() - correctly extracts and returns subject")
    void extractUsername_returnsSubject() {
        String token = generateValidToken();
        assertEquals("testuser", jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("extractRole() - correctly extracts and returns role claim")
    void extractRole_returnsRole() {
        String token = generateValidToken();
        assertEquals("ROLE_USER", jwtUtil.extractRole(token));
    }
}
