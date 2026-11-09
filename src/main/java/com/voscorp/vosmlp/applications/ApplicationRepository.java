package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantOrderByCreatedAtDesc(AppUser applicant);
    List<Application> findAllByOrderByCreatedAtDesc();
}