package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.dto.request.RegistrationFormRequestDTO;
import com.groupe.gestin_inscription.dto.request.UserRequestDTO;
import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Document;
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
import java.util.*;

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


    // Nouvelle méthode qui utilise le profil utilisateur existant
    @Transactional
    public Application createApplicationFromExistingUser(String username, List<DocumentUploadRequestDTO> documents)
            throws MessagingException {

        // 1. retrieve the existing user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found with username: " + username));

        // 2. Verify if there is an existing application for the current user
        List<Application> existingApplications = applicationRepository.findByApplicantName(user);
        boolean hasActiveApplication = existingApplications.stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.PRE_VALIDATION ||
                        app.getStatus() == ApplicationStatus.MANUAL_REVIEW);

        if (hasActiveApplication) {
            throw new IllegalStateException("User already has an active application. Please wait for it to be processed.");
        }

        // 3. Create a new application with the existing profile infos
        Application application = new Application();
        application.setApplicantName(user);
        application.setCompletionRate(calculateCompletionRateFromExistingUser(user, documents));
        application.setSubmissionDate(LocalDateTime.now());
        application.setStatus(ApplicationStatus.PRE_VALIDATION);

        // Save the application AND FLUSH to database immediately
        application = applicationRepository.saveAndFlush(application);

        System.out.println("=== createApplicationFromExistingUser ===");
        System.out.println("Documents received: " + (documents != null ? documents.size() : "null"));

