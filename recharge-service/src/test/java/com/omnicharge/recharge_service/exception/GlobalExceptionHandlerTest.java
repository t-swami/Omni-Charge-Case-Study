package com.omnicharge.recharge_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recharge GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleRuntimeException() - returns 400 with business error message")
    void handleRuntimeException_returns400() {
        RuntimeException ex = new RuntimeException("Recharge not found with id: 99");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Recharge not found with id: 99");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGenericException() - returns 500 with safe message, no internal details")
    void handleGenericException_returns500() throws Exception {
        Exception ex = new Exception("Internal DB failure");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message",
                "An unexpected error occurred. Please try again later.");
        // Raw exception detail should NOT be exposed
        assertThat(response.getBody().toString()).doesNotContain("Internal DB failure");
    }
}
