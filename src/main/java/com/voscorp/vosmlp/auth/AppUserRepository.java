package com.voscorp.vosmlp.auth;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByLinkedStudent(Student linkedStudent);
    List<AppUser> findByRole(UserRole role);
}