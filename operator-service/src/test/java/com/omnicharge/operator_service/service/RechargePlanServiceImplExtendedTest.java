package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.entity.RechargePlan;
import com.omnicharge.operator_service.repository.OperatorRepository;
import com.omnicharge.operator_service.repository.RechargePlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargePlanServiceImpl Extended Unit Tests")
class RechargePlanServiceImplExtendedTest {

    @InjectMocks
    private RechargePlanServiceImpl rechargePlanService;

    @Mock
    private RechargePlanRepository rechargePlanRepository;

    @Mock
    private OperatorRepository operatorRepository;

    private Operator buildOperator(Long id) {
        Operator op = new Operator();
        op.setId(id);
        op.setName("Airtel");
        op.setType("MOBILE");
        op.setStatus("ACTIVE");
        return op;
    }

    private RechargePlan buildPlan(Long id, Operator op, String status) {
        RechargePlan plan = new RechargePlan();
        plan.setId(id);
        plan.setPlanName("Basic 149");
        plan.setPrice(149.0);
        plan.setValidity("28 days");
        plan.setData("1.5 GB/day");
        plan.setCalls("Unlimited");
        plan.setSms("100/day");
        plan.setDescription("Popular plan");
        plan.setCategory("POPULAR");
        plan.setStatus(status);
        plan.setOperator(op);
        return plan;
    }

    // ── getAllPlans() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPlans() - returns all plans as DTOs")
    void getAllPlans_returnsList() {
        Operator op = buildOperator(1L);
        when(rechargePlanRepository.findAll()).thenReturn(List.of(
                buildPlan(10L, op, "ACTIVE"),
                buildPlan(11L, op, "INACTIVE")
        ));

        List<RechargePlanDto> result = rechargePlanService.getAllPlans();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlanName()).isEqualTo("Basic 149");
    }

    @Test
    @DisplayName("getAllPlans() - returns empty list when no plans exist")
    void getAllPlans_emptyList() {
        when(rechargePlanRepository.findAll()).thenReturn(List.of());

        List<RechargePlanDto> result = rechargePlanService.getAllPlans();

        assertThat(result).isEmpty();
    }

    // ── getPlansByCategory() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getPlansByCategory() - returns plans in given category")
    void getPlansByCategory_returnsList() {
        Operator op = buildOperator(1L);
        when(rechargePlanRepository.findByCategory("POPULAR")).thenReturn(List.of(
                buildPlan(10L, op, "ACTIVE")
        ));

        List<RechargePlanDto> result = rechargePlanService.getPlansByCategory("POPULAR");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("POPULAR");
    }

    @Test
    @DisplayName("getPlansByCategory() - returns empty list for unknown category")
    void getPlansByCategory_emptyList() {
        when(rechargePlanRepository.findByCategory("UNKNOWN")).thenReturn(List.of());

        List<RechargePlanDto> result = rechargePlanService.getPlansByCategory("UNKNOWN");

        assertThat(result).isEmpty();
    }

    // ── getPlanById() - not found ─────────────────────────────────────────────

    @Test
    @DisplayName("getPlanById() - fail: plan not found throws exception")
    void getPlanById_notFound_throwsException() {
        when(rechargePlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.getPlanById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan not found");
    }

    // ── patchPlan() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchPlan() - success: only provided fields are updated")
    void patchPlan_onlyPriceUpdated() {
        Operator op = buildOperator(1L);
        RechargePlan existing = buildPlan(10L, op, "ACTIVE");
        RechargePlanRequest req = new RechargePlanRequest();
        req.setPrice(199.0);
        // No other fields - should be unchanged

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(rechargePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargePlanDto dto = rechargePlanService.patchPlan(10L, req);

        assertThat(dto.getPrice()).isEqualTo(199.0);
        assertThat(dto.getPlanName()).isEqualTo("Basic 149"); // unchanged
        verify(rechargePlanRepository).save(any(RechargePlan.class));
    }

    @Test
    @DisplayName("patchPlan() - success: plan name, validity, data updated")
    void patchPlan_multipleFieldsUpdated() {
        Operator op = buildOperator(1L);
        RechargePlan existing = buildPlan(10L, op, "ACTIVE");
        RechargePlanRequest req = new RechargePlanRequest();
        req.setPlanName("Super 299");
        req.setValidity("56 days");
        req.setData("2 GB/day");
        req.setCalls("Unlimited");
        req.setSms("50/day");
        req.setDescription("Premium plan");
        req.setCategory("PREMIUM");
        req.setStatus("ACTIVE");

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(rechargePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargePlanDto dto = rechargePlanService.patchPlan(10L, req);

        assertThat(dto.getPlanName()).isEqualTo("Super 299");
        assertThat(dto.getValidity()).isEqualTo("56 days");
        assertThat(dto.getCategory()).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("patchPlan() - success: operator updated via patch")
    void patchPlan_operatorUpdated() {
        Operator op1 = buildOperator(1L);
        Operator op2 = buildOperator(2L);
        op2.setName("Jio");

        RechargePlan existing = buildPlan(10L, op1, "ACTIVE");
        RechargePlanRequest req = new RechargePlanRequest();
        req.setOperatorId(2L); // change operator

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(op2));
        when(rechargePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargePlanDto dto = rechargePlanService.patchPlan(10L, req);

        assertThat(dto.getOperatorId()).isEqualTo(2L);
        assertThat(dto.getOperatorName()).isEqualTo("Jio");
    }

    @Test
    @DisplayName("patchPlan() - fail: plan not found throws exception")
    void patchPlan_notFound_throwsException() {
        when(rechargePlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.patchPlan(99L, new RechargePlanRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan not found");
    }

    @Test
    @DisplayName("patchPlan() - fail: operator not found during patch throws exception")
    void patchPlan_operatorNotFound_throwsException() {
        Operator op = buildOperator(1L);
        RechargePlan existing = buildPlan(10L, op, "ACTIVE");
        RechargePlanRequest req = new RechargePlanRequest();
        req.setOperatorId(99L); // invalid operator

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.patchPlan(10L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── updatePlan() - operator not found ─────────────────────────────────────

    @Test
    @DisplayName("updatePlan() - fail: operator not found for update throws exception")
    void updatePlan_operatorNotFound_throwsException() {
        Operator op = buildOperator(1L);
        RechargePlan existing = buildPlan(10L, op, "ACTIVE");
        RechargePlanRequest req = new RechargePlanRequest();
        req.setOperatorId(99L); // invalid
        req.setPlanName("New Plan");

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.updatePlan(10L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }
}
