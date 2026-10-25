package com.trustvault.controller;

import com.trustvault.model.*;
import com.trustvault.repository.*;
import com.trustvault.service.ActivityLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/shares")
public class DocumentShareController {
    private final DocumentRepository documents; private final UserRepository users;
    private final DocumentShareRepository shares; private final ActivityLogService activity;
    public DocumentShareController(DocumentRepository documents, UserRepository users, DocumentShareRepository shares, ActivityLogService activity) {
        this.documents=documents; this.users=users; this.shares=shares; this.activity=activity;
    }
    @PostMapping
    @Transactional
    public Map<String,String> share(@RequestBody ShareRequest req, Authentication auth) {
        if (req == null || req.documentId() == null || req.recipientUsername() == null || req.recipientUsername().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document and recipient are required.");
        Document doc = documents.findById(req.documentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (doc.getOwner()==null || !doc.getOwner().getUsername().equals(auth.getName())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        User recipient = users.findByUsername(req.recipientUsername().trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered recipient not found."));
        if (recipient.getUsername().equals(auth.getName())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already own this document.");
        DocumentShare share = shares.findByDocument_IdAndRecipient_Username(doc.getId(), recipient.getUsername()).orElseGet(DocumentShare::new);
        share.setDocument(doc); share.setRecipient(recipient); share.setSharedBy(doc.getOwner()); share.setPermission("VIEWER"); shares.save(share);
        activity.logActivity(auth.getName(), "SHARE", doc.getOriginalFilename());
        return Map.of("message", "Document shared with " + recipient.getUsername() + " as Viewer.");
    }
    @GetMapping("/with-me")
    public List<Map<String,Object>> sharedWithMe(Authentication auth) {
        return shares.findByRecipient_UsernameOrderByCreatedAtDesc(auth.getName()).stream().map(s -> {
            Document d=s.getDocument(); Map<String,Object> m=new LinkedHashMap<>(); m.put("id",d.getId()); m.put("filename",d.getOriginalFilename());
            m.put("owner", d.getOwner()==null ? "unknown" : d.getOwner().getUsername()); m.put("contentType",d.getContentType());
            m.put("fileSize",d.getFileSize()); m.put("uploadedAt",d.getUploadedAt()); m.put("permission",s.getPermission()); return m;
        }).toList();
    }
    @GetMapping("/document/{id}")
    public List<Map<String,Object>> recipients(@PathVariable Long id, Authentication auth) {
        Document d=documents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (d.getOwner()==null || !d.getOwner().getUsername().equals(auth.getName())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return shares.findByDocument_IdOrderByCreatedAtDesc(id).stream().map(s -> Map.<String,Object>of("username",s.getRecipient().getUsername(),"email",s.getRecipient().getEmail(),"permission",s.getPermission())).toList();
    }
    @DeleteMapping("/{documentId}/{recipientUsername}")
    @Transactional
    public Map<String,String> revoke(@PathVariable Long documentId, @PathVariable String recipientUsername, Authentication auth) {
        Document d=documents.findById(documentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (d.getOwner()==null || !d.getOwner().getUsername().equals(auth.getName())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        shares.deleteByDocument_IdAndRecipient_Username(documentId, recipientUsername);
        activity.logActivity(auth.getName(), "REVOKE_SHARE", d.getOriginalFilename());
        return Map.of("message", "Share access revoked if it existed.");
    }
    public record ShareRequest(Long documentId, String recipientUsername) {}
}
