
package com.trustvault.repository;

import com.trustvault.model.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentShareRepository
        extends JpaRepository<DocumentShare, Long> {

    Optional<DocumentShare> findByDocument_IdAndRecipient_Username(
            Long documentId, String username);

    List<DocumentShare> findByRecipient_UsernameOrderByCreatedAtDesc(
            String username);

    List<DocumentShare> findByDocument_IdOrderByCreatedAtDesc(
            Long documentId);

    void deleteByDocument_IdAndRecipient_Username(
            Long documentId, String username);

    void deleteByDocument_Id(Long documentId);

    boolean existsByDocument_IdAndRecipient_Username(
            Long documentId, String username);
}
