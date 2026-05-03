package com.omnicharge.recharge_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OperatorResponse DTO Tests")
class OperatorResponseTest {

    @Test
    @DisplayName("All getters and setters work correctly")
    void gettersAndSetters() {
        OperatorResponse op = new OperatorResponse();
        op.setId(1L);
        op.setName("Airtel");
        op.setStatus("ACTIVE");

        assertThat(op.getId()).isEqualTo(1L);
        assertThat(op.getName()).isEqualTo("Airtel");
        assertThat(op.getStatus()).isEqualTo("ACTIVE");
    }
}
