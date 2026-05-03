package com.omnicharge.operator_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Operator GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleRuntimeException() - returns 400 BAD_REQUEST with message")
    void handleRuntimeException_returns400() {
        RuntimeException ex = new RuntimeException("Operator not found with id: 99");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Operator not found with id: 99");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsKey("status");
    }

    @Test
    @DisplayName("handleGenericException() - returns 500 INTERNAL_SERVER_ERROR with safe message")
    void handleGenericException_returns500WithSafeMessage() throws Exception {
        Exception ex = new Exception("NullPointerException in controller");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message",
                "An unexpected error occurred. Please try again later.");
        // Ensure raw exception is not exposed
        assertThat(response.getBody().toString()).doesNotContain("NullPointerException");
    }
}
