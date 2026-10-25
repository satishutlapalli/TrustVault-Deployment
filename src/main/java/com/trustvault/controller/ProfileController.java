
package com.trustvault.controller;

import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.repository.DocumentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public ProfileController(
            UserRepository userRepository,
            DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        long documentCount = documentRepository
                .findByOwner_UsernameOrderByUploadedAtDesc(
                        authentication.getName())
                .size();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("documentCount", documentCount);

        return ResponseEntity.ok(profile);
    }
}
