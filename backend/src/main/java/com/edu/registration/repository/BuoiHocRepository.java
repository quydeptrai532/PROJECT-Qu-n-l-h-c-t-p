package com.edu.registration.repository;

import com.edu.registration.entity.BuoiHoc;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BuoiHocRepository extends JpaRepository<BuoiHoc, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "nguoiTao", "nguoiCapNhat", "nguoiKhoa"})
    List<BuoiHoc> findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(Long lopHocPhanId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "nguoiTao", "nguoiCapNhat", "nguoiKhoa"})
    Optional<BuoiHoc> findWithDetailsById(Long id);

    Optional<BuoiHoc> findByLopHocPhanIdAndNgayHocAndCaHoc(Long lopHocPhanId, LocalDate ngayHoc, Integer caHoc);

    boolean existsByLopHocPhanIdAndNgayHocAndCaHoc(Long lopHocPhanId, LocalDate ngayHoc, Integer caHoc);

    @Query("""
            select b from BuoiHoc b
            where b.lopHocPhan.hocKy.id = :hocKyId
            order by b.ngayHoc asc, b.caHoc asc
            """)
    List<BuoiHoc> findByHocKyId(@Param("hocKyId") Long hocKyId);

    long countByLopHocPhanId(Long lopHocPhanId);
}
