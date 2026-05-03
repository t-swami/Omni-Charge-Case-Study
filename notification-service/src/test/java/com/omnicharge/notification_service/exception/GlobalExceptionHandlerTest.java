package com.omnicharge.notification_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleRuntimeException() - returns 400 BAD_REQUEST")
    void handleRuntimeException_returns400() {
        RuntimeException ex = new RuntimeException("Message processing failed");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Message processing failed");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsKey("status");
    }

    @Test
    @DisplayName("handleGenericException() - returns 500 INTERNAL_SERVER_ERROR")
    void handleGenericException_returns500() throws Exception {
        Exception ex = new Exception("Unexpected serialization error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message",
                "An unexpected error occurred. Please try again later.");
        // Should not expose internal error details
        assertThat(response.getBody().toString()).doesNotContain("Unexpected serialization error");
    }
}
