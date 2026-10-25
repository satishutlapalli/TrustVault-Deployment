package com.trustvault.repository;

import com.trustvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByUploadedAtDesc();
    List<Document> findByOwner_UsernameOrderByUploadedAtDesc(
        String username);
}