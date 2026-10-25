
package com.trustvault.controller;

import com.trustvault.model.User;
import com.trustvault.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (request == null
                || request.username() == null
                || request.email() == null
                || request.password() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "All fields are required."));
        }

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Username must be 3–30 characters and contain only letters, numbers, or underscores."));
        }

        if (email.length() > 254
                || !EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Enter a valid email address."));
        }

        // BCrypt has a 72-byte input limit.
        if (password.length() < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Password must be at least 8 characters and no more than 72 UTF-8 bytes."));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Username already exists."));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Email already exists."));
        }

        User user = new User(
                username,
                email,
                passwordEncoder.encode(password)
        );

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message",
                        "Account created successfully. You can now log in."));
    }

    public record RegisterRequest(
            String username,
            String email,
            String password) {
    }
}
