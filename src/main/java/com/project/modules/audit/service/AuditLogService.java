package com.project.modules.audit.service;

public interface AuditLogService {
    void log(String username, String action, String message, String status);
}
