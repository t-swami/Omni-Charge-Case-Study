package com.omnicharge.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.exception.GlobalExceptionHandler;
import com.omnicharge.payment_service.service.PaymentService;
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
@DisplayName("TransactionController Unit Tests")
class TransactionControllerTest {

    @InjectMocks
    private TransactionController transactionController;

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionDto testDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDto = new TransactionDto();
        testDto.setTransactionId("TXN12345");
        testDto.setRechargeId(100L);
        testDto.setUsername("alice");
        testDto.setMobileNumber("9876543210");
        testDto.setStatus("SUCCESS");
    }

    private UsernamePasswordAuthenticationToken aliceAuth() {
        return new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("makePayment - success")
    void makePayment_success() throws Exception {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");
        
        when(paymentService.makePayment(eq("alice"), any(), eq("Bearer dummy-token"))).thenReturn(testDto);

        mockMvc.perform(post("/api/transactions/pay")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN12345"));
    }

    @Test
    @DisplayName("getMyTransactions - success")
    void getMyTransactions_success() throws Exception {
        when(paymentService.getMyTransactions("alice")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/transactions/my-transactions")
                        .principal(aliceAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN12345"));
    }

    @Test
    @DisplayName("getByTransactionId - success")
    void getByTransactionId_success() throws Exception {
        when(paymentService.getByTransactionId("TXN12345")).thenReturn(testDto);

        mockMvc.perform(get("/api/transactions/txn/TXN12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("getByRechargeId - success")
    void getByRechargeId_success() throws Exception {
        when(paymentService.getByRechargeId(100L)).thenReturn(testDto);

        mockMvc.perform(get("/api/transactions/recharge/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("getAllTransactions - success")
    void getAllTransactions_success() throws Exception {
        when(paymentService.getAllTransactions()).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/transactions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getByStatus - success")
    void getByStatus_success() throws Exception {
        when(paymentService.getTransactionsByStatus("SUCCESS")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/transactions/status/SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @DisplayName("getByMobile - success")
    void getByMobile_success() throws Exception {
        when(paymentService.getTransactionsByMobile("9876543210")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/transactions/mobile/9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }
}
