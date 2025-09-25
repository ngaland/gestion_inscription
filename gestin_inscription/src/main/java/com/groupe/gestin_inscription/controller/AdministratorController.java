package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.dto.request.AdministratorCreationDTO;
import com.groupe.gestin_inscription.dto.request.administratorRequestDTO;
import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.security.Utils.ObjectLevelSecurity;
import com.groupe.gestin_inscription.services.serviceImpl.AdministratorServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@Tag(name = "Admin Profile")

public class AdministratorController {

    @Autowired
    private AdministratorServiceImpl administratorService;
    private ObjectLevelSecurity objectLevelSecurity;

    @Operation(summary = "Completes the profile for the currently authenticated administrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not authorized")
    })
    @PutMapping("/complete")
    // Ensure only an authenticated ADMIN can hit this endpoint
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Administrator> completeProfile(@RequestBody administratorRequestDTO profileDTO) {

        // Get the ID of the currently authenticated user from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentAdminUsername = authentication.getName(); // assuming username is stored

        Administrator updatedAdmin = administratorService.completeAdminProfile(currentAdminUsername, profileDTO);

        return ResponseEntity.ok(updatedAdmin);
    }

    @Operation(
            summary = "Create a new Agent account",
            description = "Allows a Super Admin to create a new Administrator account with the 'AGENT' role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Agent account created successfully",
                    content = @Content(schema = @Schema(implementation = Administrator.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., email already exists)"),
            @ApiResponse(responseCode = "403", description = "Forbidden: User must have the 'SUPER_ADMIN' role")
    })
    @PostMapping("/agent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Administrator> createNewAgent(@RequestBody AdministratorCreationDTO creationDTO) {
        Administrator newAgent = administratorService.createAgent(creationDTO);
        return ResponseEntity.ok(newAgent);
    }
}
