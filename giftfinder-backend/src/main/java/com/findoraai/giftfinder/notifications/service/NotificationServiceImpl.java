package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.NotificationLog;
import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;
    private final TemplateService templateService;
    
    @Value("${spring.mail.username:noreply@giftfinder.com}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Override
    public boolean sendEmail(String to, String subject, String templateName, Map<String, Object> templateData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateService.processTemplate(templateName, templateData);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to {} with template {}", to, templateName);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email to {} with template {}: {}", to, templateName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendReminderNotification(User user, Reminder reminder, String eventName, 
                                          String eventDate, int daysUntil, String recipientName) {
        Map<String, Object> data = new HashMap<>();
        data.put("userName", user.getName() != null ? user.getName() : "there");
        data.put("eventName", eventName);
        data.put("eventDate", eventDate);
        data.put("daysUntil", daysUntil);
        if (recipientName != null) {
            data.put("recipientName", recipientName);
        }
        data.put("appUrl", appBaseUrl);
        
        String subject = String.format("Reminder: %s in %d days", eventName, daysUntil);
        boolean success = sendEmail(user.getEmail(), subject, "reminder-email", data);
        
        String referenceId = String.format("reminder-%d-%d", reminder.getImportantDate().getId(), reminder.getDaysBefore());
        logNotification(
            user,
            NotificationLog.NotificationType.REMINDER,
            referenceId,
            reminder.getChannel(),
            user.getEmail(),
            success ? NotificationLog.NotificationStatus.SUCCESS : NotificationLog.NotificationStatus.FAILED,
            success ? null : "Failed to send email"
        );
        
        return success;
    }

    @Override
    public boolean sendPriceDropNotification(User user, String productTitle, String productUrl,
                                           String productImageUrl, String oldPrice, String newPrice,
                                           String currency, String dropPercentage, String savingsAmount) {
        Map<String, Object> data = new HashMap<>();
        data.put("userName", user.getName() != null ? user.getName() : "there");
        data.put("productTitle", productTitle);
        data.put("productUrl", productUrl);
        if (productImageUrl != null) {
            data.put("productImageUrl", productImageUrl);
        }
        data.put("oldPrice", oldPrice);
        data.put("newPrice", newPrice);
        data.put("currency", currency);
        data.put("dropPercentage", dropPercentage);
        data.put("savingsAmount", savingsAmount);
        
        String subject = String.format("Price Drop Alert: %s", productTitle);
        boolean success = sendEmail(user.getEmail(), subject, "price-drop-email", data);
        
        String referenceId = String.format("price-drop-%s", productTitle);
        logNotification(
            user,
            NotificationLog.NotificationType.PRICE_DROP,
            referenceId,
            Reminder.NotificationChannel.EMAIL,
            user.getEmail(),
            success ? NotificationLog.NotificationStatus.SUCCESS : NotificationLog.NotificationStatus.FAILED,
            success ? null : "Failed to send email"
        );
        
        return success;
    }

    @Override
    @Transactional
    public void logNotification(User user, NotificationLog.NotificationType type, String referenceId,
                               Reminder.NotificationChannel channel, String recipient,
                               NotificationLog.NotificationStatus status, String errorMessage) {
        NotificationLog log = NotificationLog.builder()
            .user(user)
            .notificationType(type)
            .referenceId(referenceId)
            .channel(channel)
            .recipient(recipient)
            .status(status)
            .errorMessage(errorMessage)
            .build();
        
        notificationLogRepository.save(log);
    }

    @Override
    public boolean wasRecentlySent(User user, NotificationLog.NotificationType type, 
                                   String referenceId, Reminder.NotificationChannel channel) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return notificationLogRepository.existsByUserAndNotificationTypeAndReferenceIdAndChannelAndSentAtAfter(
            user, type, referenceId, channel, oneDayAgo
        );
    }
}
