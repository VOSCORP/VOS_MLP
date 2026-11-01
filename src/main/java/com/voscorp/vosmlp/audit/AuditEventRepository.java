package com.voscorp.vosmlp.audit;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditEventRepository extends Repository<AuditEvent, Long> {
    AuditEvent save(AuditEvent event);
    List<AuditEvent> findTop500ByOrderByCreatedAtDesc();
    long count();
}