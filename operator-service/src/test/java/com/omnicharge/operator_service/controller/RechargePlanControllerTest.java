package com.omnicharge.operator_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.exception.GlobalExceptionHandler;
import com.omnicharge.operator_service.service.RechargePlanService;
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
@DisplayName("RechargePlanController Unit Tests")
class RechargePlanControllerTest {

    @InjectMocks
    private RechargePlanController rechargePlanController;

    @Mock
    private RechargePlanService rechargePlanService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RechargePlanDto testDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rechargePlanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDto = new RechargePlanDto();
        testDto.setId(10L);
        testDto.setPlanName("Basic 149");
        testDto.setCategory("POPULAR");
    }

    @Test
    @DisplayName("addPlan - success")
    void addPlan_success() throws Exception {
        RechargePlanRequest req = new RechargePlanRequest();
        req.setPlanName("Basic 149");

        when(rechargePlanService.addPlan(any())).thenReturn(testDto);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planName").value("Basic 149"));
    }

    @Test
    @DisplayName("updatePlan - success")
    void updatePlan_success() throws Exception {
        RechargePlanRequest req = new RechargePlanRequest();
        req.setPlanName("Basic 149");

        when(rechargePlanService.updatePlan(eq(10L), any())).thenReturn(testDto);

        mockMvc.perform(put("/api/plans/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("patchPlan - success")
    void patchPlan_success() throws Exception {
        RechargePlanRequest req = new RechargePlanRequest();
        req.setPrice(199.0);

        when(rechargePlanService.patchPlan(eq(10L), any())).thenReturn(testDto);

        mockMvc.perform(patch("/api/plans/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deletePlan - success")
    void deletePlan_success() throws Exception {
        doNothing().when(rechargePlanService).deletePlan(10L);

        mockMvc.perform(delete("/api/plans/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Plan deleted successfully"));
    }

    @Test
    @DisplayName("getPlanById - success")
    void getPlanById_success() throws Exception {
        when(rechargePlanService.getPlanById(10L)).thenReturn(testDto);

        mockMvc.perform(get("/api/plans/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Basic 149"));
    }

    @Test
    @DisplayName("getAllPlans - success")
    void getAllPlans_success() throws Exception {
        when(rechargePlanService.getAllPlans()).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Basic 149"));
    }

    @Test
    @DisplayName("getPlansByOperator - success")
    void getPlansByOperator_success() throws Exception {
        when(rechargePlanService.getPlansByOperator(1L)).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/plans/operator/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Basic 149"));
    }

    @Test
    @DisplayName("getActivePlansByOperator - success")
    void getActivePlansByOperator_success() throws Exception {
        when(rechargePlanService.getActivePlansByOperator(1L)).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/plans/operator/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Basic 149"));
    }

    @Test
    @DisplayName("getPlansByCategory - success")
    void getPlansByCategory_success() throws Exception {
        when(rechargePlanService.getPlansByCategory("POPULAR")).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/plans/category/POPULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("POPULAR"));
    }
}
