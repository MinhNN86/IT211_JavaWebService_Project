package com.project.modules.court.repository;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.common.enums.CourtStatus;
import com.project.modules.court.entity.Court;

public interface CourtRepository extends JpaRepository<Court, Long> {
    @Query("select c from Court c where (:name is null or lower(c.name) like lower(concat('%', :name, '%'))) and (:status is null or c.status = :status) and (:minPrice is null or c.pricePerHour >= :minPrice) and (:maxPrice is null or c.pricePerHour <= :maxPrice)")
    Page<Court> search(String name, CourtStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    boolean existsByIdAndManagersUsername(Long id, String username);

    boolean existsByManagersId(java.util.UUID managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(Long id);
}
