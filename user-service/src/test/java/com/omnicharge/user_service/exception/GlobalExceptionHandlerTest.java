package com.omnicharge.user_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleInvalidCredentials() - returns 401 UNAUTHORIZED")
    void handleInvalidCredentials_returns401() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid username or password");

        ResponseEntity<Map<String, String>> response = handler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Invalid username or password");
    }

    @Test
    @DisplayName("handleUserAlreadyExists() - returns 409 CONFLICT")
    void handleUserAlreadyExists_returns409() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Username already exists");

        ResponseEntity<Map<String, String>> response = handler.handleUserAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Username already exists");
    }

    @Test
    @DisplayName("handleResourceNotFound() - returns 404 NOT_FOUND")
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<Map<String, String>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "User not found");
    }

    @Test
    @DisplayName("handleBadRequest() - returns 400 BAD_REQUEST")
    void handleBadRequest_returns400() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid input");
    }

    @Test
    @DisplayName("handleRuntimeException() - returns 400 BAD_REQUEST")
    void handleRuntimeException_returns400() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Something went wrong");
    }

    @Test
    @DisplayName("handleGenericException() - returns 500 INTERNAL_SERVER_ERROR with generic message")
    void handleGenericException_returns500() {
        Exception ex = new Exception("Critical internal error");

        ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error",
                "An unexpected error occurred. Please try again later.");
    }

    @Test
    @DisplayName("handleValidation() - returns 400 with field error details")
    void handleValidation_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerRequest", "email", "must be a valid email");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("email", "must be a valid email");
    }
}
