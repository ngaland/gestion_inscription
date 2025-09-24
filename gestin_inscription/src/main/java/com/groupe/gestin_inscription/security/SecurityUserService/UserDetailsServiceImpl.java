package com.groupe.gestin_inscription.security.SecurityUserService;

import com.groupe.gestin_inscription.model.User;
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

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Create GrantedAuthorities from the user's role
        //Collection<? extends GrantedAuthority> authorities =
        //        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Building Spring Security's UserDetails object from your User entity
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // This is the HASHED password from DB
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getAdministratorRole().name()))) // Map your Role enum to SimpleGrantedAuthority
                .build();

    }
}
