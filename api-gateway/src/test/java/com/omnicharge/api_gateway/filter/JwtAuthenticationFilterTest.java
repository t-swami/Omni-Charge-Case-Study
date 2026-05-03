package com.omnicharge.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        lenient().when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("filter() - allows public endpoints to pass through without token")
    void filter_publicEndpoint_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("filter() - blocks request missing Authorization header")
    void filter_missingAuthHeader_returnsUnauthorized() {
        // Mock chain filter should NOT be called.
        reset(filterChain);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain, jwtUtil);
    }

    @Test
    @DisplayName("filter() - blocks request with weakly formatted Authorization header")
    void filter_invalidAuthHeaderFormat_returnsUnauthorized() {
        reset(filterChain);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Basic token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain, jwtUtil);
    }

    @Test
    @DisplayName("filter() - blocks request with invalid token")
    void filter_invalidToken_returnsUnauthorized() {
        reset(filterChain);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("invalid.jwt.token")).thenReturn(false);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("filter() - allows request with valid token and mutates headers")
    void filter_validToken_mutatesRequestAndPassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("testuser");
        when(jwtUtil.extractRole("valid.jwt.token")).thenReturn("ROLE_USER");

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        
        verify(filterChain, times(1)).filter(argThat(mutatedExchange -> {
            String username = mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-Username");
            String role = mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-Role");
            return "testuser".equals(username) && "ROLE_USER".equals(role);
        }));
    }

    @Test
    @DisplayName("getOrder() - returns -1")
    void getOrder_returnsMinusOne() {
        assertEquals(-1, jwtFilter.getOrder());
    }
}
