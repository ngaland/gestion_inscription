package com.groupe.gestin_inscription.dto.response;

import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import com.groupe.gestin_inscription.model.Enums.UserRole;
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
    private String phoneNumber;
    private String address;
    private UserRole Role;

    public UserResponseDTO(Long id,
                           String username,
                           String firstName,
                           String lastName,
                           String email,
                           UserRole Role) {
        this.id=id;
        this.username=username;
        this.firstName=firstName;
        this.lastName=lastName;
        this.email=email;
        this.Role=Role;

    }
    // Getters and Setters
}
