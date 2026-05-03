package com.omnicharge.recharge_service.service;

import com.omnicharge.recharge_service.dto.RechargeRequestDto;
import com.omnicharge.recharge_service.entity.RechargeRequest;
import com.omnicharge.recharge_service.entity.RechargeStatus;
import com.omnicharge.recharge_service.feign.OperatorFeignClient;
import com.omnicharge.recharge_service.messaging.RechargeEventPublisher;
import com.omnicharge.recharge_service.repository.RechargeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargeServiceImpl Extended Unit Tests")
class RechargeServiceImplExtendedTest {

    @InjectMocks
    private RechargeServiceImpl rechargeService;

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private OperatorFeignClient operatorFeignClient;

    @Mock
    private RechargeEventPublisher rechargeEventPublisher;

    private RechargeRequest buildRecharge(Long id, String username, RechargeStatus status) {
        RechargeRequest r = new RechargeRequest();
        r.setId(id);
        r.setUsername(username);
        r.setMobileNumber("9876543210");
        r.setOperatorId(1L);
        r.setOperatorName("Airtel");
        r.setPlanId(10L);
        r.setPlanName("Basic 149");
        r.setAmount(149.0);
        r.setValidity("28 days");
        r.setDataInfo("1.5 GB/day");
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ── cancelRecharge() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelRecharge() - success: owner cancels own PENDING recharge")
    void cancelRecharge_ownerCancelsPending_success() {
        RechargeRequest pending = buildRecharge(1L, "alice", RechargeStatus.PENDING);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeRequestDto dto = rechargeService.cancelRecharge(1L, "alice", false);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        assertThat(dto.getFailureReason()).contains("Cancelled");
    }

    @Test
    @DisplayName("cancelRecharge() - success: admin cancels any recharge")
    void cancelRecharge_adminCancelsPending_success() {
        RechargeRequest pending = buildRecharge(1L, "alice", RechargeStatus.PENDING);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeRequestDto dto = rechargeService.cancelRecharge(1L, "admin", true);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        assertThat(dto.getFailureReason()).contains("Cancelled");
    }

    @Test
    @DisplayName("cancelRecharge() - fail: user cannot cancel another user's recharge")
    void cancelRecharge_userCancelsOthers_throwsException() {
        RechargeRequest pending = buildRecharge(1L, "alice", RechargeStatus.PENDING);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> rechargeService.cancelRecharge(1L, "bob", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("cancelRecharge() - fail: cannot cancel SUCCESS recharge")
    void cancelRecharge_alreadySuccess_throwsException() {
        RechargeRequest success = buildRecharge(1L, "alice", RechargeStatus.SUCCESS);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(success));

        assertThatThrownBy(() -> rechargeService.cancelRecharge(1L, "alice", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel recharge");
    }

    @Test
    @DisplayName("cancelRecharge() - fail: cannot cancel FAILED recharge")
    void cancelRecharge_alreadyFailed_throwsException() {
        RechargeRequest failed = buildRecharge(1L, "alice", RechargeStatus.FAILED);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(failed));

        assertThatThrownBy(() -> rechargeService.cancelRecharge(1L, "alice", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel recharge");
    }

    @Test
    @DisplayName("cancelRecharge() - fail: recharge not found throws exception")
    void cancelRecharge_notFound_throwsException() {
        when(rechargeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.cancelRecharge(999L, "alice", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Recharge not found");
    }

    // ── getAllRecharges() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllRecharges() - returns all recharges ordered by date")
    void getAllRecharges_returnsList() {
        when(rechargeRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(
                        buildRecharge(1L, "alice", RechargeStatus.PENDING),
                        buildRecharge(2L, "bob", RechargeStatus.SUCCESS)
                ));

        List<RechargeRequestDto> result = rechargeService.getAllRecharges();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getAllRecharges() - returns empty list when no recharges")
    void getAllRecharges_emptyList() {
        when(rechargeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<RechargeRequestDto> result = rechargeService.getAllRecharges();

        assertThat(result).isEmpty();
    }

    // ── getRechargesByMobile() ────────────────────────────────────────────────

    @Test
    @DisplayName("getRechargesByMobile() - returns recharges for mobile number")
    void getRechargesByMobile_returnsList() {
        when(rechargeRepository.findByMobileNumber("9876543210"))
                .thenReturn(List.of(buildRecharge(1L, "alice", RechargeStatus.SUCCESS)));

        List<RechargeRequestDto> result = rechargeService.getRechargesByMobile("9876543210");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMobileNumber()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("getRechargesByMobile() - unknown mobile returns empty list")
    void getRechargesByMobile_emptyList() {
        when(rechargeRepository.findByMobileNumber("0000000000")).thenReturn(List.of());

        List<RechargeRequestDto> result = rechargeService.getRechargesByMobile("0000000000");

        assertThat(result).isEmpty();
    }

    // ── initiateRecharge() - feign exception for plan ─────────────────────────

    @Test
    @DisplayName("initiateRecharge() - feign exception for plan: throws RuntimeException")
    void initiateRecharge_feignExceptionForPlan_throwsException() {
        com.omnicharge.recharge_service.dto.InitiateRechargeRequest req
                = new com.omnicharge.recharge_service.dto.InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(99L);

        com.omnicharge.recharge_service.dto.OperatorResponse op
                = new com.omnicharge.recharge_service.dto.OperatorResponse();
        op.setId(1L);
        op.setName("Airtel");
        op.setStatus("ACTIVE");

        when(operatorFeignClient.getOperatorById(1L, "Bearer token")).thenReturn(op);
        when(operatorFeignClient.getPlanById(99L, "Bearer token"))
                .thenThrow(new RuntimeException("Plan not found"));

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan not found");
    }

    // ── updateRechargeStatus() - null failure reason ───────────────────────────

    @Test
    @DisplayName("updateRechargeStatus() - null failureReason: status updated, no reason set")
    void updateRechargeStatus_nullFailureReason_success() {
        RechargeRequest existing = buildRecharge(1L, "alice", RechargeStatus.PENDING);
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest req
                = new com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest();
        req.setStatus("SUCCESS");
        req.setFailureReason(null);

        RechargeRequestDto dto = rechargeService.updateRechargeStatus(1L, req);

        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getFailureReason()).isNull();
    }
}
