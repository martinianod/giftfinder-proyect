package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.NotificationLog;
import com.findoraai.giftfinder.notifications.model.Reminder;

import java.util.Map;

public interface NotificationService {
    /**
     * Send an email notification
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Template file name (without .html extension)
     * @param templateData Data to populate the template
     * @return true if sent successfully
     */
    boolean sendEmail(String to, String subject, String templateName, Map<String, Object> templateData);
    
    /**
     * Send a reminder notification
     * @param user User to send notification to
     * @param reminder Reminder to send
     * @param eventName Name of the event
     * @param eventDate Date of the event (formatted)
     * @param daysUntil Days until the event
     * @param recipientName Optional recipient name
     * @return true if sent successfully
     */
    boolean sendReminderNotification(User user, Reminder reminder, String eventName, 
                                    String eventDate, int daysUntil, String recipientName);
    
    /**
     * Send a price drop notification
     * @param user User to send notification to
     * @param productTitle Product title
     * @param productUrl Product URL
     * @param productImageUrl Product image URL
     * @param oldPrice Old price
     * @param newPrice New price
     * @param currency Currency code
     * @param dropPercentage Percentage of price drop
     * @param savingsAmount Amount saved
     * @return true if sent successfully
     */
    boolean sendPriceDropNotification(User user, String productTitle, String productUrl,
                                     String productImageUrl, String oldPrice, String newPrice,
                                     String currency, String dropPercentage, String savingsAmount);
    
    /**
     * Log a notification
     * @param user User
     * @param type Notification type
     * @param referenceId Reference ID
     * @param channel Notification channel
     * @param recipient Recipient email/phone
     * @param status Status
     * @param errorMessage Error message if failed
     */
    void logNotification(User user, NotificationLog.NotificationType type, String referenceId,
                        Reminder.NotificationChannel channel, String recipient,
                        NotificationLog.NotificationStatus status, String errorMessage);
    
    /**
     * Check if a notification was already sent recently (within 24 hours)
     * @param user User
     * @param type Notification type
     * @param referenceId Reference ID
     * @param channel Notification channel
     * @return true if already sent recently
     */
    boolean wasRecentlySent(User user, NotificationLog.NotificationType type, 
                           String referenceId, Reminder.NotificationChannel channel);
}
