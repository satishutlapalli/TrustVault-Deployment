
package com.trustvault.controller;

import com.trustvault.model.Document;
import com.trustvault.model.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.DocumentShareRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.ActivityLogService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final DocumentRepository documents;
    private final DocumentShareRepository shares;
    private final Path uploadDirectory;
    private final ActivityLogService activityLogService;

    public AdminController(
            UserRepository users,
            DocumentRepository documents,
            DocumentShareRepository shares,
            @Value("${trustvault.upload-dir}") String uploadDir,
            ActivityLogService activityLogService) throws IOException {

        this.users = users;
        this.documents = documents;
        this.shares = shares;
        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
        this.activityLogService = activityLogService;

        Files.createDirectories(this.uploadDirectory);
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return Map.of(
                "userCount", users.count(),
                "documentCount", documents.count(),
                "shareCount", shares.count()
        );
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return users.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(user -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", user.getId());
                    data.put("username", user.getUsername());
                    data.put("email", user.getEmail());
                    data.put("role", user.getRole());
                    data.put("createdAt", user.getCreatedAt());
                    data.put("enabled", user.isEnabled());
                    return data;
                })
                .toList();
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> getDocuments() {
        return documents.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(document -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", document.getId());
                    data.put("filename", document.getOriginalFilename());
                    data.put(
                            "owner",
                            document.getOwner() == null
                                    ? "unknown"
                                    : document.getOwner().getUsername()
                    );
                    data.put("size", document.getFileSize());
                    data.put("uploadedAt", document.getUploadedAt());
                    return data;
                })
                .toList();
    }

    @PatchMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {

        Boolean enabled = request.get("enabled");

        if (enabled == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The enabled field is required."
            );
        }

        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (authentication != null
                && user.getUsername().equals(authentication.getName())
                && !enabled) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot disable your own account."
            );
        }

        user.setEnabled(enabled);
        users.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("enabled", user.isEnabled());
        response.put(
                "message",
                enabled ? "User enabled." : "User disabled."
        );

        return response;
    }

    @DeleteMapping("/documents/{id}")
    @Transactional
    public Map<String, String> deleteAnyDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document document = documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."
                ));

        Path filePath = uploadDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        if (!filePath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stored file path."
            );
        }

        String filename = document.getOriginalFilename();
        String adminUsername = authentication.getName();

        try {
            // Delete the physical file if it exists.
            Files.deleteIfExists(filePath);

            // Remove shares referencing this document.
            shares.deleteByDocument_Id(id);

            // Remove the document record.
            documents.delete(document);

            // Record the administrator action.
            activityLogService.logActivity(
                    adminUsername,
                    "ADMIN_DELETE_DOCUMENT",
                    filename == null ? "Unknown file" : filename
            );

            return Map.of(
                    "message",
                    "Document deleted successfully."
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete the stored file."
            );
        }
    }
}
