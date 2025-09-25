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

@Service
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdministratorRepository administratorRepository;

    public UserDetailsServiceImpl(AdministratorRepository administratorRepository) {

        this.administratorRepository = administratorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Administrator user = administratorRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Create GrantedAuthorities from the user's role
        //Collection<? extends GrantedAuthority> authorities =
        //        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Building Spring Security's UserDetails object from your User entity
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword()) // This is the HASHED password from DB
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))) // Map your Role enum to SimpleGrantedAuthority
                .build();

    }
}
