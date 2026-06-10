package com.project.modules.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.modules.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
