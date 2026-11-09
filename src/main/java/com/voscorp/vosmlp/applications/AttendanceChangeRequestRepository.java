package com.voscorp.vosmlp.applications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceChangeRequestRepository extends JpaRepository<AttendanceChangeRequest, Long> {
    Optional<AttendanceChangeRequest> findByApplication(Application application);
}