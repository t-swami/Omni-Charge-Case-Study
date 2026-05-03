package com.omnicharge.recharge_service.messaging;

import com.omnicharge.recharge_service.dto.RechargeEventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargeEventPublisher Unit Tests")
class RechargeEventPublisherTest {

    @InjectMocks
    private RechargeEventPublisher rechargeEventPublisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("publishRechargeEvent - success")
    void publishRechargeEvent_success() {
        ReflectionTestUtils.setField(rechargeEventPublisher, "paymentQueue", "payment.queue");

        RechargeEventMessage message = new RechargeEventMessage();
        message.setRechargeId(10L);

        rechargeEventPublisher.publishRechargeEvent(message);

        verify(rabbitTemplate).convertAndSend("payment.queue", message);
    }
}
