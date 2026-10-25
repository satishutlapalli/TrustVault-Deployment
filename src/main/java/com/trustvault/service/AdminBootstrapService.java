package com.trustvault.service;

import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapService implements ApplicationRunner {
    private final UserRepository users;
    private final String adminUsername;

    public AdminBootstrapService(UserRepository users,
            @Value("${trustvault.admin.username:}") String adminUsername) {
        this.users = users;
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Promote only an explicitly configured, already-registered account.
        // Never allow public registration to choose its own role.
        if (adminUsername.isBlank()) return;
        users.findByUsername(adminUsername).ifPresent(user -> {
            if (!"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                users.save(user);
            }
        });
    }
}
