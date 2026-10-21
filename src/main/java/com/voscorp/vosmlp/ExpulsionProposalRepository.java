package com.voscorp.vosmlp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpulsionProposalRepository extends JpaRepository<ExpulsionProposal, Long> {
    boolean existsByStudentAndStatus(Student student, ProposalStatus status);
}