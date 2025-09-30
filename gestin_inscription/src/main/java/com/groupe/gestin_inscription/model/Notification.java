package com.groupe.gestin_inscription.model;

import com.groupe.gestin_inscription.model.Enums.NotificationStatus;
import com.groupe.gestin_inscription.model.Enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    @CreatedDate
    private LocalDateTime createdAt;


    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    // Getters and Setters
}