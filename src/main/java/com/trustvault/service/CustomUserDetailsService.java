
package com.trustvault.service;

import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Value("${trustvault.admin.username:}")
    private String adminUsername;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        String role;

        if (adminUsername != null
                && !adminUsername.isBlank()
                && user.getUsername().equalsIgnoreCase(adminUsername.trim())) {
            role = "ADMIN";
        } else {
            role = user.getRole() == null
                    || user.getRole().isBlank()
                    ? "USER"
                    : user.getRole().replaceFirst("^ROLE_", "");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(role)
                .disabled(!user.isEnabled())
                .build();
    }
}
