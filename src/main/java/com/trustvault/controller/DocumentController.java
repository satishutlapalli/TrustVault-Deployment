
package com.trustvault.controller;

import com.trustvault.model.Document;
import com.trustvault.model.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.DocumentShareRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.ActivityLogService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of(".pdf", ".txt", ".png", ".jpg", ".jpeg");

    private final Path uploadDirectory;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentShareRepository documentShareRepository;
    private final ActivityLogService activityLogService;

    public DocumentController(
            @Value("${trustvault.upload-dir}") String uploadDir,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DocumentShareRepository documentShareRepository,
            ActivityLogService activityLogService) throws IOException {

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentShareRepository = documentShareRepository;
        this.activityLogService = activityLogService;

        Files.createDirectories(this.uploadDirectory);
    }

    // List documents belonging to the logged-in user.
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyDocuments(
            Authentication authentication) {

        List<Document> documents =
                documentRepository.findByOwner_UsernameOrderByUploadedAtDesc(
                        authentication.getName());

        List<Map<String, Object>> result = documents.stream()
                .map(document -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", document.getId());
                    item.put("filename", document.getOriginalFilename());
                    item.put("contentType", document.getContentType());
                    item.put("fileSize", document.getFileSize());
                    item.put("uploadedAt", document.getUploadedAt());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    // Upload and validate a document.
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please select a file."));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "File must be 10 MB or smaller."));
        }

        String submittedName = file.getOriginalFilename();

        if (submittedName == null || submittedName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid filename."));
        }

        // Strip any submitted directory path.
        String originalName = submittedName.replace('\\', '/');
        originalName = originalName.substring(
                originalName.lastIndexOf('/') + 1);

        if (originalName.isBlank()
                || originalName.equals(".")
                || originalName.equals("..")
                || originalName.length() > 255
                || originalName.contains("\r")
                || originalName.contains("\n")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid filename."));
        }

        int dot = originalName.lastIndexOf('.');

        if (dot <= 0 || dot == originalName.length() - 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "The file must have an allowed extension."));
        }

        String extension =
                originalName.substring(dot).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Only PDF, TXT, PNG, JPG and JPEG files are allowed."));
        }

        final byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Could not read the uploaded file."));
        }

        if (fileBytes.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Empty files are not allowed."));
        }

        if (fileBytes.length > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "File must be 10 MB or smaller."));
        }

        if (!hasValidContent(extension, fileBytes)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "File content does not match its extension."));
        }

        String storedName = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(storedName).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid filename."));
        }

        try {
            // Do not overwrite an existing stored file.
            Files.write(
                    destination,
                    fileBytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);

            User owner = userRepository
                    .findByUsername(authentication.getName())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Logged-in user not found"));

            Document document = new Document();
            document.setOwner(owner);
            document.setOriginalFilename(originalName);
            document.setStoredFilename(storedName);
            document.setContentType(getSafeContentType(extension));
            document.setFileSize((long) fileBytes.length);
            document.setUploadedAt(LocalDateTime.now());

            documentRepository.save(document);

            activityLogService.logActivity(
                    authentication.getName(),
                    "UPLOAD",
                    originalName);

            return ResponseEntity.ok(Map.of(
                    "message", "File saved successfully.",
                    "filename", originalName,
                    "storedId", storedName));

        } catch (IOException e) {
            deleteStoredFileQuietly(destination);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not save the file."));
        } catch (RuntimeException e) {
            deleteStoredFileQuietly(destination);
            throw e;
        }
    }

    // Basic signature checks. These do not prove a file is malware-free.
    private boolean hasValidContent(String extension, byte[] data) {

        if (data == null || data.length == 0) {
            return false;
        }

        if (extension.equals(".pdf")) {
            return data.length >= 5
                    && data[0] == '%'
                    && data[1] == 'P'
                    && data[2] == 'D'
                    && data[3] == 'F'
                    && data[4] == '-';
        }

        if (extension.equals(".png")) {
            byte[] signature = {
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            };

            if (data.length < signature.length) {
                return false;
            }

            for (int i = 0; i < signature.length; i++) {
                if (data[i] != signature[i]) {
                    return false;
                }
            }

            return true;
        }

        if (extension.equals(".jpg") || extension.equals(".jpeg")) {
            return data.length >= 3
                    && (data[0] & 0xFF) == 0xFF
                    && (data[1] & 0xFF) == 0xD8
                    && (data[2] & 0xFF) == 0xFF;
        }

        if (extension.equals(".txt")) {
            for (byte value : data) {
                if (value == 0) {
                    return false;
                }
            }

            try {
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(data));

                return true;
            } catch (CharacterCodingException e) {
                return false;
            }
        }

        return false;
    }

    private String getSafeContentType(String extension) {
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private void deleteStoredFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Preserve the original error.
        }
    }

    private boolean canRead(Document document, String username) {
        if (document.getOwner() != null && document.getOwner().getUsername().equals(username)) return true;
        return documentShareRepository.existsByDocument_IdAndRecipient_Username(document.getId(), username);
    }

    // Shared users can read/preview/download, but deletion remains owner-only.
    // Preview a supported document, only for its owner.
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletResponse servletResponse) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!canRead(document, authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String filename = document.getOriginalFilename() == null
                ? ""
                : document.getOriginalFilename();

        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        String contentType;

        if (lowerFilename.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (lowerFilename.endsWith(".txt")) {
            contentType = "text/plain";
        } else if (lowerFilename.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerFilename.endsWith(".jpg")
                || lowerFilename.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Preview supports PDF, TXT, PNG, JPG and JPEG files only.");
        }

        Path filePath = uploadDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        if (!filePath.startsWith(uploadDirectory)
                || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            servletResponse.setHeader(
                    "X-Content-Type-Options", "nosniff");
            servletResponse.setHeader(
                    "Cache-Control", "private, no-store");

            activityLogService.logActivity(
                    authentication.getName(),
                    "PREVIEW",
                    filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + filename.replace("\"", "")
                                            .replace("\r", "")
                                            .replace("\n", "")
                                    + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "private, no-store")
                    .header("Content-Security-Policy", "default-src 'none'")
                    .body(resource);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not preview the document.");
        }
    }

    // Download a document only if it belongs to the logged-in user.
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!canRead(document, authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Path filePath = uploadDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        if (!filePath.startsWith(uploadDirectory)
                || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            String filename = document.getOriginalFilename() == null
                    ? "document"
                    : document.getOriginalFilename()
                            .replace("\"", "")
                            .replace("\r", "")
                            .replace("\n", "");

            activityLogService.logActivity(
                    authentication.getName(),
                    "DOWNLOAD",
                    filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "private, no-store")
                    .header("Content-Security-Policy", "default-src 'none'")
                    .body(resource);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not download the file.");
        }
    }

    // Delete a document only if it belongs to the logged-in user.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (document.getOwner() == null
                || !document.getOwner().getUsername().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Path filePath = uploadDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        if (!filePath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String deletedFilename = document.getOriginalFilename();

        try {
            Files.deleteIfExists(filePath);
            documentRepository.delete(document);

            activityLogService.logActivity(
                    authentication.getName(),
                    "DELETE",
                    deletedFilename);

            return ResponseEntity.ok(Map.of(
                    "message", "Document deleted successfully."));

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete the stored file.");
        }
    }
}
