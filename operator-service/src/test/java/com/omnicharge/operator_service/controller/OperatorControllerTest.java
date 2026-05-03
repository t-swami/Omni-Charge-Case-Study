package com.omnicharge.operator_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.exception.GlobalExceptionHandler;
import com.omnicharge.operator_service.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorController Unit Tests")
class OperatorControllerTest {

    @InjectMocks
    private OperatorController operatorController;

    @Mock
    private OperatorService operatorService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OperatorDto testDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operatorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDto = new OperatorDto();
        testDto.setId(1L);
        testDto.setName("Airtel");
        testDto.setType("MOBILE");
        testDto.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("addOperator - success")
    void addOperator_success() throws Exception {
        OperatorRequest req = new OperatorRequest();
        req.setName("Airtel");

        when(operatorService.addOperator(any())).thenReturn(testDto);

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Airtel"));
    }

    @Test
    @DisplayName("updateOperator - success")
    void updateOperator_success() throws Exception {
        OperatorRequest req = new OperatorRequest();
        req.setName("Airtel");

        when(operatorService.updateOperator(eq(1L), any())).thenReturn(testDto);

        mockMvc.perform(put("/api/operators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("patchOperator - success")
    void patchOperator_success() throws Exception {
        OperatorRequest req = new OperatorRequest();
        req.setStatus("INACTIVE");

        when(operatorService.patchOperator(eq(1L), any())).thenReturn(testDto);

        mockMvc.perform(patch("/api/operators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteOperator - success")
    void deleteOperator_success() throws Exception {
        doNothing().when(operatorService).deleteOperator(1L);

        mockMvc.perform(delete("/api/operators/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Operator deleted successfully"));
    }

    @Test
    @DisplayName("getOperatorById - success")
    void getOperatorById_success() throws Exception {
        when(operatorService.getOperatorById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/api/operators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Airtel"));
    }

    @Test
    @DisplayName("getAllOperators - success")
    void getAllOperators_success() throws Exception {
        when(operatorService.getAllOperators()).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/operators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Airtel"));
    }

    @Test
    @DisplayName("getOperatorsByStatus - success")
    void getOperatorsByStatus_success() throws Exception {
        when(operatorService.getOperatorsByStatus("ACTIVE")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/operators/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Airtel"));
    }

    @Test
    @DisplayName("getOperatorsByType - success")
    void getOperatorsByType_success() throws Exception {
        when(operatorService.getOperatorsByType("MOBILE")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/operators/type/MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Airtel"));
    }
}
