package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.*;
import com.omnicharge.payment_service.entity.PaymentMethod;
import com.omnicharge.payment_service.entity.Transaction;
import com.omnicharge.payment_service.entity.TransactionStatus;
import com.omnicharge.payment_service.feign.RechargeServiceFeignClient;
import com.omnicharge.payment_service.feign.UserServiceFeignClient;
import com.omnicharge.payment_service.messaging.PaymentResultPublisher;
import com.omnicharge.payment_service.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final String LOG_BORDER = "══════════════════════════════════════════════════════════════";
    private static final String LOG_DIVIDER = "──────────────────────────────────────────────────────────────";
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RechargeServiceFeignClient rechargeServiceFeignClient;

    @Autowired
    private PaymentResultPublisher paymentResultPublisher;

    @Autowired
    private DummyPaymentGatewayService paymentGatewayService;

    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    @Value("${app.retry.max-attempts:6}")
    private int maxRetryAttempts;

    @Value("${app.retry.interval-seconds:30}")
    private long retryIntervalSeconds;

    @Value("${app.refund.eta:5-7 business days}")
    private String refundEta;

    @Value("${app.transaction.id-prefix:TXN}")
    private String transactionIdPrefix;

    // ──────────────────────────────────────────────────────
    // Step 1 — Called by RabbitMQ when recharge is initiated.
    // Creates a PENDING transaction; user calls /pay to complete it.
    // ──────────────────────────────────────────────────────
    @Override
    public TransactionDto processPayment(RechargeEventMessage event) {

        if (transactionRepository.findByRechargeId(event.getRechargeId()).isPresent()) {
            log.warn("Duplicate event received for rechargeId={}. Returning existing transaction.",
                    event.getRechargeId());
            return mapToDto(transactionRepository.findByRechargeId(event.getRechargeId()).get());
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setRechargeId(event.getRechargeId());
        transaction.setUsername(event.getUsername());
        transaction.setMobileNumber(event.getMobileNumber());
        transaction.setOperatorName(event.getOperatorName());
        transaction.setPlanName(event.getPlanName());
        transaction.setAmount(event.getAmount());
        transaction.setValidity(event.getValidity());
        transaction.setDataInfo(event.getDataInfo());
        transaction.setUserEmail(event.getUserEmail());
        transaction.setStatus(TransactionStatus.PENDING);

        transactionRepository.save(transaction);

        log.info(LOG_BORDER);
        log.info("  TRANSACTION CREATED — AWAITING PAYMENT");
        log.info(LOG_BORDER);
        log.info("  Transaction ID : {}", transaction.getTransactionId());
        log.info("  Recharge ID    : {}", event.getRechargeId());
        log.info("  Username       : {}", event.getUsername());
        log.info("  Mobile         : {}", event.getMobileNumber());
        log.info("  Operator       : {}", event.getOperatorName());
        log.info("  Plan           : {}", event.getPlanName());
        log.info("  Amount         : ₹{}", event.getAmount());
        log.info("  Status         : PENDING");
        log.info("  Next Step      : Call /api/transactions/pay to complete payment");
        log.info(LOG_BORDER);

        return mapToDto(transaction);
    }

    // ──────────────────────────────────────────────────────
    // Step 2 — Called by user to actually make the payment
    // ──────────────────────────────────────────────────────
    @Override
    public TransactionDto makePayment(String username, PaymentGatewayRequest request, String token) {

        Transaction transaction = transactionRepository.findByRechargeId(request.getRechargeId())
                .orElseThrow(() -> new RuntimeException(
                        "No pending transaction found for recharge id: "
                        + request.getRechargeId()
                        + ". Please initiate recharge first"));

        if (!transaction.getUsername().equals(username)) {
            throw new RuntimeException("Access denied. You can only pay for your own recharge");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException(
                    "Transaction is already " + transaction.getStatus().name() + ". Cannot process again");
        }

        log.info(LOG_BORDER);
        log.info("  PAYMENT GATEWAY — PROCESSING");
        log.info(LOG_BORDER);
        log.info("  Transaction ID  : {}", transaction.getTransactionId());
        log.info("  Amount          : ₹{}", transaction.getAmount());
        log.info("  Payment Method  : {}", request.getPaymentMethod());
        log.info(LOG_DIVIDER);

        // Real-World Wallet: Check balance in user-service before processing
        if ("WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
            try {
                java.math.BigDecimal balance = userServiceFeignClient.getWalletBalance(token);
                if (balance.compareTo(java.math.BigDecimal.valueOf(transaction.getAmount())) < 0) {
                    throw new RuntimeException("Insufficient OmniCharge Wallet balance. Current: ₹" + balance);
                }
            } catch (Exception ex) {
                log.error("  ✗ FAILED TO VERIFY WALLET BALANCE: {}", ex.getMessage());
                throw new RuntimeException("Could not verify wallet balance: " + ex.getMessage());
            }
        }

        PaymentGatewayResponse gatewayResponse = paymentGatewayService.processPayment(request, transaction.getAmount());

        if (gatewayResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setPaymentReference(gatewayResponse.getPaymentReference());
            
            // Real-World Wallet: Deduct from user-service balance if success
            if ("WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
                try {
                    userServiceFeignClient.updateWalletBalance(token, java.math.BigDecimal.valueOf(transaction.getAmount()), false);
                    log.info("  ✓ AMOUNT DEDUCTED FROM OMNICHARGE WALLET");
                } catch (Exception ex) {
                    log.error("  ⚠ PAYMENT SUCCESSFUL BUT WALLET DEDUCTION FAILED: {}", ex.getMessage());
                    // In a real system, you'd handle this with a compensation transaction or manual intervention
                }
            }
            
            log.info("  ✓ PAYMENT SUCCESSFUL");
            log.info("  Reference       : {}", gatewayResponse.getPaymentReference());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(gatewayResponse.getFailureReason());
            log.warn("  ✗ PAYMENT FAILED");
            log.warn("  Reason          : {}", gatewayResponse.getFailureReason());
        }
        log.info(LOG_BORDER);

        transaction.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        transactionRepository.save(transaction);

        // Asynchronously update recharge status with retry logic (circuit breaker)
        updateRechargeStatusWithRetry(transaction.getRechargeId(), transaction.getId(), token);

        // Publish notification immediately so user gets feedback
        publishNotification(transaction.getRechargeId(), transaction);

        return mapToDto(transaction);
    }

    @Override
    public TransactionDto topUpWallet(String username, PaymentGatewayRequest request, String token) {
        log.info(LOG_BORDER);
        log.info("  WALLET TOP-UP — PROCESSING");
        log.info(LOG_BORDER);
        log.info("  User            : {}", username);
        log.info("  Amount          : ₹{}", request.getAmount());
        log.info("  Method          : {}", request.getPaymentMethod());
        log.info(LOG_DIVIDER);

        // Process top-up payment via gateway
        PaymentGatewayResponse gatewayResponse = paymentGatewayService.processPayment(request, request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setRechargeId(-1L); // Use -1 to indicate it's a top-up, not a recharge
        transaction.setUsername(username);
        transaction.setAmount(request.getAmount());
        transaction.setMobileNumber("N/A");
        transaction.setOperatorName("OmniCharge");
        transaction.setPlanName("Wallet Top-Up");
        transaction.setValidity("N/A");
        transaction.setDataInfo("N/A");
        transaction.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        
        if (gatewayResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setPaymentReference(gatewayResponse.getPaymentReference());
            
            // Update user balance in user-service
            try {
                userServiceFeignClient.updateWalletBalance(token, java.math.BigDecimal.valueOf(request.getAmount()), true);
                log.info("  ✓ WALLET BALANCE UPDATED IN USER-SERVICE");
            } catch (Exception ex) {
                log.error("  ✗ FAILED TO UPDATE WALLET BALANCE: {}", ex.getMessage());
                throw new RuntimeException("Payment successful but failed to update wallet balance: " + ex.getMessage());
            }
            
            log.info("  ✓ TOP-UP SUCCESSFUL");
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(gatewayResponse.getFailureReason());
            log.warn("  ✗ TOP-UP FAILED: {}", gatewayResponse.getFailureReason());
        }
        
        log.info(LOG_BORDER);
        transactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    // ──────────────────────────────────────────────────────
    // Async retry: attempts to update recharge status.
    // If all retries fail and payment was SUCCESS:
    //   → mark as REFUND_PENDING
    //   → notify user via notification-service
    // ──────────────────────────────────────────────────────
    @Async
    public void updateRechargeStatusWithRetry(Long rechargeId, Long transactionId, String token) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);
        if (transaction == null) {
            log.error("Transaction not found for async retry: id={}", transactionId);
            return;
        }

        log.info(LOG_DIVIDER);
        log.info("  CIRCUIT BREAKER — Starting recharge status update");
        log.info("  Recharge ID     : {}", rechargeId);
        log.info("  Max Attempts    : {}", maxRetryAttempts);
        log.info("  Retry Interval  : {}s", retryIntervalSeconds);
        log.info(LOG_DIVIDER);

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                RechargeStatusUpdateRequest updateRequest = new RechargeStatusUpdateRequest(
                        transaction.getStatus().name(),
                        transaction.getFailureReason());
                rechargeServiceFeignClient.updateRechargeStatus(rechargeId, updateRequest);

                log.info(LOG_BORDER);
                log.info("  ✓ RECHARGE STATUS UPDATED SUCCESSFULLY");
                log.info("  Attempt         : {}/{}", attempt, maxRetryAttempts);
                log.info("  Recharge ID     : {}", rechargeId);
                log.info("  Status Sent     : {}", transaction.getStatus().name());
                log.info(LOG_BORDER);
                return; // Success — exit retry loop

            } catch (Exception ex) {
                log.warn("  ⟳ Attempt {}/{} FAILED — Recharge service unreachable", attempt, maxRetryAttempts);
                log.warn("    Error: {}", ex.getMessage());

                if (attempt < maxRetryAttempts) {
                    log.info("    Retrying in {}s...", retryIntervalSeconds);
                    try {
                        Thread.sleep(retryIntervalSeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("    Retry thread interrupted for rechargeId={}", rechargeId);
                        return;
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // All retries exhausted — handle refund scenario
        // ──────────────────────────────────────────────────────
        handleRetryExhaustion(rechargeId, transactionId, token);
    }

    /**
     * Called when all retry attempts are exhausted.
     * If payment was SUCCESS → marks transaction as REFUND_PENDING and notifies user.
     */
    private void handleRetryExhaustion(Long rechargeId, Long transactionId, String token) {
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            return;
        }

        log.error(LOG_BORDER);
        log.error("  ✗ CIRCUIT BREAKER — ALL RETRIES EXHAUSTED");
        log.error(LOG_BORDER);
        log.error("  Total Attempts  : {}", maxRetryAttempts);
        log.error("  Recharge ID     : {}", rechargeId);
        log.error("  Transaction ID  : {}", transaction.getTransactionId());
        log.error("  Current Status  : {}", transaction.getStatus().name());

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {

            String refundMessage = String.format(
                "Reason: Recharge unsuccessful due to operator system error. Refund: ₹%.0f will be returned to your original payment method within %s.",
                transaction.getAmount(),
                refundEta
            );

            transaction.setStatus(TransactionStatus.REFUND_PENDING);
            transaction.setFailureReason(refundMessage);
            transactionRepository.save(transaction);

            if (transaction.getPaymentMethod() == PaymentMethod.WALLET) {
                try {
                    userServiceFeignClient.updateWalletBalance(token, java.math.BigDecimal.valueOf(transaction.getAmount()), true);
                    log.warn("  ✓ INSTANT WALLET REFUND PROCESSED FOR AMOUNT: ₹{}", transaction.getAmount());
                    refundMessage = "Instant refund of ₹" + transaction.getAmount() + " added to your OmniCharge Wallet.";
                    transaction.setFailureReason(refundMessage);
                    transactionRepository.save(transaction);
                } catch (Exception ex) {
                    log.error("  ✗ FAILED TO PROCESS INSTANT WALLET REFUND: {}", ex.getMessage());
                }
            }

            log.warn(LOG_BORDER);
            log.warn("  ⚠ REFUND INITIATED");
            log.warn(LOG_BORDER);
            log.warn("  Transaction ID  : {}", transaction.getTransactionId());
            log.warn("  Recharge ID     : {}", rechargeId);
            log.warn("  Username        : {}", transaction.getUsername());
            log.warn("  Mobile          : {}", transaction.getMobileNumber());
            log.warn("  Amount          : ₹{}", transaction.getAmount());
            log.warn("  Payment Ref     : {}", transaction.getPaymentReference());
            log.warn("  New Status      : REFUND_PENDING");
            log.warn("  Refund ETA      : {}", refundEta);
            log.warn("  Timestamp       : {}", LocalDateTime.now().format(TIMESTAMP_FMT));
            log.warn(LOG_DIVIDER);
            log.warn("  Action: Refund notification being sent to user...");
            log.warn(LOG_BORDER);

            // Send refund notification to notification-service
            publishRefundNotification(rechargeId, transaction);

        } else {
            log.error("  Payment status is {} — no refund needed", transaction.getStatus().name());
            log.error(LOG_BORDER);
        }
    }

    /**
     * Publishes a refund-specific notification to the notification queue.
     */
    private void publishRefundNotification(Long rechargeId, Transaction transaction) {
        try {
            PaymentResultMessage result = new PaymentResultMessage();
            result.setRechargeId(rechargeId);
            result.setTransactionId(transaction.getTransactionId());
            result.setUsername(transaction.getUsername());
            result.setMobileNumber(transaction.getMobileNumber());
            result.setOperatorName(transaction.getOperatorName());
            result.setPlanName(transaction.getPlanName());
            result.setAmount(transaction.getAmount());
            result.setValidity(transaction.getValidity());
            result.setDataInfo(transaction.getDataInfo());
            result.setStatus("REFUND_PENDING");
            result.setFailureReason(transaction.getFailureReason());
            result.setPaymentReference(transaction.getPaymentReference());
            result.setProcessedAt(LocalDateTime.now());
            result.setUserEmail(transaction.getUserEmail());
            paymentResultPublisher.publishPaymentResult(result);

            log.info("  ✓ Refund notification published to notification-service");
            log.info("    RechargeId={}, TxnId={}", rechargeId, transaction.getTransactionId());
        } catch (Exception ex) {
            log.error("  ✗ Failed to publish refund notification: {}", ex.getMessage());
        }
    }

    /**
     * Publishes payment result notification (SUCCESS/FAILED) to notification queue.
     */
    private void publishNotification(Long rechargeId, Transaction transaction) {
        try {
            PaymentResultMessage result = new PaymentResultMessage();
            result.setRechargeId(rechargeId);
            result.setTransactionId(transaction.getTransactionId());
            result.setUsername(transaction.getUsername());
            result.setMobileNumber(transaction.getMobileNumber());
            result.setOperatorName(transaction.getOperatorName());
            result.setPlanName(transaction.getPlanName());
            result.setAmount(transaction.getAmount());
            result.setValidity(transaction.getValidity());
            result.setDataInfo(transaction.getDataInfo());
            result.setStatus(transaction.getStatus().name());
            result.setFailureReason(transaction.getFailureReason());
            result.setPaymentReference(transaction.getPaymentReference());
            result.setProcessedAt(LocalDateTime.now());
            result.setUserEmail(transaction.getUserEmail());
            paymentResultPublisher.publishPaymentResult(result);

            log.info("  Notification published → notification-service [status={}]", transaction.getStatus().name());
        } catch (Exception ex) {
            log.error("  Failed to publish notification for rechargeId={}: {}", rechargeId, ex.getMessage());
        }
    }

    @Override
    public List<TransactionDto> getMyTransactions(String username) {
        return transactionRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public TransactionDto getByTransactionId(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return mapToDto(transaction);
    }

    @Override
    public TransactionDto getByRechargeId(Long rechargeId) {
        Transaction transaction = transactionRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found for recharge id: " + rechargeId));
        return mapToDto(transaction);
    }

    @Override
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDto> getTransactionsByStatus(String status) {
        try {
            TransactionStatus txStatus = TransactionStatus.valueOf(status.toUpperCase());
            return transactionRepository.findByStatus(txStatus)
                    .stream().map(this::mapToDto).collect(Collectors.toList());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                "Invalid status. Valid: PENDING, SUCCESS, FAILED, REFUND_PENDING, CANCELLED");
        }
    }

    @Override
    public List<TransactionDto> getTransactionsByMobile(String mobileNumber) {
        return transactionRepository.findByMobileNumber(mobileNumber)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private String generateTransactionId() {
        return transactionIdPrefix + UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 12);
    }

    private TransactionDto mapToDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionId(transaction.getTransactionId());
        dto.setRechargeId(transaction.getRechargeId());
        dto.setUsername(transaction.getUsername());
        dto.setMobileNumber(transaction.getMobileNumber());
        dto.setOperatorName(transaction.getOperatorName());
        dto.setPlanName(transaction.getPlanName());
        dto.setAmount(transaction.getAmount());
        dto.setValidity(transaction.getValidity());
        dto.setDataInfo(transaction.getDataInfo());
        dto.setStatus(transaction.getStatus().name());
        dto.setPaymentMethod(
                transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null);
        dto.setPaymentReference(transaction.getPaymentReference());
        dto.setFailureReason(transaction.getFailureReason());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        return dto;
    }
}