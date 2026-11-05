package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findByOwner(AppUser owner);
    List<UserDocument> findByOwnerAndType(AppUser owner, DocType type);
    List<UserDocument> findByTypeIn(List<DocType> types);
}