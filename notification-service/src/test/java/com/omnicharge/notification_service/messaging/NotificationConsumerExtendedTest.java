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

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Extended Unit Tests")
class NotificationConsumerExtendedTest {

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Mock
    private EmailService emailService;

    private PaymentResultMessage buildMessage(String status) {
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
        msg.setProcessedAt(LocalDateTime.now());
        return msg;
    }

    // ── REFUND_PENDING notifications ──────────────────────────────────────────

    @Test
    @DisplayName("consumePaymentResult() - REFUND_PENDING message sends refund email")
    void consume_refundPendingMessage_noException() {
        PaymentResultMessage msg = buildMessage("REFUND_PENDING");
        msg.setPaymentReference("REF-UPI-ALICE-12345");
        msg.setFailureReason("Recharge service unavailable. Refund in 5-7 business days.");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));

        verify(emailService).sendEmail(eq("alice@example.com"),
                eq("Refund Initiated — OmniCharge"), anyString());
    }

    @Test
    @DisplayName("consumePaymentResult() - REFUND_PENDING with null paymentReference handled gracefully")
    void consume_refundPendingNullReference_noException() {
        PaymentResultMessage msg = buildMessage("REFUND_PENDING");
        msg.setPaymentReference(null);
        msg.setFailureReason("Service disruption refund");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - unknown status falls to default: no exception")
    void consume_unknownStatus_noException() {
        PaymentResultMessage msg = buildMessage("UNKNOWN_STATUS");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - SUCCESS message with payment reference sends email")
    void consume_successWithReference_noException() {
        PaymentResultMessage msg = buildMessage("SUCCESS");
        msg.setPaymentReference("REF-CARD-9999-ABCDEF");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));

        verify(emailService).sendEmail(eq("alice@example.com"),
                eq("Recharge Successful — OmniCharge"), anyString());
    }

    @Test
    @DisplayName("consumePaymentResult() - FAILED message with null mobile still works")
    void consume_failedNullMobile_noException() {
        PaymentResultMessage msg = buildMessage("FAILED");
        msg.setFailureReason("Insufficient funds");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }
}
