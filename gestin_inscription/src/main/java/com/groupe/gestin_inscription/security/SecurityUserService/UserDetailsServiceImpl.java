package com.groupe.gestin_inscription.security.SecurityUserService;

import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.repository.AdministratorRepository;
import com.groupe.gestin_inscription.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(AdministratorRepository administratorRepository,
                                  UserRepository userRepository) {
        this.administratorRepository = administratorRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // finding an admin first
        Optional<Administrator> adminOptional = administratorRepository.findByUserName(username);
        if (adminOptional.isPresent()) {
            Administrator admin = adminOptional.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(admin.getUserName())
                    .password(admin.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name())))
                    .build();
        }

        // instead, finding a normal user
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    // a regular user has the CANDIDATE default ROLE
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                    .build();
        }

        // if no user is found
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
