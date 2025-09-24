package com.groupe.gestin_inscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponseDTO {
    private Long notificationId;
    private String message;
    private String type; // e.g., "Email", "SMS", "In-app" [cite: 74, 73, 72]
    private LocalDateTime timestamp;
    private boolean isRead;
}
