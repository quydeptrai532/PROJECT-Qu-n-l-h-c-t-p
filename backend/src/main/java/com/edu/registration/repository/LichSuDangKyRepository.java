package com.edu.registration.repository;

import com.edu.registration.entity.LichSuDangKy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuDangKyRepository extends JpaRepository<LichSuDangKy, Long> {
    boolean existsByDangKyId(Long dangKyId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "sinhVien"})
    @Query("select h from LichSuDangKy h where h.sinhVien.id = :sinhVienId and h.hocKyId = :hocKyId")
    Page<LichSuDangKy> findLichSu(@Param("sinhVienId") Long sinhVienId,
                                   @Param("hocKyId") Long hocKyId, Pageable pageable);
}
