package com.project.modules.court.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.modules.court.entity.CourtImage;

public interface CourtImageRepository extends JpaRepository<CourtImage, UUID> {
}
