package com.voscorp.vosmlp.applications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalAccessRequestRepository extends JpaRepository<JournalAccessRequest, Long> {
    Optional<JournalAccessRequest> findByApplication(Application application);
}