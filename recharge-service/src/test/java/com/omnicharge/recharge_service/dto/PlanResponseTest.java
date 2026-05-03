package com.omnicharge.recharge_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanResponse DTO Tests")
class PlanResponseTest {

    @Test
    @DisplayName("All getters and setters work correctly")
    void gettersAndSetters() {
        PlanResponse plan = new PlanResponse();
        plan.setId(10L);
        plan.setPlanName("Basic 149");
        plan.setPrice(149.0);
        plan.setValidity("28 days");
        plan.setData("1.5 GB/day");
        plan.setOperatorId(1L);
        plan.setStatus("ACTIVE");

        assertThat(plan.getId()).isEqualTo(10L);
        assertThat(plan.getPlanName()).isEqualTo("Basic 149");
        assertThat(plan.getPrice()).isEqualTo(149.0);
        assertThat(plan.getValidity()).isEqualTo("28 days");
        assertThat(plan.getData()).isEqualTo("1.5 GB/day");
        assertThat(plan.getOperatorId()).isEqualTo(1L);
        assertThat(plan.getStatus()).isEqualTo("ACTIVE");
    }
}
