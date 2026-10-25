package com.trustvault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_shares", uniqueConstraints = @UniqueConstraint(
        name = "uk_document_recipient", columnNames = {"document_id", "recipient_id"}))
public class DocumentShare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @Column(nullable = false, length = 20)
    private String permission = "VIEWER";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public DocumentShare() {}
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public User getSharedBy() { return sharedBy; }
    public void setSharedBy(User sharedBy) { this.sharedBy = sharedBy; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
