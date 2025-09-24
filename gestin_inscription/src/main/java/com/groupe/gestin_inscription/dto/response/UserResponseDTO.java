package com.groupe.gestin_inscription.dto.response;

import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data

public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private AdministratorRole AdministratorRole;
    private String phoneNumber;
    private String address;

    public UserResponseDTO(Long id,
                           String username,
                           String firstName,
                           String lastName,
                           String email,
                           AdministratorRole administratorRole) {
        this.id=id;
        this.username=username;
        this.firstName=firstName;
        this.lastName=lastName;
        this.email=email;
        this.AdministratorRole=administratorRole;

    }
    // Getters and Setters
}
