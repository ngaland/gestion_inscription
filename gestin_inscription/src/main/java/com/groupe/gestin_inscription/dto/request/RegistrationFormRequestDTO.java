package com.groupe.gestin_inscription.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@AllArgsConstructor
@NoArgsConstructor
@Data

public class RegistrationFormRequestDTO {
    // Section 1: Personal Information
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private String nationality;
    private String username;

    // Section 2: Academic History
    private String lastInstitution;
    private String specialization;
    private LocalDate trainingPeriodStart;
    private LocalDate trainingPeriodEnd;

    // Section 4: Contact Information
    private String email;
    private String phoneNumber;
    private String address;
    private String emergencyContact;

    // Getters and Setters
}