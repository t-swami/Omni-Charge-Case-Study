package com.omnicharge.notification_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification RechargeEventMessage DTO Tests")
class RechargeEventMessageTest {

    @Test
    @DisplayName("All getters and setters work correctly")
    void gettersAndSetters() {
        RechargeEventMessage msg = new RechargeEventMessage();
        LocalDateTime now = LocalDateTime.now();

        msg.setRechargeId(1L);
        msg.setUsername("alice");
        msg.setMobileNumber("9876543210");
        msg.setOperatorName("Airtel");
        msg.setPlanName("Basic 149");
        msg.setAmount(149.0);
        msg.setValidity("28 days");
        msg.setDataInfo("1.5 GB/day");
        msg.setStatus("PENDING");
        msg.setCreatedAt(now);

        assertThat(msg.getRechargeId()).isEqualTo(1L);
        assertThat(msg.getUsername()).isEqualTo("alice");
        assertThat(msg.getMobileNumber()).isEqualTo("9876543210");
        assertThat(msg.getOperatorName()).isEqualTo("Airtel");
        assertThat(msg.getPlanName()).isEqualTo("Basic 149");
        assertThat(msg.getAmount()).isEqualTo(149.0);
        assertThat(msg.getValidity()).isEqualTo("28 days");
        assertThat(msg.getDataInfo()).isEqualTo("1.5 GB/day");
        assertThat(msg.getStatus()).isEqualTo("PENDING");
        assertThat(msg.getCreatedAt()).isEqualTo(now);
    }
}
