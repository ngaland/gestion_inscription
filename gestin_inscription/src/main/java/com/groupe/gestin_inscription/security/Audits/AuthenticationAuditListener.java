package com.groupe.gestin_inscription.security.Audits;

import com.groupe.gestin_inscription.model.LoginAttempt;
import com.groupe.gestin_inscription.model.Enums.Status;
import com.groupe.gestin_inscription.repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuthenticationAuditListener {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;


     //Safely extracts the IP address from the Authentication object.
     //Handles cases where details might be null or not of the expected type.

    private String getIpAddress(Authentication authentication) {
        Object details = authentication.getDetails();

        // Check if details is null
        if (details == null) {
            return "Unknown";
        }

        // Check if details is WebAuthenticationDetails
        if (details instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails webDetails = (WebAuthenticationDetails) details;
            return webDetails.getRemoteAddress();
        }

        // For REST API calls or other authentication types
        return "API/System";
    }

    // Listener for successful login attempts
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        try {
            String username = event.getAuthentication().getName();
            String ipAddress = getIpAddress(event.getAuthentication());

            LoginAttempt attempt = new LoginAttempt(null, username, LocalDateTime.now(), ipAddress, Status.SUCCESS);
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            // Log the error but don't let it break the authentication flow
            System.err.println("Error saving successful login attempt: " + e.getMessage());
        }
    }

    // Listener for failed login attempts
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        try {
            // The principal might be null or a username string
            String username = event.getAuthentication().getPrincipal() != null
                    ? event.getAuthentication().getPrincipal().toString()
                    : "Unknown";
            String ipAddress = getIpAddress(event.getAuthentication());

            LoginAttempt attempt = new LoginAttempt(null, username, LocalDateTime.now(), ipAddress, Status.FAILURE);
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            // Log the error but don't let it break the authentication flow
            System.err.println("Error saving failed login attempt: " + e.getMessage());
        }
    }
}