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

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.lopHocPhan", "dangKy.lopHocPhan.monHoc", "dangKy.lopHocPhan.hocKy"})
    Optional<Diem> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.lopHocPhan", "dangKy.lopHocPhan.monHoc", "dangKy.lopHocPhan.hocKy"})
    @Query("select d from Diem d where d.dangKy.sinhVien.id = :sinhVienId order by d.dangKy.lopHocPhan.hocKy.ngayBatDau desc, d.id desc")
    List<Diem> findBySinhVienIdWithDetails(@Param("sinhVienId") Long sinhVienId);

    @Query("""
            select distinct d.dangKy.lopHocPhan.monHoc.id from Diem d
            where d.dangKy.sinhVien.id = :sinhVienId
              and d.diemTongKet >= :diemDat
            """)
    List<Long> findMonDaDat(@Param("sinhVienId") Long sinhVienId, @Param("diemDat") double diemDat);
}
