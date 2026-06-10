package com.project.modules.court.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.ForbiddenException;
import com.project.common.util.SecurityUtils;
import com.project.modules.court.repository.CourtRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourtAccessService {
    private final CourtRepository courts;

    @Transactional(readOnly = true)
    public void requireCanManage(Long courtId) {
        if (!SecurityUtils.hasRole("ADMIN")
                && !courts.existsByIdAndManagersUsername(courtId, SecurityUtils.currentUsername()))
            throw new ForbiddenException("You are not a manager of this court");
    }
}
