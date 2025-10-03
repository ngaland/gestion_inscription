package com.groupe.gestin_inscription.model;

import com.groupe.gestin_inscription.model.Enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDateTime timestamp;

    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private Status status; // SUCCESS or FAILURE


}