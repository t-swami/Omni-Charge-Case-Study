package com.omnicharge.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user_service.dto.ChangePasswordRequest;
import com.omnicharge.user_service.dto.UserDto;
import com.omnicharge.user_service.exception.GlobalExceptionHandler;
import com.omnicharge.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController standalone MockMvc test.
 * standaloneSetup() = zero Spring context, zero Security, no 401 ever.
 * Uses Principal injection via MockMvc for authenticated calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Web Layer Tests")
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("alice");
        testUserDto.setEmail("alice@test.com");
        testUserDto.setFullName("Alice");
        testUserDto.setRole("ROLE_USER");
    }

    private UsernamePasswordAuthenticationToken aliceAuth() {
        return new UsernamePasswordAuthenticationToken(
                "alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("GET /api/users/profile - Success")
    void getProfile_success() throws Exception {
        when(userService.getUserProfile("alice")).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/profile")
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));

        verify(userService, times(1)).getUserProfile("alice");
    }

    @Test
    @DisplayName("GET /api/users/all - Success")
    void getAllUsers_success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(testUserDto));

        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$.size()").value(1));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("PUT /api/users/promote/{userId} - Success")
    void promoteToAdmin_success() throws Exception {
        UserDto promotedUser = new UserDto();
        promotedUser.setId(1L);
        promotedUser.setUsername("alice");
        promotedUser.setRole("ROLE_ADMIN");

        when(userService.promoteToAdmin(1L)).thenReturn(promotedUser);

        mockMvc.perform(put("/api/users/promote/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        verify(userService, times(1)).promoteToAdmin(1L);
    }

    @Test
    @DisplayName("PUT /api/users/change-password - Success")
    void changePassword_success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userService.changePassword(eq("alice"), any(ChangePasswordRequest.class)))
                .thenReturn("Password changed successfully");

        mockMvc.perform(put("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(userService, times(1)).changePassword(eq("alice"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("GET /api/users/profile/wallet - Success")
    void getWalletBalance_success() throws Exception {
        when(userService.getWalletBalance("alice")).thenReturn(new java.math.BigDecimal("150.00"));

        mockMvc.perform(get("/api/users/profile/wallet")
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string("150.00"));

        verify(userService, times(1)).getWalletBalance("alice");
    }

    @Test
    @DisplayName("POST /api/users/profile/wallet/update - Success")
    void updateWalletBalance_success() throws Exception {
        mockMvc.perform(post("/api/users/profile/wallet/update")
                        .param("amount", "50.00")
                        .param("isTopUp", "true")
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Amount ₹50.00 added to wallet successfully"));

        verify(userService, times(1)).updateWalletBalance("alice", new java.math.BigDecimal("50.00"), true);
    }
}
