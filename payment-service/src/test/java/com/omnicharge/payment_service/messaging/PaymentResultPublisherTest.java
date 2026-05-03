package com.omnicharge.payment_service.messaging;

import com.omnicharge.payment_service.dto.PaymentResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentResultPublisherTest {

    @InjectMocks
    private PaymentResultPublisher paymentResultPublisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentResultPublisher, "notificationQueue", "notification.queue");
    }

    @Test
    void publishPaymentResult_success() {
        PaymentResultMessage message = new PaymentResultMessage();
        message.setUserEmail("test@example.com");
        
        paymentResultPublisher.publishPaymentResult(message);
        
        verify(rabbitTemplate).convertAndSend("notification.queue", message);
    }
}
