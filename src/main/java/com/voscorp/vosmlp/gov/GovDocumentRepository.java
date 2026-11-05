package com.voscorp.vosmlp.gov;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.documents.DocType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GovDocumentRepository extends JpaRepository<GovDocument, Long> {
    Optional<GovDocument> findByOwnerAndType(AppUser owner, DocType type);
}