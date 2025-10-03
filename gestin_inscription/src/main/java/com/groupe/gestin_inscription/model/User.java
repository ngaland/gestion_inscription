package com.groupe.gestin_inscription.model;


import com.groupe.gestin_inscription.config.SensitiveDataConverter;
import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import com.groupe.gestin_inscription.model.Enums.Gender;
import com.groupe.gestin_inscription.model.Enums.UserRole;
import jakarta.validation.constraints.Past;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Information
    @Convert(converter = SensitiveDataConverter.class)
    private String firstName;

    @Convert(converter = SensitiveDataConverter.class)
    private String lastName;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    @Convert(converter = SensitiveDataConverter.class)
    private String username;

    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Past(message = "Date de naissance doit être antérieure à aujourd'hui")
    private LocalDate dateOfBirth;
    private String nationality;
    private String emergencyContact;

    // Contact Information
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;
    private String email;

    @Convert(converter = SensitiveDataConverter.class)
    private String phoneNumber;

    @Convert(converter = SensitiveDataConverter.class)
    private String address;

    // Relationships
    @OneToOne(mappedBy = "applicantName", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Application applicationfile;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_history_id")
    private AcademicHistory academicHistory;

    // Getters and Setters
}




