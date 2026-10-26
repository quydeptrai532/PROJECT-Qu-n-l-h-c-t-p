package com.edu.registration.repository;

import com.edu.registration.entity.Diem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiemRepository extends JpaRepository<Diem, Long> {

    Optional<Diem> findByDangKyId(Long dangKyId);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien"})
    List<Diem> findByDangKyIdIn(Collection<Long> dangKyIds);

    @Query("""
            select distinct d.dangKy.lopHocPhan.monHoc.id from Diem d
            where d.dangKy.sinhVien.id = :sinhVienId
              and d.diemTongKet >= :diemDat
            """)
    List<Long> findMonDaDat(@Param("sinhVienId") Long sinhVienId, @Param("diemDat") double diemDat);
}
