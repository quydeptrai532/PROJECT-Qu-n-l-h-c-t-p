package com.edu.registration.repository;

import com.edu.registration.entity.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HocKyRepository extends JpaRepository<HocKy, Long> {

    List<HocKy> findAllByOrderByNgayBatDauDesc();

    Optional<HocKy> findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(
            LocalDate ngay, LocalDate cungNgay);

    Optional<HocKy> findFirstByOrderByDangKyDenDesc();

    boolean existsByTenAndNamHoc(String ten, String namHoc);
}
