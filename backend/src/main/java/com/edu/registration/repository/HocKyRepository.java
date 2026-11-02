package com.edu.registration.repository;

import com.edu.registration.entity.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HocKyRepository extends JpaRepository<HocKy, Long> {

    List<HocKy> findAllByOrderByNgayBatDauDesc();

    Optional<HocKy> findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(
            LocalDate ngay, LocalDate cungNgay);

    Optional<HocKy> findFirstByOrderByDangKyDenDesc();

    boolean existsByTenAndNamHoc(String ten, String namHoc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HocKy h where h.id = :id")
    Optional<HocKy> findByIdForUpdate(@Param("id") Long id);
}