//        // 4. manage uploaded documents
//        for (DocumentUploadRequestDTO docDTO : documents) {
//            documentService.uploadDocument(application.getId(), docDTO);
//        }
//
//        // Reload the entity to ensure the 'documents' collection is fetched and populated by JPA
//        application = applicationRepository.findById(application.getId())
//                .orElseThrow(() -> new RuntimeException("Application not found after saving documents."));

        // 4. manage uploaded documents - NOW the application ID exists in DB
        for (DocumentUploadRequestDTO docDTO : documents) {
            try {
                Document savedDoc = documentService.uploadDocument(application.getId(), docDTO);
                System.out.println("Document saved with ID: " + savedDoc.getId());
            } catch (Exception e) {
                System.err.println("Error uploading document: " + e.getMessage());
                throw e;
            }
        }

        // 5. perform automatic pre-validation and notifications
        performPreValidation(application);
        notificationService.sendEmailNotification(
                user.getUsername(), // Pass userId
                application.getId(), // Pass applicationId
                user.getEmail(),
                "Candidature soumise",
                "Votre candidature a été reçue et est en cours de traitement."
        );

        return application;
    }

    // Creates a new application from user data and documents
    @Transactional
    @Override
    public Application createApplication(RegistrationFormRequestDTO registrationForm, List<DocumentUploadRequestDTO> documents) throws MessagingException {
        // Step 1: Find the existing user by username.
        User user = userRepository.findByUsername(registrationForm.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found with Username: " + registrationForm.getUsername()));

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
        notificationService.sendEmailNotification(user.getUsername(), application.getId(), user.getEmail(), "Application Submitted", "Your application has been received.");


        return application;
    }

    // computing the completionRate based on the existing user profile
    private double calculateCompletionRateFromExistingUser(User user, List<DocumentUploadRequestDTO> documents) {
        double completionRate = 0.0;
        double totalWeight = 100.0;

        // Verify profil infos (60% du total)
        double profileWeight = 60.0;
        double profileScore = 0.0;

        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) profileScore += 10;
        if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) profileScore += 10;
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) profileScore += 10;
        if (user.getDateOfBirth() != null) profileScore += 10;
        if (user.getNationality() != null && !user.getNationality().trim().isEmpty()) profileScore += 5;
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) profileScore += 5;
        if (user.getAddress() != null && !user.getAddress().trim().isEmpty()) profileScore += 5;
        if (user.getEmergencyContact() != null && !user.getEmergencyContact().trim().isEmpty()) profileScore += 5;

        completionRate += (profileScore / 60.0) * profileWeight;

        // verify academic history (20% du total)
        double academicWeight = 20.0;
        if (user.getAcademicHistory() != null) {
            double academicScore = 0.0;
            if (user.getAcademicHistory().getLastInstitution() != null) academicScore += 10;
            if (user.getAcademicHistory().getSpecialization() != null) academicScore += 5;
            if (user.getAcademicHistory().getStartDate() != null) academicScore += 2.5;
            if (user.getAcademicHistory().getEndDate() != null) academicScore += 2.5;

            completionRate += (academicScore / 20.0) * academicWeight;
        }

        // verify documents (20% du total)
        double documentsWeight = 20.0;
        if (documents != null && !documents.isEmpty()) {
            // Score basé sur le nombre de documents fournis (estimation)
            double documentScore = Math.min(documents.size() * 5.0, 20.0);
            completionRate += (documentScore / 20.0) * documentsWeight;
        }

        return Math.min(completionRate, 100.0);
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
        boolean docsValid = true;
        boolean noFraud = true;

        // Iterate over all uploaded documents to perform specific checks
        for (Document doc : application.getDocuments()) {
            String filePath = doc.getFilePath(); //returns the storage path
            String documentType = doc.getFileType(); // the stored type

            // 1. Basic Format/Size Check (done in DocumentManagerService.verifyFormat)
            // If the main format check fails, we stop.
            // verifyFormat is called at upload time, re-verify specialized aspects here.

            // 2. OCR Partiel (Required for "Relevés de notes")
            if ("Relevés de notes".equalsIgnoreCase(documentType)) {
                // Check for elementary fraud in the OCR content
                if (!documentManagerService.performOcrCheck(filePath)) {
                    noFraud = false;
                    System.out.println("Fraud detected via OCR check for: " + documentType);
                }
            }

            // 3. Détection de filigrane (Required for "Acte de naissance")
            if ("Acte de naissance".equalsIgnoreCase(documentType)) {
                if (!documentManagerService.detectWatermark(filePath, documentType)) {
                    noFraud = false;
                    System.out.println("Watermark missing or tampered for: " + documentType);
                }
            }
        }

        // ALERT FOR UPLOADS SUSPECTS
        if (docsValid && noFraud) {
            application.setStatus(ApplicationStatus.MANUAL_REVIEW);
            applicationRepository.save(application);
            assignForManualReview(application);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(application);
            if (!noFraud) {
                // Send a high-priority in-app/email alert to a SUPER_ADMIN
                // Placeholder: log to alert system
                System.err.println("!!! SECURITY ALERT: Application " + application.getId() + " rejected due to suspicious document uploads.");
            }

            try {
                notificationService.sendEmailNotification(application.getApplicantName().getUsername(), application.getId(), application.getApplicantName().getEmail(), "Application Rejected", "Your application failed pre-validation.");
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
            System.err.println("CRITICAL: No ADMINISTRATOR with the AGENT role found for assignment.");
            return;
        }

        // 2. FIND THE LEAST BUSY AGENT using the optimized JPQL query
        Administrator leastBusyAgent = applicationRepository.findLeastBusyAgent()
                .orElseGet(() -> {
                    // Fallback: If the query returns no applications (e.g., system is empty),
                    // pick the first agent in the list for initial distribution.
                    return agents.get(0);
                });

        // Simple logic for random assignment
       // Random random = new Random();
        //Administrator agent = agents.get(random.nextInt(agents.size()));


        System.out.println("Assigning application " + application.getId() + " to agent: " + leastBusyAgent.getUserName());

        // link the application to the agent and save
        application.setAssignedAdmin(leastBusyAgent);
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
        notificationService.sendEmailNotification(application.getApplicantName().getUsername(), application.getId(), applicant.getEmail(), emailSubject, emailBody);
        //notificationService.sendSmsReminder(applicant.getPhoneNumber(), emailBody);
        notificationService.sendInAppNotification(applicant.getId(), emailBody);
    }

    /**
     * Handles the online appeal process for a rejected application.
     */
    public void handleRecourse(Long applicationId, String recourseType) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found."));

        if (application.getStatus() != ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Recourse is only possible for rejected applications.");
        }

        if ("appointment".equalsIgnoreCase(recourseType)) {
            //Update application status (optional: to "PENDING_RECOURSE")
            application.setStatus(ApplicationStatus.PENDING_RECOURSE);
            applicationRepository.save(application);
            try {
                notificationService.sendEmailNotification(application.getApplicantName().getUsername(), application.getId(), application.getApplicantName()
                        .getEmail(), "Prise de rendez-vous", "Prenez un rendez-vous virtuel avec l'administration.");
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        } else if ("chat".equalsIgnoreCase(recourseType)) {
            // Logic for chat with administration

            // 1. Get the Applicant's User ID
            Long applicantUserId = application.getApplicantName().getId();

            // 2. Define the in-app message
            String chatMessage = "Votre demande de recours par chat a été enregistrée. Un agent de l'administration sera notifié pour démarrer une session de chat en direct. Veuillez rester attentif à vos notifications in-app.";

            // 3. Send the in-app notification
            notificationService.sendInAppNotification(applicantUserId, chatMessage);

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
