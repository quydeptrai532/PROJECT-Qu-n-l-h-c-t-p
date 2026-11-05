package com.edu.registration.repository;

import com.edu.registration.entity.DiemDanh;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiemDanhRepository extends JpaRepository<DiemDanh, Long> {

    @EntityGraph(attributePaths = {"sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh", "buoiHoc", "nguoiTao", "nguoiCapNhat"})
    List<DiemDanh> findByBuoiHocIdOrderBySinhVienMaAsc(Long buoiHocId);

    @EntityGraph(attributePaths = {"sinhVien", "buoiHoc"})
    List<DiemDanh> findByBuoiHocLopHocPhanId(Long lopHocPhanId);

    Optional<DiemDanh> findByBuoiHocIdAndSinhVienId(Long buoiHocId, Long sinhVienId);

    @Query("""
            select d from DiemDanh d
            join fetch d.buoiHoc b
            join fetch b.lopHocPhan l
            left join fetch l.monHoc
            left join fetch l.hocKy
            where d.sinhVien.id = :sinhVienId
              and (:hocKyId is null or l.hocKy.id = :hocKyId)
            order by b.ngayHoc desc, b.caHoc desc
            """)
    List<DiemDanh> findBySinhVienAndHocKy(@Param("sinhVienId") Long sinhVienId,
                                          @Param("hocKyId") Long hocKyId);

    @Query("""
            select d from DiemDanh d
            join fetch d.buoiHoc b
            join fetch b.lopHocPhan l
            where d.sinhVien.id = :sinhVienId
              and l.id = :lopHocPhanId
            order by b.ngayHoc asc, b.caHoc asc
            """)
    List<DiemDanh> findBySinhVienAndLopHocPhan(@Param("sinhVienId") Long sinhVienId,
                                               @Param("lopHocPhanId") Long lopHocPhanId);

    long countByBuoiHocId(Long buoiHocId);

    void deleteByBuoiHocId(Long buoiHocId);
}
