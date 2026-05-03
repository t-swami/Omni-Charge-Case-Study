package com.omnicharge.payment_service.messaging;

import com.omnicharge.payment_service.dto.RechargeEventMessage;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargeEventConsumer Unit Tests")
class RechargeEventConsumerTest {

    @InjectMocks
    private RechargeEventConsumer rechargeEventConsumer;

    @Mock
    private PaymentService paymentService;

    private RechargeEventMessage buildEvent(Long rechargeId) {
        RechargeEventMessage event = new RechargeEventMessage();
        event.setRechargeId(rechargeId);
        event.setUsername("alice");
        event.setMobileNumber("9876543210");
        event.setOperatorName("Airtel");
        event.setPlanName("Basic 149");
        event.setAmount(149.0);
        event.setValidity("28 days");
        event.setDataInfo("1.5 GB/day");
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private TransactionDto buildTransactionDto(String status, String failureReason) {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionId("TXN12345ABCDE");
        dto.setRechargeId(100L);
        dto.setStatus(status);
        dto.setFailureReason(failureReason);
        return dto;
    }

    @Test
    @DisplayName("consumeRechargeEvent() - successful payment: logs result without exception")
    void consumeRechargeEvent_success_noException() {
        RechargeEventMessage event = buildEvent(100L);
        TransactionDto successDto = buildTransactionDto("PENDING", null);
        when(paymentService.processPayment(any(RechargeEventMessage.class))).thenReturn(successDto);

        assertThatNoException().isThrownBy(() ->
                rechargeEventConsumer.consumeRechargeEvent(event));

        verify(paymentService).processPayment(any(RechargeEventMessage.class));
    }

    @Test
    @DisplayName("consumeRechargeEvent() - failed payment: logs failure reason without exception")
    void consumeRechargeEvent_failed_noException() {
        RechargeEventMessage event = buildEvent(101L);
        TransactionDto failedDto = buildTransactionDto("FAILED", "Insufficient funds");
        when(paymentService.processPayment(any(RechargeEventMessage.class))).thenReturn(failedDto);

        assertThatNoException().isThrownBy(() ->
                rechargeEventConsumer.consumeRechargeEvent(event));
    }

    @Test
    @DisplayName("consumeRechargeEvent() - payment service called with the event")
    void consumeRechargeEvent_callsPaymentService() {
        RechargeEventMessage event = buildEvent(102L);
        TransactionDto dto = buildTransactionDto("PENDING", null);
        when(paymentService.processPayment(event)).thenReturn(dto);

        rechargeEventConsumer.consumeRechargeEvent(event);

        verify(paymentService, times(1)).processPayment(event);
    }
}
