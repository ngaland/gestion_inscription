package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.dto.response.DocumentResponseDTO;
import com.groupe.gestin_inscription.dto.request.RegistrationFormRequestDTO;
import com.groupe.gestin_inscription.dto.response.ApplicationStatusResponseDto;
import com.groupe.gestin_inscription.dto.response.NotificationResponseDTO;
import com.groupe.gestin_inscription.model.*;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.repository.DocumentRepository;
import com.groupe.gestin_inscription.repository.NotificationRepository;
import com.groupe.gestin_inscription.security.Utils.ObjectLevelSecurity;
import com.groupe.gestin_inscription.services.serviceImpl.ApplicationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Application Management", description = "Endpoints for managing the application submission workflow")
public class ApplicationController {

    @Autowired
    private ApplicationServiceImpl applicationServiceImpl;
    @Autowired
    private ObjectLevelSecurity objectLevelSecurity;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    // Endpoint for applicants to submit a new application using their existing profile
    @Operation(summary = "Submit a new application using existing user profile")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationStatusResponseDto> submitApplication(
            @RequestParam(value = "documentNames", required = false) List<String> documentNames,
            @RequestParam(value = "documentTypes", required = false) List<String> documentTypes,
            @RequestParam(value = "files", required = false) List<MultipartFile> files)
            throws MessagingException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Construire la liste des DocumentUploadRequestDTO à partir des paramètres
        List<DocumentUploadRequestDTO> documentsDTOs = new ArrayList<>();

        System.out.println("Files received: " + (files != null ? files.size() : "null"));
        System.out.println("Document names: " + documentNames);
        System.out.println("Document types: " + documentTypes);

        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                DocumentUploadRequestDTO dto = new DocumentUploadRequestDTO();
                dto.setFileContent(files.get(i));

                if (documentNames != null && i < documentNames.size()) {
                    dto.setName(documentNames.get(i));
                }
                if (documentTypes != null && i < documentTypes.size()) {
                    dto.setDocumentType(documentTypes.get(i));
                }

                documentsDTOs.add(dto);
            }
        }

        System.out.println("Total DTOs created: " + documentsDTOs.size());

        Application newApplication = applicationServiceImpl.createApplicationFromExistingUser(
                currentUsername,
                documentsDTOs
        );

        return ResponseEntity.ok(convertToDto(newApplication));
    }

    // Endpoint for applicants to check their application status
    @Operation(summary = "Get applications by status for the current user")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<ApplicationStatusResponseDto>> getApplicationsByStatus(
            @PathVariable ApplicationStatus status) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        List<Application> applications = applicationServiceImpl.getApplicationsByStatus(status);

        // Filter applications for the current user only
        List<Application> userApplications = applications.stream()
                .filter(app -> app.getApplicantName() != null &&
                        currentUsername.equals(app.getApplicantName().getUsername()))
                .collect(Collectors.toList());

        List<ApplicationStatusResponseDto> responseDtos = userApplications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }


    // Endpoint for an agent to manually review an application
    @Operation(summary = "review an as assigned admin application")
    @PutMapping("/review/{applicationId}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Void> reviewApplication(@PathVariable Long applicationId, @RequestParam("decision") String reviewDecision) {
        try {
            applicationServiceImpl.reviewDossier(applicationId, reviewDecision);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Initiates a recourse process for a REJECTED application")
    @PutMapping("/recourse/{applicationId}")
    @PreAuthorize("hasRole('CANDIDATE')") // Only the applicant should initiate recourse
    public ResponseEntity<Void> handleRecourse(
            @PathVariable Long applicationId,
            @RequestParam("type") String recourseType) {

        // You should ideally verify that the application belongs to the current principal user.
        // For now, we rely on the service logic and security setup.

        try {
            // Call the service method
            applicationServiceImpl.handleRecourse(applicationId, recourseType);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // Application not REJECTED
            return ResponseEntity.status(403).build(); // 403 Forbidden/Conflict status
        } catch (IllegalArgumentException e) {
            // Invalid recourse type
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        } catch (RuntimeException e) {
            // Handle MessagingException wrapped in RuntimeException
            if (e.getCause() instanceof MessagingException) {
                // Log the error
            }
            throw e; // Or handle more gracefully
        }
    }

    // Endpoint for a super-admin to view all applications
    @Operation(summary = "Get a list of all applications for a Super Admin")
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ApplicationStatusResponseDto>> getAllApplications() {
        List<Application> applications = applicationServiceImpl.getAllApplications();
        return ResponseEntity.ok(applications.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    // Private helper method to convert a single Application entity to a DTO
    private ApplicationStatusResponseDto convertToDto(Application application) {
        ApplicationStatusResponseDto dto = new ApplicationStatusResponseDto();
        dto.setApplicationId(application.getId());
        dto.setStatus(application.getStatus().name());
        dto.setCompletionRate(application.getCompletionRate());
        dto.setSubmissionDate(application.getSubmissionDate());

        Administrator assignedAdmin = application.getAssignedAdmin();
        // Conditional assignment to prevent NullPointerException if no agent is assigned yet
        if (assignedAdmin != null) {
            dto.setAssignedAdminId(assignedAdmin.getId());
            dto.setAssignedAdminUsername(assignedAdmin.getUserName());
        } else {
            // Explicitly set to null or a default value
            dto.setAssignedAdminId(null);
            dto.setAssignedAdminUsername("Not assigned");
        }

        // Populate applicant information
        User applicant = application.getApplicantName();
        if (applicant != null) {
            dto.setUsername(applicant.getUsername());
            dto.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        }

       // Populate documentsStatus
        List<Document> documents = documentRepository.findByApplicationId(application.getId());
        List<DocumentResponseDTO> docStatusList = documents.stream()
                .map(DocumentResponseDTO::new)
                .collect(Collectors.toList());
        dto.setDocumentsStatus(docStatusList);

        // Populate recentNotifications
        List<Notification> notifications = notificationRepository.findTop5ByUserOrderByCreatedAtDesc(application.getApplicantName());
        List<NotificationResponseDTO> notifList = notifications.stream()
                .map(NotificationResponseDTO::new)
                .collect(Collectors.toList());
        dto.setRecentNotifications(notifList);

        return dto;
    }
}
