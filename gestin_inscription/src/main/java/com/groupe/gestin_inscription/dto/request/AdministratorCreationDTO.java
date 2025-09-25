package com.groupe.gestin_inscription.dto.request;

import com.groupe.gestin_inscription.model.Enums.AdministratorRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministratorCreationDTO {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private AdministratorRole role;
}