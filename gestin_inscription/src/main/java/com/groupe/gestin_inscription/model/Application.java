package com.groupe.gestin_inscription.model;

import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime submissionDate;
    private double completionRate;
    private LocalDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private Administrator assignedAdmin;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User applicantName;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    //private Set<Document> documents = new HashSet<>();
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<Notification> notifications = new ArrayList<>();

    // Getters and Setters
}
