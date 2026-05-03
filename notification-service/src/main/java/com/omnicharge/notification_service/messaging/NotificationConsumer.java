package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import com.omnicharge.notification_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private static final String BORDER = "══════════════════════════════════════════════════════════════";
    private static final String DIVIDER = "──────────────────────────────────────────────────────────────";

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.notification.queue}")
    public void consumePaymentResult(PaymentResultMessage result) {

        log.info(BORDER);
        log.info("  NOTIFICATION SERVICE — PAYMENT RESULT RECEIVED");
        log.info(BORDER);
        log.info("  Recharge ID     : {}", result.getRechargeId());
        log.info("  Transaction ID  : {}", result.getTransactionId());
        log.info("  Username        : {}", result.getUsername());
        log.info("  Email           : {}", result.getUserEmail());
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
        // SMS (logged)
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

        // Email (actually sent)
        String emailSubject = "Recharge Successful — OmniCharge";
        String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your recharge was completed SUCCESSFULLY.\n\n" +
                "Transaction ID : %s\n" +
                "Recharge ID    : %s\n" +
                "Mobile         : %s\n" +
                "Operator       : %s\n" +
                "Plan           : %s\n" +
                "Amount         : ₹%.0f\n" +
                "Validity       : %s\n" +
                "Data           : %s\n" +
                "Payment Ref    : %s\n" +
                "Processed At   : %s\n\n" +
                "Thank you for using OmniCharge!\n" +
                "— Team OmniCharge",
                result.getUsername(),
                result.getTransactionId(),
                result.getRechargeId(),
                result.getMobileNumber(),
                result.getOperatorName(),
                result.getPlanName(),
                result.getAmount(),
                result.getValidity(),
                result.getDataInfo(),
                result.getPaymentReference(),
                result.getProcessedAt()
        );

        log.info(DIVIDER);
        log.info("  📧 EMAIL NOTIFICATION — SUCCESS");
        log.info(DIVIDER);
        emailService.sendEmail(result.getUserEmail(), emailSubject, emailBody);
        log.info(DIVIDER);
    }

    // ──────────────────────────────────────────────────────
    //  FAILED notifications
    // ──────────────────────────────────────────────────────
    private void sendFailureNotifications(PaymentResultMessage result) {
        // SMS (logged)
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

        // Email (actually sent)
        String emailSubject = "Recharge Failed — OmniCharge";
        String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your recharge has FAILED.\n\n" +
                "Reason         : %s\n" +
                "Transaction ID : %s\n" +
                "Recharge ID    : %s\n" +
                "Mobile         : %s\n" +
                "Amount         : ₹%.0f\n\n" +
                "Please try again or contact support.\n" +
                "— Team OmniCharge",
                result.getUsername(),
                result.getFailureReason() != null ? result.getFailureReason() : "Unknown error",
                result.getTransactionId(),
                result.getRechargeId(),
                result.getMobileNumber(),
                result.getAmount()
        );

        log.info(DIVIDER);
        log.warn("  📧 EMAIL NOTIFICATION — FAILED");
        log.info(DIVIDER);
        emailService.sendEmail(result.getUserEmail(), emailSubject, emailBody);
        log.info(DIVIDER);
    }

    // ──────────────────────────────────────────────────────
    //  REFUND_PENDING notifications
    // ──────────────────────────────────────────────────────
    private void sendRefundNotifications(PaymentResultMessage result) {
        // SMS (logged)
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

        // Email (actually sent)
        String emailSubject = "Refund Initiated — OmniCharge";
        String emailBody = String.format(
                "Dear %s,\n\n" +
                "We regret to inform you that while your payment was processed\n" +
                "successfully, the recharge could not be completed due to a\n" +
                "temporary service disruption.\n\n" +
                "REFUND DETAILS:\n" +
                "────────────────────────────────────\n" +
                "Transaction ID  : %s\n" +
                "Recharge ID     : %s\n" +
                "Mobile Number   : %s\n" +
                "Operator        : %s\n" +
                "Plan            : %s\n" +
                "Amount          : ₹%.0f\n" +
                "Payment Ref     : %s\n" +
                "Refund Status   : INITIATED\n" +
                "Refund ETA      : 5-7 business days\n" +
                "────────────────────────────────────\n\n" +
                "The refund of ₹%.0f will be credited back to your original\n" +
                "payment method within 5-7 business days. No action is needed\n" +
                "from your side.\n\n" +
                "If you do not receive the refund within 7 business days,\n" +
                "please contact our support team at helpdesk@omnicharge.com\n" +
                "with your Transaction ID: %s\n\n" +
                "We sincerely apologize for the inconvenience.\n" +
                "— Team OmniCharge",
                result.getUsername(),
                result.getTransactionId(),
                result.getRechargeId(),
                result.getMobileNumber(),
                result.getOperatorName(),
                result.getPlanName(),
                result.getAmount(),
                result.getPaymentReference() != null ? result.getPaymentReference() : "N/A",
                result.getAmount(),
                result.getTransactionId()
        );

        log.warn(DIVIDER);
        log.warn("  📧 EMAIL NOTIFICATION — REFUND PENDING");
        log.warn(DIVIDER);
        emailService.sendEmail(result.getUserEmail(), emailSubject, emailBody);
        log.warn(DIVIDER);
    }
}