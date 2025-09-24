package com.groupe.gestin_inscription.security.Utils;

import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Document;
import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("ObjectLevelSecurity")
@AllArgsConstructor

public class ObjectLevelSecurity {

    public boolean canAccessApplication(Application application) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Administrator admin = (Administrator) authentication.getPrincipal(); // Assuming the Principal is an Administrator object

        if (admin.getRole().equals(AdministratorRole.SUPER_ADMIN)) {
            return true; // Super-admin can access any application
        }

        if (admin.getRole().equals(AdministratorRole.AGENT)) {
            // Check if the application is assigned to the agent
            return application.getAssignedAdmin().equals(admin.getId());
        }

        return false;
    }

    public boolean canValidateDocument(Document document) {
        // Only agents and super-admins can validate documents
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Administrator admin = (Administrator) authentication.getPrincipal();

        if (admin.getRole().equals(AdministratorRole.SUPER_ADMIN) || admin.getRole().equals(AdministratorRole.AGENT)) {
            return true;
        }

        return false;
    }
}
