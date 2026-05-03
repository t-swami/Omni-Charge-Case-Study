package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.PaymentGatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DummyPaymentGatewayService Unit Tests")
class DummyPaymentGatewayServiceTest {

    private final DummyPaymentGatewayService gateway = new DummyPaymentGatewayService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "validBanksConfig",
                "SBI,HDFC,ICICI,AXIS,KOTAK,BOB,PNB,CANARA,UNION,INDUSIND");
    }

    // ── CARD ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CARD - success: valid card details return SUCCESS")
    void card_validDetails_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        assertThat(resp.getPaymentReference()).isNotBlank();
    }

    @Test
    @DisplayName("CARD - success: any valid 16-digit card number succeeds")
    void card_anyValidNumber_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111110000");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("CARD - fail: card number not 16 digits → invalid")
    void card_invalidCardNumber_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("1234");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid card number");
    }

    @Test
    @DisplayName("CARD - fail: invalid expiry format → failure")
    void card_invalidExpiry_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("13/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid card expiry");
    }

    @Test
    @DisplayName("CARD - fail: CVV not 3 digits → failure")
    void card_invalidCvv_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("12/26");
        req.setCardCvv("12");
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid CVV");
    }

    @Test
    @DisplayName("CARD - fail: missing card holder name → failure")
    void card_missingHolderName_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Card holder name is required");
    }

    // ── UPI ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UPI - success: valid UPI id succeeds")
    void upi_valid_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("alice@okicici");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("UPI - success: any valid UPI id format always succeeds")
    void upi_anyValidFormat_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("testuser@upi");

        PaymentGatewayResponse resp = gateway.processPayment(req, 699.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("UPI - fail: invalid UPI format (missing @) → failure")
    void upi_invalidFormat_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("invalidemail");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid UPI ID");
    }

    // ── NETBANKING ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NETBANKING - success: valid bank and account")
    void netbanking_valid_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("HDFC");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("NETBANKING - success: all valid banks work")
    void netbanking_allBanksWork_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("BOB");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("NETBANKING - fail: invalid bank code → failure")
    void netbanking_invalidBankCode_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("UNKNOWN");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid bank code");
    }

    @Test
    @DisplayName("NETBANKING - fail: account number too short (< 9 digits)")
    void netbanking_invalidAccountNumber_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("SBI");
        req.setAccountNumber("1234");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid account number");
    }

    @Test
    @DisplayName("NETBANKING - fail: missing bank code → failure")
    void netbanking_missingBankCode_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Bank code is required");
    }

    // ── WALLET (OmniCharge Only) ──────────────────────────────────────────────

    @Test
    @DisplayName("WALLET - success: OmniCharge wallet succeeds")
    void wallet_omnicharge_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("WALLET");
        req.setWalletType("OMNICHARGE");

        PaymentGatewayResponse resp = gateway.processPayment(req, 999.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getPaymentReference()).contains("OMNICHARGE-WALLET");
    }

    @Test
    @DisplayName("WALLET - fail: non-OmniCharge wallet type rejected")
    void wallet_nonOmnicharge_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("WALLET");
        req.setWalletType("PAYTM");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Only OmniCharge Wallet is supported");
    }

    // ── Invalid payment method ────────────────────────────────────────────────

    @Test
    @DisplayName("processPayment() - unknown method returns FAILED response")
    void unknownMethod_returnsFailure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CRYPTO");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid payment method");
    }
}
