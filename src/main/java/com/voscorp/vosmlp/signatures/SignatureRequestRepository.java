package com.voscorp.vosmlp.signatures;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, Long> {
    Optional<SignatureRequest> findByToken(String token);
}