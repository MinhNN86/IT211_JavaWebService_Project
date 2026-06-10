package com.project.modules.audit.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import com.project.modules.audit.entity.AuditLog;
import com.project.modules.audit.repository.AuditLogRepository;
import com.project.modules.audit.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String action, String message, String status) {
        repository.save(AuditLog.builder().username(username).action(action).message(message).status(status).build());
    }
}
