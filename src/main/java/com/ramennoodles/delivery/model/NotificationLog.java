package com.ramennoodles.delivery.model;

import com.ramennoodles.delivery.enums.ChannelType;
import com.ramennoodles.delivery.enums.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a notification sent to a customer.
 * Tracks notification status and delivery confirmation.
 * 
 * Shared with: DEI Hires (Delivery Orders) subsystem.
 * 
 * Relationships:
 *  - NotificationLog belongs to 1 Order
 *  - NotificationLog belongs to 1 Customer
 */
public class NotificationLog {
    private String notifId;          // PK
    private String orderId;          // FK -> Order
    private String customerId;       // FK -> Customer
    private ChannelType channel;
    private String message;
    private LocalDateTime sentAt;
    private DeliveryStatus deliveryStatus;

    // --- Factory Methods ---
    public static NotificationLog sendSMS(String orderId, String customerId, String message) {
        return createNotification(orderId, customerId, ChannelType.SMS, message);
    }

    public static NotificationLog sendEmail(String orderId, String customerId, String message) {
        return createNotification(orderId, customerId, ChannelType.EMAIL, message);
    }

    private static NotificationLog createNotification(String orderId, String customerId,
                                                       ChannelType channel, String message) {
        NotificationLog log = new NotificationLog();
        log.notifId = "NTF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.orderId = orderId;
        log.customerId = customerId;
        log.channel = channel;
        log.message = message;
        log.sentAt = LocalDateTime.now();
        log.deliveryStatus = DeliveryStatus.SENT;
        return log;
    }

    // --- Business Methods ---
    public void updateDeliveryStatus(DeliveryStatus status) {
        this.deliveryStatus = status;
    }

    public void retryFailed() {
        if (this.deliveryStatus == DeliveryStatus.FAILED) {
            this.deliveryStatus = DeliveryStatus.RETRYING;
            this.sentAt = LocalDateTime.now(); // Update send timestamp
        }
    }

    // --- Getters ---
    public String getNotifId() { return notifId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public ChannelType getChannel() { return channel; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }

    @Override
    public String toString() {
        return String.format("NotificationLog[%s, order=%s, channel=%s, status=%s, msg='%s']",
                notifId, orderId, channel, deliveryStatus, message);
    }
}
