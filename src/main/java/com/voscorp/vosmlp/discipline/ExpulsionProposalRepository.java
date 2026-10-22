package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpulsionProposalRepository extends JpaRepository<ExpulsionProposal, Long> {
    boolean existsByStudentAndStatus(Student student, ProposalStatus status);
}