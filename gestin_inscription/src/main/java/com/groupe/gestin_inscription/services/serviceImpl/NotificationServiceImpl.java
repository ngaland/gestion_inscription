package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.model.Enums.NotificationStatus;
import com.groupe.gestin_inscription.model.Enums.NotificationType;
import com.groupe.gestin_inscription.model.Notification;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.repository.NotificationRepository;
import com.groupe.gestin_inscription.repository.UserRepository;
import com.groupe.gestin_inscription.services.serviceInterfaces.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender emailSender;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    // Uses the Email Service backend module
    @Override
    public void sendEmailNotification(String username, Long Id, String recipient, String subject, String content) throws MessagingException {

        // Retrieve the related entities
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        Application application = applicationRepository.findById(Id)
                .orElseThrow(() -> new NoSuchElementException("Application not found: " + Id));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setApplication(application);
        notification.setType(NotificationType.EMAIL); // Assuming you have a NotificationType enum
        notification.setMessage(subject + ": " + content); // Combine subject and content
        notification.setStatus(NotificationStatus.SENT); // Assuming a NotificationStatus enum
        // You might also want to set a timestamp here

        notificationRepository.save(notification);

        try {
            // 2. Transmit the Email
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, true);

            emailSender.send(message);

            // 3. Update Status on Success
            notification.setStatus(NotificationStatus.SENT);

        } catch (MessagingException e) {
            // 4. Update Status on Failure
            notification.setStatus(NotificationStatus.FAILED);
            // Re-throw or log the error
            throw e;

        } finally {
            // Ensure the final status is saved
            notificationRepository.save(notification);
        }
    }

    // Logic for sending SMS
    @Override
    public void sendSmsReminder(String phoneNumber, String message) {

        // Your Twilio Account SID and Auth Token
        String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
        String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);



        Message.creator(
                        new PhoneNumber(phoneNumber),
                        new PhoneNumber("+15017122661"), // Your Twilio phone number
                        message)
                .create();


        // Finding the User by phone number (New logic)
        User retrievedUser = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NoSuchElementException("User not found with phoneNumber: " + phoneNumber));

        // 3. Persist the Notification Record (The Fix)
        Notification notification = new Notification();
        notification.setType(NotificationType.SMS);
        notification.setMessage(message.substring(0, Math.min(message.length(), 255)));
        notification.setStatus(NotificationStatus.SENT);

        // Link the user if found
        if (retrievedUser != null) {
            notification.setUser(retrievedUser);
        }

        notificationRepository.save(notification);
    }



    // Logic for sending in-app notifications
    /**
     * Sends an in-app notification to a specific user.
     * @param userId The ID of the user to notify.
     * @param message The notification message content.
     */
    @Override
    public void sendInAppNotification(Long userId, String message) {
        // The destination is a user-specific topic, for example, /topic/notifications/{userId}
        // This pattern allows a client to subscribe to their own notifications.
        String destination = "/topic/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        //  Retrieve the User entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        // Persist the Notification Record (The Fix)
        Notification notification = new Notification();
        notification.setType(NotificationType.IN_APP);
        notification.setMessage(message.substring(0, Math.min(message.length(), 255)));
        notification.setStatus(NotificationStatus.UNREAD); // Typically UNREAD for in-app
        notification.setUser(user);

        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void sendIncompleteApplicationReminders() {

        // Defining what constitutes "incomplete" (e.g., status is DRAFT or PRE_VALIDATION for > 3 days)
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        applicationRepository.findIncompleteApplicationsOlderThan(ApplicationStatus.PRE_VALIDATION ,threeDaysAgo);

        List<Application> incompleteApps = applicationRepository.findIncompleteApplicationsOlderThan(
                ApplicationStatus.PRE_VALIDATION, //target applications that are still waiting for review only
                threeDaysAgo
        );

        for (Application app : incompleteApps) {
            User applicant = app.getApplicantName();
            try {
                // Send sms reminder
               // sendSmsReminder(applicant.getPhoneNumber(),
                 //   "Reminder: Your application is incomplete. Please log in to complete it.");

                // Send email reminder too
                sendEmailNotification(applicant.getUsername(), app.getId(),
                    applicant.getEmail(), "Rappel: Candidature Incomplète",
                    "Votre dossier est en attente de complétion. Merci de vous connecter.");

            } catch (MessagingException e) {
                System.err.println("Failed to send reminder to " + applicant.getEmail() + ": " + e.getMessage());
            }
        }

        System.out.println("Scheduler check for incomplete applications executed at: " + LocalDateTime.now());
    }

}