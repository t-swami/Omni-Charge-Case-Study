package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.*;
import com.omnicharge.payment_service.entity.Transaction;
import com.omnicharge.payment_service.entity.TransactionStatus;
import com.omnicharge.payment_service.feign.RechargeServiceFeignClient;
import com.omnicharge.payment_service.feign.UserServiceFeignClient;
import com.omnicharge.payment_service.messaging.PaymentResultPublisher;
import com.omnicharge.payment_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Extended Unit Tests")
class PaymentServiceImplExtendedTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RechargeServiceFeignClient rechargeServiceFeignClient;

    @Mock
    private PaymentResultPublisher paymentResultPublisher;

    @Mock
    private DummyPaymentGatewayService paymentGatewayService;

    @Mock
    private UserServiceFeignClient userServiceFeignClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "maxRetryAttempts", 1);
        ReflectionTestUtils.setField(paymentService, "retryIntervalSeconds", 0L);
        ReflectionTestUtils.setField(paymentService, "refundEta", "5-7 business days");
        ReflectionTestUtils.setField(paymentService, "transactionIdPrefix", "TXN");
    }

    private Transaction buildSuccessTransaction(Long rechargeId) {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setTransactionId("TXN123456789");
        t.setRechargeId(rechargeId);
        t.setUsername("alice");
        t.setMobileNumber("9876543210");
        t.setOperatorName("Airtel");
        t.setPlanName("Basic 149");
        t.setAmount(149.0);
        t.setValidity("28 days");
        t.setDataInfo("1.5 GB/day");
        t.setStatus(TransactionStatus.SUCCESS);
        t.setPaymentReference("REF-UPI-12345");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    private Transaction buildPendingTransaction(Long rechargeId) {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setTransactionId("TXN123456789");
        t.setRechargeId(rechargeId);
        t.setUsername("alice");
        t.setMobileNumber("9876543210");
        t.setOperatorName("Airtel");
        t.setPlanName("Basic 149");
        t.setAmount(149.0);
        t.setValidity("28 days");
        t.setDataInfo("1.5 GB/day");
        t.setStatus(TransactionStatus.PENDING);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    // ── getByRechargeId() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getByRechargeId() - found: returns DTO for recharge")
    void getByRechargeId_found() {
        Transaction t = buildSuccessTransaction(200L);
        when(transactionRepository.findByRechargeId(200L)).thenReturn(Optional.of(t));

        TransactionDto dto = paymentService.getByRechargeId(200L);

        assertThat(dto.getRechargeId()).isEqualTo(200L);
        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getByRechargeId() - not found: throws RuntimeException")
    void getByRechargeId_notFound_throwsException() {
        when(transactionRepository.findByRechargeId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByRechargeId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found for recharge id");
    }

    // ── getAllTransactions() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTransactions() - returns all transactions ordered by date")
    void getAllTransactions_returnsList() {
        Transaction t1 = buildSuccessTransaction(100L);
        Transaction t2 = buildPendingTransaction(101L);
        when(transactionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(t1, t2));

        List<TransactionDto> result = paymentService.getAllTransactions();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(result.get(1).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getAllTransactions() - returns empty list when no transactions")
    void getAllTransactions_emptyList() {
        when(transactionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<TransactionDto> result = paymentService.getAllTransactions();

        assertThat(result).isEmpty();
    }

    // ── getTransactionsByMobile() ─────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionsByMobile() - returns transactions for mobile number")
    void getTransactionsByMobile_returnsList() {
        Transaction t = buildSuccessTransaction(100L);
        when(transactionRepository.findByMobileNumber("9876543210")).thenReturn(List.of(t));

        List<TransactionDto> result = paymentService.getTransactionsByMobile("9876543210");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMobileNumber()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("getTransactionsByMobile() - returns empty list for unknown mobile")
    void getTransactionsByMobile_emptyList() {
        when(transactionRepository.findByMobileNumber("0000000000")).thenReturn(List.of());

        List<TransactionDto> result = paymentService.getTransactionsByMobile("0000000000");

        assertThat(result).isEmpty();
    }

    // ── updateRechargeStatusWithRetry() - retry exhaustion with SUCCESS ────────

    @Test
    @DisplayName("updateRechargeStatusWithRetry() - retry exhaustion with SUCCESS triggers REFUND_PENDING")
    void updateRechargeStatusWithRetry_allFail_successTransaction_triggersRefund() throws Exception {
        Transaction successTransaction = buildSuccessTransaction(100L);

        // findById called multiple times (first in retry, then in handleRetryExhaustion)
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(successTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Feign call fails all retries
        doThrow(new RuntimeException("Recharge service down"))
                .when(rechargeServiceFeignClient).updateRechargeStatus(anyLong(), any());

        // Call the async method (run synchronously in test via direct invocation)
        paymentService.updateRechargeStatusWithRetry(100L, 1L, "dummy-token");

        // Verify refund notification was published
        verify(paymentResultPublisher, atLeastOnce()).publishPaymentResult(argThat(
                msg -> "REFUND_PENDING".equals(msg.getStatus())
        ));
        // Verify transaction was saved with REFUND_PENDING status
        verify(transactionRepository, atLeastOnce()).save(argThat(
                t -> t.getStatus() == TransactionStatus.REFUND_PENDING
        ));
    }

    @Test
    @DisplayName("updateRechargeStatusWithRetry() - retry exhaustion with FAILED transaction: no refund")
    void updateRechargeStatusWithRetry_allFail_failedTransaction_noRefund() throws Exception {
        Transaction failedTransaction = buildPendingTransaction(100L);
        failedTransaction.setStatus(TransactionStatus.FAILED);
        failedTransaction.setFailureReason("Card declined");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(failedTransaction));

        // Feign call fails
        doThrow(new RuntimeException("Recharge service down"))
                .when(rechargeServiceFeignClient).updateRechargeStatus(anyLong(), any());

        paymentService.updateRechargeStatusWithRetry(100L, 1L, "dummy-token");

        // No refund notification for FAILED payment
        verify(paymentResultPublisher, never()).publishPaymentResult(argThat(
                msg -> "REFUND_PENDING".equals(msg.getStatus())
        ));
    }

    @Test
    @DisplayName("updateRechargeStatusWithRetry() - transaction not found: logs error and returns")
    void updateRechargeStatusWithRetry_transactionNotFound_logsAndReturns() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // Should not throw, just log and return
        assertThatNoException().isThrownBy(() ->
                paymentService.updateRechargeStatusWithRetry(100L, 999L, "dummy-token"));
    }

    // ── makePayment() with null payment method ─────────────────────────────────

    @Test
    @DisplayName("makePayment() - FAILED payment result: recharge update still attempted")
    void makePayment_failedGateway_rechargeStillUpdated() {
        Transaction pending = buildPendingTransaction(100L);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("CARD");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(false);
        gatewayResp.setStatus("FAILED");
        gatewayResp.setFailureReason("Card declined");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(paymentGatewayService.processPayment(any(), eq(149.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = paymentService.makePayment("alice", req, "dummy-token");

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        // Notification is published even for FAILED payments
        verify(paymentResultPublisher, atLeastOnce()).publishPaymentResult(any());
    }

    // ── publishNotification exception handling ─────────────────────────────────

    // ── processPayment() ──────────────────────────────────────────────────────
    
    @Test
    @DisplayName("processPayment() - duplicate event: returns existing transaction")
    void processPayment_duplicateEvent_returnsExisting() {
        RechargeEventMessage event = new RechargeEventMessage();
        event.setRechargeId(100L);
        Transaction existing = buildPendingTransaction(100L);
        
        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(existing));
        
        TransactionDto dto = paymentService.processPayment(event);
        
        assertThat(dto.getTransactionId()).isEqualTo(existing.getTransactionId());
        verify(transactionRepository, never()).save(any());
    }

    // ── WALLET Payments ───────────────────────────────────────────────────────

    @Test
    @DisplayName("makePayment() - WALLET success: balance checked and deducted")
    void makePayment_walletSuccess() {
        Transaction pending = buildPendingTransaction(100L);
        pending.setAmount(100.0);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("WALLET");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(true);
        gatewayResp.setPaymentReference("REF-WALLET-123");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(userServiceFeignClient.getWalletBalance("dummy-token")).thenReturn(new java.math.BigDecimal("200.00"));
        when(paymentGatewayService.processPayment(any(), eq(100.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.makePayment("alice", req, "dummy-token");

        verify(userServiceFeignClient).updateWalletBalance("dummy-token", new java.math.BigDecimal("100.0"), false);
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    @DisplayName("makePayment() - WALLET fail: insufficient balance")
    void makePayment_walletInsufficientBalance() {
        Transaction pending = buildPendingTransaction(100L);
        pending.setAmount(100.0);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("WALLET");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(userServiceFeignClient.getWalletBalance("dummy-token")).thenReturn(new java.math.BigDecimal("50.00"));

        assertThatThrownBy(() -> paymentService.makePayment("alice", req, "dummy-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient OmniCharge Wallet balance");
    }

    @Test
    @DisplayName("makePayment() - WALLET fail: balance check exception")
    void makePayment_walletBalanceCheckError() {
        Transaction pending = buildPendingTransaction(100L);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("WALLET");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(userServiceFeignClient.getWalletBalance("dummy-token")).thenThrow(new RuntimeException("Service down"));

        assertThatThrownBy(() -> paymentService.makePayment("alice", req, "dummy-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not verify wallet balance");
    }

    // ── Wallet TOP-UP ────────────────────────────────────────────────────────

    @Test
    @DisplayName("topUpWallet() - success: balance updated")
    void topUpWallet_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setAmount(500.0);
        req.setPaymentMethod("UPI");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(true);
        gatewayResp.setPaymentReference("REF-TOPUP-123");

        when(paymentGatewayService.processPayment(any(), eq(500.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = paymentService.topUpWallet("alice", req, "dummy-token");

        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        verify(userServiceFeignClient).updateWalletBalance("dummy-token", new java.math.BigDecimal("500.0"), true);
    }

    @Test
    @DisplayName("topUpWallet() - gateway fail: transaction marked FAILED")
    void topUpWallet_gatewayFail() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setAmount(500.0);
        req.setPaymentMethod("UPI");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(false);
        gatewayResp.setFailureReason("Declined");

        when(paymentGatewayService.processPayment(any(), eq(500.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = paymentService.topUpWallet("alice", req, "dummy-token");

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        verify(userServiceFeignClient, never()).updateWalletBalance(anyString(), any(), anyBoolean());
    }

    @Test
    @DisplayName("topUpWallet() - balance update fail: throws exception")
    void topUpWallet_balanceUpdateFail_throwsException() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setAmount(500.0);
        req.setPaymentMethod("UPI");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(true);

        when(paymentGatewayService.processPayment(any(), eq(500.0))).thenReturn(gatewayResp);
        doThrow(new RuntimeException("Service down")).when(userServiceFeignClient).updateWalletBalance(anyString(), any(), anyBoolean());

        assertThatThrownBy(() -> paymentService.topUpWallet("alice", req, "dummy-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("failed to update wallet balance");
    }

    // ── publishNotification exception handling ─────────────────────────────────

    @Test
    @DisplayName("makePayment() - notification publish failure: does not throw exception")
    void makePayment_notificationPublishFails_noException() {
        Transaction pending = buildPendingTransaction(100L);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");
        req.setUpiId("alice@upi");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(true);
        gatewayResp.setStatus("SUCCESS");
        gatewayResp.setPaymentReference("REF-UPI-ABCDEF");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(paymentGatewayService.processPayment(any(), eq(149.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("RabbitMQ down")).when(paymentResultPublisher).publishPaymentResult(any());

        // Should not throw - notification failures are caught internally
        assertThatNoException().isThrownBy(() ->
                paymentService.makePayment("alice", req, "dummy-token"));
    }
}
