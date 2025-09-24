package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.dto.request.RegistrationFormRequestDTO;
import com.groupe.gestin_inscription.dto.request.UserRequestDTO;
import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.repository.AdministratorRepository;
import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.repository.UserRepository;
import com.groupe.gestin_inscription.services.serviceInterfaces.ApplicationService;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdministratorRepository administratorRepository;
    @Autowired
    private DocumentServiceImpl documentService;
    @Autowired
    private NotificationServiceImpl notificationService;
    @Autowired
    private DocumentManagerService documentManagerService;

    // Creates a new application from user data and documents
    @Override
    public Application createApplication(RegistrationFormRequestDTO registrationForm, List<DocumentUploadRequestDTO> documents) throws MessagingException {
        // Step 1: Find the existing user by ID.
        User user = userRepository.findByUsername(registrationForm.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + registrationForm.getUsername()));

        // Step 2: Create a new Application entity associated with the retrieved user.
        Application application = new Application();
        application.setApplicantName(user);
        application.setCompletionRate(calculateCompletionRate(registrationForm, documents));
        application.setSubmissionDate(LocalDateTime.now());
        application.setStatus(ApplicationStatus.PRE_VALIDATION);
        application = applicationRepository.save(application);

        // Step 3: Upload and associate documents.
        for (DocumentUploadRequestDTO docDTO : documents) {
            documentService.uploadDocument(application.getId(), docDTO);
        }

        // Step 4: Trigger automatic pre-validation and notifications.
        performPreValidation(application);
        notificationService.sendEmailNotification(user.getEmail(), "Application Submitted", "Your application has been received.");

        return application;
    }

    // Retrieves an application by its ID
    public Application getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with ID: " + applicationId));
    }

    // Retrieves all applications for the admin dashboard
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    // Finds applications by status for filtering in the admin dashboard
    public List<Application> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status);
    }

    // Finds applications with a certain completion rate
    public List<Application> getApplicationsByCompletionRate(double rate) {
        return applicationRepository.findByCompletionRateGreaterThanEqual(rate);
    }

    // Performs automated pre-validation checks (2 min)
    @Override
    public void performPreValidation(Application application) {
        // Verification of document formats
        boolean docsValid = application.getDocuments().stream()
                .allMatch(doc -> documentManagerService.verifyFormat(doc.getFilePath(), (MultipartFile) doc));

        // Elementary fraud detection
        boolean noFraud = application.getDocuments().stream()
                .noneMatch(doc -> documentManagerService.performOcrCheck(doc.getFilePath()));

        if (docsValid && noFraud) {
            application.setStatus(ApplicationStatus.MANUAL_REVIEW);
            applicationRepository.save(application);
            assignForManualReview(application);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
            try {
                notificationService.sendEmailNotification(application.getApplicantName().getEmail(), "Application Rejected", "Your application failed pre-validation.");
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Assigns an application to an agent for manual review (24-48h)
    @Override
    public void assignForManualReview(Application application) {

        // Get all agents
        List<Administrator> agents = administratorRepository.findByRole(AdministratorRole.AGENT);
        if (agents.isEmpty()) {
            // Handle case with no agents
            return;
        }

        // Simple logic for random assignment
        Random random = new Random();
        Administrator agent = agents.get(random.nextInt(agents.size()));

        // You would typically link the application to the agent here
        application.setAssignedAdmin(agent);
        applicationRepository.save(application);

    }

    // Agent's action to manually validate or reject a dossier
    @Transactional
    @Override
    public void reviewDossier(Long applicationId, String reviewDecision) throws MessagingException {
        Application application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new NoSuchElementException("Application with ID " + applicationId + " not found.")
        );

        User applicant = application.getApplicantName();

        String emailSubject;
        String emailBody;

        if ("approve".equalsIgnoreCase(reviewDecision)) {
            application.setStatus(ApplicationStatus.APPROVED);
            emailSubject = "Application Approved";
            emailBody = "Congratulations! Your application has been approved.";
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            emailSubject = "Application Rejected";
            emailBody = "We regret to inform you that your application has been rejected.";
        }

        applicationRepository.save(application);

        // Send multi-channel notifications
        notificationService.sendEmailNotification(applicant.getEmail(), emailSubject, emailBody);
        notificationService.sendSmsReminder(applicant.getPhoneNumber(), emailBody);
        notificationService.sendInAppNotification(applicant.getId(), emailBody);
    }

    /**
     * Handles the online appeal process for a rejected application.
     */
    public void handleRecourse(Long applicationId, String recourseType) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> new EntityNotFoundException("Application not found."));

        if (application.getStatus() != ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Recourse is only possible for rejected applications.");
        }

        if ("appointment".equalsIgnoreCase(recourseType)) {
            // Logic for virtual appointment scheduling [cite: 76]
            try {
                notificationService.sendEmailNotification(application.getApplicantName().getEmail(), "Prise de rendez-vous", "Prenez un rendez-vous virtuel avec l'administration.");
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        } else if ("chat".equalsIgnoreCase(recourseType)) {
            // Logic for chat with administration [cite: 77]
            // ... enable chat functionality
        } else {
            throw new IllegalArgumentException("Invalid recourse type.");
        }
    }


     // Calculates the completion rate of an application based on submitted data.
    private double calculateCompletionRate(RegistrationFormRequestDTO userDTO, List<DocumentUploadRequestDTO> documents) {
        // Simple example: 50% for personal info, 50% for documents
        int totalFields = 2;
        int completedFields = 0;
        if (userDTO != null) completedFields++;
        if (documents != null && !documents.isEmpty()) completedFields++;
        return ((double) completedFields / totalFields) * 100;
    }
}
