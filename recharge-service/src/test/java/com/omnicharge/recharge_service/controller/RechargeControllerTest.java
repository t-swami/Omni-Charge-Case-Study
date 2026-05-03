package com.omnicharge.recharge_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.recharge_service.dto.InitiateRechargeRequest;
import com.omnicharge.recharge_service.dto.RechargeRequestDto;
import com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest;
import com.omnicharge.recharge_service.exception.GlobalExceptionHandler;
import com.omnicharge.recharge_service.service.RechargeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargeController Unit Tests")
class RechargeControllerTest {

    @InjectMocks
    private RechargeController rechargeController;

    @Mock
    private RechargeService rechargeService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RechargeRequestDto testDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rechargeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDto = new RechargeRequestDto();
        testDto.setId(100L);
        testDto.setUsername("alice");
        testDto.setMobileNumber("9876543210");
        testDto.setStatus("PENDING");
    }

    private UsernamePasswordAuthenticationToken aliceAuth() {
        return new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("initiateRecharge - success")
    void initiateRecharge_success() throws Exception {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(1L);

        when(rechargeService.initiateRecharge(eq("alice"), eq("Bearer token"), any())).thenReturn(testDto);

        mockMvc.perform(post("/api/recharge/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer token")
                        .principal(aliceAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @DisplayName("cancelRecharge - success")
    void cancelRecharge_success() throws Exception {
        when(rechargeService.cancelRecharge(100L, "alice", false)).thenReturn(testDto);

        mockMvc.perform(put("/api/recharge/100/cancel")
                        .principal(aliceAuth()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getMyHistory - success")
    void getMyHistory_success() throws Exception {
        when(rechargeService.getMyRechargeHistory("alice")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/recharge/my-history")
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getAllRecharges - success")
    void getAllRecharges_success() throws Exception {
        when(rechargeService.getAllRecharges()).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/recharge/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getByStatus - success")
    void getByStatus_success() throws Exception {
        when(rechargeService.getRechargesByStatus("PENDING")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/recharge/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getByMobile - success")
    void getByMobile_success() throws Exception {
        when(rechargeService.getRechargesByMobile("9876543210")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/recharge/mobile/9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getRechargeById - success")
    void getRechargeById_success() throws Exception {
        when(rechargeService.getRechargeById(100L, "admin", true)).thenReturn(testDto);

        mockMvc.perform(get("/api/recharge/100")
                        .principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("updateRechargeStatus - success")
    void updateRechargeStatus_success() throws Exception {
        RechargeStatusUpdateRequest req = new RechargeStatusUpdateRequest();
        req.setStatus("SUCCESS");

        when(rechargeService.updateRechargeStatus(eq(100L), any())).thenReturn(testDto);

        mockMvc.perform(put("/api/recharge/update-status/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }
}
