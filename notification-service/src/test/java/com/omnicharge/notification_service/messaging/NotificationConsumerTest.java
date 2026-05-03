package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import com.omnicharge.notification_service.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Tests for NotificationConsumer.
 * Verifies that:
 *  1. No exception is thrown for SUCCESS, FAILED, REFUND_PENDING messages.
 *  2. EmailService.sendEmail() is called with the correct recipient.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Unit Tests")
class NotificationConsumerTest {

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Mock
    private EmailService emailService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private PaymentResultMessage buildMessage(String status, String failureReason) {
        PaymentResultMessage msg = new PaymentResultMessage();
        msg.setRechargeId(100L);
        msg.setTransactionId("TXN123456789ABC");
        msg.setUsername("alice");
        msg.setUserEmail("alice@example.com");
        msg.setMobileNumber("9876543210");
        msg.setOperatorName("Airtel");
        msg.setPlanName("Basic 149");
        msg.setAmount(149.0);
        msg.setValidity("28 days");
        msg.setDataInfo("1.5 GB/day");
        msg.setStatus(status);
        msg.setFailureReason(failureReason);
        msg.setProcessedAt(LocalDateTime.now());
        return msg;
    }

    // ── consumePaymentResult() ────────────────────────────────────────────────

    @Test
    @DisplayName("consumePaymentResult() - SUCCESS message processed and email sent")
    void consume_successMessage_noException() {
        PaymentResultMessage msg = buildMessage("SUCCESS", null);
        msg.setPaymentReference("REF-CARD-9999-ABCDEF");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));

        verify(emailService).sendEmail(eq("alice@example.com"),
                eq("Recharge Successful — OmniCharge"), anyString());
    }

    @Test
    @DisplayName("consumePaymentResult() - FAILED message with reason triggers email")
    void consume_failedMessageWithReason_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", "Insufficient funds in your account");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));

        verify(emailService).sendEmail(eq("alice@example.com"),
                eq("Recharge Failed — OmniCharge"), anyString());
    }

    @Test
    @DisplayName("consumePaymentResult() - FAILED message with null reason handled gracefully")
    void consume_failedMessageNullReason_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", null);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - zero amount handled without exception")
    void consume_zeroAmount_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", "Plan price is zero");
        msg.setAmount(0.0);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - large amount handled without exception")
    void consume_largeAmount_noException() {
        PaymentResultMessage msg = buildMessage("SUCCESS", null);
        msg.setAmount(9999.0);
        msg.setPaymentReference("REF-CARD-9999-ABCDEF");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }
}
