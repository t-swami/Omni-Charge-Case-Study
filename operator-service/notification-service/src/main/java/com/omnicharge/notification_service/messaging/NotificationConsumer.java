package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private static final String BORDER = "══════════════════════════════════════════════════════════════";
    private static final String DIVIDER = "──────────────────────────────────────────────────────────────";

    @RabbitListener(queues = "${rabbitmq.notification.queue}")
    public void consumePaymentResult(PaymentResultMessage result) {

        log.info(BORDER);
        log.info("  NOTIFICATION SERVICE — PAYMENT RESULT RECEIVED");
        log.info(BORDER);
        log.info("  Recharge ID     : {}", result.getRechargeId());
        log.info("  Transaction ID  : {}", result.getTransactionId());
        log.info("  Username        : {}", result.getUsername());
        log.info("  Mobile          : {}", result.getMobileNumber());
        log.info("  Operator        : {}", result.getOperatorName());
        log.info("  Plan            : {}", result.getPlanName());
        log.info("  Amount          : ₹{}", result.getAmount());
        log.info("  Validity        : {}", result.getValidity());
        log.info("  Data            : {}", result.getDataInfo());
        log.info("  Payment Status  : {}", result.getStatus());

        if (result.getPaymentReference() != null) {
            log.info("  Payment Ref     : {}", result.getPaymentReference());
        }
        if (result.getFailureReason() != null) {
            log.warn("  Failure Reason  : {}", result.getFailureReason());
        }
        log.info("  Processed At    : {}", result.getProcessedAt());
        log.info(BORDER);

        // Route notification based on status
        switch (result.getStatus()) {
            case "SUCCESS":
                sendSuccessNotifications(result);
                break;
            case "FAILED":
                sendFailureNotifications(result);
                break;
            case "REFUND_PENDING":
                sendRefundNotifications(result);
                break;
            default:
                log.warn("  Unknown payment status: {}. Sending generic notification.", result.getStatus());
                sendFailureNotifications(result);
                break;
        }

        log.info(BORDER);
        log.info("  ✓ ALL NOTIFICATIONS DISPATCHED SUCCESSFULLY");
        log.info("  RechargeId     : {}", result.getRechargeId());
        log.info("  TransactionId  : {}", result.getTransactionId());
        log.info("  Final Status   : {}", result.getStatus());
        log.info(BORDER);
    }

    // ──────────────────────────────────────────────────────
    //  SUCCESS notifications
    // ──────────────────────────────────────────────────────
    private void sendSuccessNotifications(PaymentResultMessage result) {
        // SMS
        String sms = String.format(
                "Dear %s, your recharge of ₹%.0f for %s " +
                "with %s plan %s is SUCCESSFUL. " +
                "TxnID: %s | Validity: %s | Data: %s — Team OmniCharge",
                result.getUsername(),
                result.getAmount(),
                result.getMobileNumber(),
                result.getOperatorName(),
                result.getPlanName(),
                result.getTransactionId(),
                result.getValidity(),
                result.getDataInfo()
        );

        log.info(DIVIDER);
        log.info("  📱 SMS NOTIFICATION — SUCCESS");
        log.info(DIVIDER);
        log.info("  To      : {}", result.getMobileNumber());
        log.info("  Message : {}", sms);
        log.info(DIVIDER);

        // Email
        log.info(DIVIDER);
        log.info("  📧 EMAIL NOTIFICATION — SUCCESS");
        log.info(DIVIDER);
        log.info("  To      : {}@omnicharge.com", result.getUsername());
        log.info("  Subject : Recharge Successful — OmniCharge");
        log.info("  Body    :");
        log.info("  Dear {},", result.getUsername());
        log.info("  Your recharge was completed SUCCESSFULLY.");
        log.info("  Transaction ID : {}", result.getTransactionId());
        log.info("  Recharge ID    : {}", result.getRechargeId());
        log.info("  Mobile         : {}", result.getMobileNumber());
        log.info("  Operator       : {}", result.getOperatorName());
        log.info("  Plan           : {}", result.getPlanName());
        log.info("  Amount         : ₹{}", result.getAmount());
        log.info("  Validity       : {}", result.getValidity());
        log.info("  Data           : {}", result.getDataInfo());
        log.info("  Payment Ref    : {}", result.getPaymentReference());
        log.info("  Processed At   : {}", result.getProcessedAt());
        log.info("  Thank you for using OmniCharge!");
        log.info("  — Team OmniCharge");
        log.info(DIVIDER);
    }

    // ──────────────────────────────────────────────────────
    //  FAILED notifications
    // ──────────────────────────────────────────────────────
    private void sendFailureNotifications(PaymentResultMessage result) {
        // SMS
        String sms = String.format(
                "Dear %s, your recharge of ₹%.0f for %s FAILED. " +
                "Reason: %s. Please try again. — Team OmniCharge",
                result.getUsername(),
                result.getAmount(),
                result.getMobileNumber(),
                result.getFailureReason() != null ? result.getFailureReason() : "Unknown error"
        );

        log.info(DIVIDER);
        log.warn("  📱 SMS NOTIFICATION — FAILED");
        log.info(DIVIDER);
        log.info("  To      : {}", result.getMobileNumber());
        log.info("  Message : {}", sms);
        log.info(DIVIDER);

        // Email
        log.info(DIVIDER);
        log.warn("  📧 EMAIL NOTIFICATION — FAILED");
        log.info(DIVIDER);
        log.info("  To      : {}@omnicharge.com", result.getUsername());
        log.info("  Subject : Recharge Failed — OmniCharge");
        log.info("  Body    :");
        log.info("  Dear {},", result.getUsername());
        log.warn("  Your recharge has FAILED.");
        log.warn("  Reason         : {}", result.getFailureReason());
        log.info("  Transaction ID : {}", result.getTransactionId());
        log.info("  Recharge ID    : {}", result.getRechargeId());
        log.info("  Mobile         : {}", result.getMobileNumber());
        log.info("  Amount         : ₹{}", result.getAmount());
        log.info("  Please try again or contact support.");
        log.info("  — Team OmniCharge");
        log.info(DIVIDER);
    }

    // ──────────────────────────────────────────────────────
    //  REFUND_PENDING notifications
    // ──────────────────────────────────────────────────────
    private void sendRefundNotifications(PaymentResultMessage result) {
        // SMS
        String sms = String.format(
                "Dear %s, your payment of ₹%.0f for %s was successful " +
                "but the recharge could not be completed due to a service issue. " +
                "Your amount will be refunded to your original payment method within 5-7 business days. " +
                "Ref: %s | TxnID: %s. For support, contact helpdesk@omnicharge.com — Team OmniCharge",
                result.getUsername(),
                result.getAmount(),
                result.getMobileNumber(),
                result.getPaymentReference() != null ? result.getPaymentReference() : "N/A",
                result.getTransactionId()
        );

        log.warn(BORDER);
        log.warn("  📱 SMS NOTIFICATION — REFUND PENDING");
        log.warn(BORDER);
        log.warn("  To      : {}", result.getMobileNumber());
        log.warn("  Message : {}", sms);
        log.warn(BORDER);

        // Email
        log.warn(BORDER);
        log.warn("  📧 EMAIL NOTIFICATION — REFUND PENDING");
        log.warn(BORDER);
        log.warn("  To      : {}@omnicharge.com", result.getUsername());
        log.warn("  Subject : Refund Initiated — OmniCharge");
        log.warn("  Body    :");
        log.warn("  Dear {},", result.getUsername());
        log.warn("");
        log.warn("  We regret to inform you that while your payment was processed");
        log.warn("  successfully, the recharge could not be completed due to a");
        log.warn("  temporary service disruption.");
        log.warn("");
        log.warn("  REFUND DETAILS:");
        log.warn("  ────────────────────────────────────");
        log.warn("  Transaction ID  : {}", result.getTransactionId());
        log.warn("  Recharge ID     : {}", result.getRechargeId());
        log.warn("  Mobile Number   : {}", result.getMobileNumber());
        log.warn("  Operator        : {}", result.getOperatorName());
        log.warn("  Plan            : {}", result.getPlanName());
        log.warn("  Amount          : ₹{}", result.getAmount());
        log.warn("  Payment Ref     : {}", result.getPaymentReference());
        log.warn("  Refund Status   : INITIATED");
        log.warn("  Refund ETA      : 5-7 business days");
        log.warn("  ────────────────────────────────────");
        log.warn("");
        log.warn("  The refund of ₹{} will be credited back to your original", result.getAmount());
        log.warn("  payment method within 5-7 business days. No action is needed");
        log.warn("  from your side.");
        log.warn("");
        log.warn("  If you do not receive the refund within 7 business days,");
        log.warn("  please contact our support team at helpdesk@omnicharge.com");
        log.warn("  with your Transaction ID: {}", result.getTransactionId());
        log.warn("");
        log.warn("  We sincerely apologize for the inconvenience.");
        log.warn("  — Team OmniCharge");
        log.warn(BORDER);
    }
}