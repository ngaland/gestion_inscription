package com.groupe.gestin_inscription.dto.response;

import com.groupe.gestin_inscription.model.Enums.NotificationStatus;
import com.groupe.gestin_inscription.model.Enums.NotificationType;
import com.groupe.gestin_inscription.model.Notification;
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
    private String type; // e.g., "Email", "SMS", "In-app"
    private LocalDateTime timestamp;
    private boolean isRead;


    public NotificationResponseDTO(Notification notif) {
        this.notificationId = notif.getId();
        this.message = notif.getMessage();
        this.type = String.valueOf(notif.getType());
        this.timestamp = notif.getCreatedAt();
        this.isRead = notif.getStatus() == NotificationStatus.READ;
    }

}
