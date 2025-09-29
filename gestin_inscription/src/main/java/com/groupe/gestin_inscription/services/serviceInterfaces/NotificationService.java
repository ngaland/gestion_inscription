package com.groupe.gestin_inscription.services.serviceInterfaces;

import jakarta.mail.MessagingException;

public interface NotificationService {

    public void sendEmailNotification(String username, Long Id, String recipient, String template, String subject) throws MessagingException;

    public void sendSmsReminder(String phoneNumber, String message);

    public void sendInAppNotification(Long userId, String message);
}
