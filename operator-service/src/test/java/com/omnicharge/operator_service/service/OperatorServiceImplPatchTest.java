package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.repository.OperatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorServiceImpl Patch Unit Tests")
class OperatorServiceImplPatchTest {

    @InjectMocks
    private OperatorServiceImpl operatorService;

    @Mock
    private OperatorRepository operatorRepository;

    private Operator buildOperator(Long id, String name, String status) {
        Operator op = new Operator();
        op.setId(id);
        op.setName(name);
        op.setType("MOBILE");
        op.setStatus(status);
        op.setLogoUrl("http://logo.url");
        op.setDescription("Test operator");
        return op;
    }

    // ── patchOperator() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("patchOperator() - success: only provided fields are updated")
    void patchOperator_onlyNameUpdated() {
        Operator existing = buildOperator(1L, "Airtel", "ACTIVE");
        OperatorRequest req = new OperatorRequest();
        req.setName("Airtel Updated");
        // Other fields null - should not be changed

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatorDto dto = operatorService.patchOperator(1L, req);

        assertThat(dto.getName()).isEqualTo("Airtel Updated");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE"); // unchanged
        verify(operatorRepository).save(any(Operator.class));
    }

    @Test
    @DisplayName("patchOperator() - success: status updated, others unchanged")
    void patchOperator_onlyStatusUpdated() {
        Operator existing = buildOperator(1L, "Airtel", "ACTIVE");
        OperatorRequest req = new OperatorRequest();
        req.setStatus("INACTIVE");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatorDto dto = operatorService.patchOperator(1L, req);

        assertThat(dto.getStatus()).isEqualTo("INACTIVE");
        assertThat(dto.getName()).isEqualTo("Airtel"); // unchanged
    }

    @Test
    @DisplayName("patchOperator() - success: type, logoUrl, description patched")
    void patchOperator_multipleFieldsUpdated() {
        Operator existing = buildOperator(1L, "Airtel", "ACTIVE");
        OperatorRequest req = new OperatorRequest();
        req.setType("DTH");
        req.setLogoUrl("http://new-logo.url");
        req.setDescription("Updated description");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatorDto dto = operatorService.patchOperator(1L, req);

        assertThat(dto.getType()).isEqualTo("DTH");
        assertThat(dto.getLogoUrl()).isEqualTo("http://new-logo.url");
        assertThat(dto.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("patchOperator() - fail: operator not found throws exception")
    void patchOperator_notFound_throwsException() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorService.patchOperator(99L, new OperatorRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    @Test
    @DisplayName("patchOperator() - empty request: nothing changes")
    void patchOperator_emptyRequest_noChange() {
        Operator existing = buildOperator(1L, "Airtel", "ACTIVE");
        OperatorRequest req = new OperatorRequest(); // all nulls

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatorDto dto = operatorService.patchOperator(1L, req);

        assertThat(dto.getName()).isEqualTo("Airtel");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }
}
