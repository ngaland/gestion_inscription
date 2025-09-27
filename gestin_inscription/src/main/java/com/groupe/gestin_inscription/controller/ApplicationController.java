package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.dto.request.RegistrationFormRequestDTO;
import com.groupe.gestin_inscription.dto.response.ApplicationStatusResponseDto;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.security.Utils.ObjectLevelSecurity;
import com.groupe.gestin_inscription.services.serviceImpl.ApplicationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
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

    // Endpoint for applicants to submit a new application
    @Operation(summary = "Submit a new application")
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationStatusResponseDto> submitApplication(@RequestBody RegistrationFormRequestDTO registrationFormRequestDTO, List<DocumentUploadRequestDTO> documentUploadRequestDTO) throws MessagingException, IOException {
        Application newApplication = applicationServiceImpl.createApplication(registrationFormRequestDTO, documentUploadRequestDTO);
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
    @PreAuthorize("hasAuthority('AGENT')")
    public ResponseEntity<Void> reviewApplication(@PathVariable Long applicationId, @RequestParam("decision") String reviewDecision) {
        try {
            applicationServiceImpl.reviewDossier(applicationId, reviewDecision);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }

    // Endpoint for a super-admin to view all applications
    @Operation(summary = "Get a list of all applications for a Super Admin")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
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

        // Populate applicant information
        User applicant = application.getApplicantName();
        if (applicant != null) {
            dto.setUserIdNum(applicant.getUserIdNum());
            dto.setUsername(applicant.getUsername());
            dto.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        }

        return dto;
    }
}
