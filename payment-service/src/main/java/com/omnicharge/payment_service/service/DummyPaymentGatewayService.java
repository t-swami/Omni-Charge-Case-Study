package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.PaymentGatewayResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simulated payment gateway with realistic validation.
 * All valid payment inputs will succeed. Only format validation errors cause failure.
 * Wallet payments only support OmniCharge Wallet (balance managed by user-service).
 */
@Service
public class DummyPaymentGatewayService {

	@Value("${payment.gateway.valid-banks:SBI,HDFC,ICICI,AXIS,KOTAK,BOB,PNB,CANARA,UNION,INDUSIND}")
	private String validBanksConfig;

	private List<String> getValidBanks() {
		return Arrays.stream(validBanksConfig.split(","))
				.map(String::trim).collect(Collectors.toList());
	}

	// Main entry point — routes to specific payment processor
	public PaymentGatewayResponse processPayment(PaymentGatewayRequest request, Double amount) {
		String method = request.getPaymentMethod().toUpperCase();

		switch (method) {
		case "CARD":
			return processCardPayment(request, amount);
		case "UPI":
			return processUpiPayment(request, amount);
		case "NETBANKING":
			return processNetbankingPayment(request, amount);
		case "WALLET":
			return processWalletPayment(request, amount);
		default:
			return failureResponse("Invalid payment method: " + method, method, amount);
		}
	}

	// ── CARD Payment ────────────────────────────────────────────────────────────

	private PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request, Double amount) {
		if (request.getCardNumber() == null || !request.getCardNumber().matches("\\d{16}")) {
			return failureResponse("Invalid card number. Must be 16 digits", "CARD", amount);
		}

		if (request.getCardExpiry() == null || !request.getCardExpiry().matches("(0[1-9]|1[0-2])/\\d{2}")) {
			return failureResponse("Invalid card expiry. Format must be MM/YY", "CARD", amount);
		}

		if (request.getCardCvv() == null || !request.getCardCvv().matches("\\d{3}")) {
			return failureResponse("Invalid CVV. Must be 3 digits", "CARD", amount);
		}

		if (request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
			return failureResponse("Card holder name is required", "CARD", amount);
		}

		String lastFour = request.getCardNumber().substring(12);
		return successResponse("CARD", amount, "CARD-" + lastFour);
	}

	// ── UPI Payment ─────────────────────────────────────────────────────────────

	private PaymentGatewayResponse processUpiPayment(PaymentGatewayRequest request, Double amount) {
		if (request.getUpiId() == null || !request.getUpiId().matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z]{3,}$")) {
			return failureResponse("Invalid UPI ID format. Example: rahul@upi or rahul@okaxis", "UPI", amount);
		}

		return successResponse("UPI", amount, "UPI-" + request.getUpiId());
	}

	// ── NETBANKING Payment ──────────────────────────────────────────────────────

	private PaymentGatewayResponse processNetbankingPayment(PaymentGatewayRequest request, Double amount) {
		if (request.getBankCode() == null || request.getBankCode().trim().isEmpty()) {
			return failureResponse("Bank code is required", "NETBANKING", amount);
		}

		List<String> banks = getValidBanks();
		if (!banks.contains(request.getBankCode().toUpperCase())) {
			return failureResponse("Invalid bank code. Valid codes: " + String.join(", ", banks),
					"NETBANKING", amount);
		}

		if (request.getAccountNumber() == null || !request.getAccountNumber().matches("\\d{9,18}")) {
			return failureResponse("Invalid account number. Must be 9 to 18 digits", "NETBANKING", amount);
		}

		return successResponse("NETBANKING", amount, "NB-" + request.getBankCode().toUpperCase() + "-"
				+ request.getAccountNumber().substring(request.getAccountNumber().length() - 4));
	}

	// ── WALLET Payment (OmniCharge Only) ─────────────────────────────────────────

	private PaymentGatewayResponse processWalletPayment(PaymentGatewayRequest request, Double amount) {
		// Only OmniCharge wallet is supported — balance is managed by user-service
		if (!"OMNICHARGE".equalsIgnoreCase(request.getWalletType())) {
			return failureResponse("Only OmniCharge Wallet is supported", "WALLET", amount);
		}

		return successResponse("WALLET", amount, "OMNICHARGE-WALLET");
	}

	// ── Response Builders ───────────────────────────────────────────────────────

	private PaymentGatewayResponse successResponse(String method, Double amount, String reference) {
		PaymentGatewayResponse response = new PaymentGatewayResponse();
		response.setSuccess(true);
		response.setStatus("SUCCESS");
		response.setPaymentMethod(method);
		response.setAmount(amount);
		response.setPaymentReference(
				"REF-" + reference + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		response.setMessage("Payment processed successfully");
		return response;
	}

	private PaymentGatewayResponse failureResponse(String reason, String method, Double amount) {
		PaymentGatewayResponse response = new PaymentGatewayResponse();
		response.setSuccess(false);
		response.setStatus("FAILED");
		response.setPaymentMethod(method);
		response.setAmount(amount);
		response.setFailureReason(reason);
		response.setMessage("Payment failed");
		return response;
	}
}