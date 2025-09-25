package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import com.groupe.gestin_inscription.dto.request.AdministratorCreationDTO;
import com.groupe.gestin_inscription.dto.request.administratorRequestDTO;
import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.repository.AdministratorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdministratorServiceImpl {

    @Autowired
    private AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;

    public Administrator completeAdminProfile(String username, administratorRequestDTO profileDTO) {
        // 1. Find the existing, initialized admin record
        Administrator admin = administratorRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found."));

        // 2. Update the fields in the SAME record
        admin.setFirstName(profileDTO.getFirstName());
        admin.setLastName(profileDTO.getLastName());


        // 3. Save the SAME record back to the database
        return administratorRepository.save(admin);
    }



    // Use constructor injection for dependencies
    public AdministratorServiceImpl(AdministratorRepository administratorRepository, PasswordEncoder passwordEncoder) {
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // New method for creating a new Administrator (e.g., an AGENT)
    public Administrator createAgent(AdministratorCreationDTO creationDTO) {
        // 1. Input Validation (Check if username/email already exists - recommended)
        if (administratorRepository.existsByUserName(creationDTO.getUsername()) ||
                administratorRepository.existsByEmail(creationDTO.getEmail())) {
            throw new RuntimeException("Username or Email already taken!");
        }

        // 2. Build the new Administrator object
        Administrator newAgent = Administrator.builder()
                .userName(creationDTO.getUsername())
                .email(creationDTO.getEmail())
                // Set the specific role here
                .role(AdministratorRole.AGENT)
                // CRITICAL: Hash the password before saving
                .password(passwordEncoder.encode(creationDTO.getPassword()))
                .firstName(creationDTO.getFirstName())
                .lastName(creationDTO.getLastName())
                // Other fields will be null/default until they complete their profile
                .build();

        // 3. Save the new AGENT record
        return administratorRepository.save(newAgent);
    }
}
