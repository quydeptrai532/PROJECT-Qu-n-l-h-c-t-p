package com.edu.registration.repository;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.TrangThaiDangKy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DangKyHocPhanRepository extends JpaRepository<DangKyHocPhan, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy",
            "lopHocPhan.giangVien", "sinhVien"})
    @Query("""
            select d from DangKyHocPhan d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.hocKy.id = :hocKyId
              and d.trangThai = :trangThai
            order by d.lopHocPhan.ma
            """)
    List<DangKyHocPhan> findTheoHocKy(@Param("sinhVienId") Long sinhVienId,
                                      @Param("hocKyId") Long hocKyId,
                                      @Param("trangThai") TrangThaiDangKy trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy",
            "lopHocPhan.giangVien", "sinhVien"})
    @Query("""
            select d from DangKyHocPhan d
            where d.sinhVien.id = :sinhVienId
              and d.trangThai = :trangThai
            order by d.lopHocPhan.hocKy.ngayBatDau desc, d.lopHocPhan.ma
            """)
    List<DangKyHocPhan> findAllActiveOfStudent(@Param("sinhVienId") Long sinhVienId,
                                               @Param("trangThai") TrangThaiDangKy trangThai);

    Optional<DangKyHocPhan> findBySinhVienIdAndLopHocPhanId(Long sinhVienId, Long lopHocPhanId);

    Optional<DangKyHocPhan> findByIdAndSinhVienId(Long id, Long sinhVienId);

    @EntityGraph(attributePaths = {"sinhVien", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.giangVien", "lopHocPhan.hocKy"})
    @Query("""
            select d from DangKyHocPhan d
            where d.lopHocPhan.id = :lopHocPhanId
              and d.trangThai = :trangThai
            order by d.sinhVien.ma
            """)
    List<DangKyHocPhan> findByLopHocPhan(@Param("lopHocPhanId") Long lopHocPhanId,
                                         @Param("trangThai") TrangThaiDangKy trangThai);

    long countByLopHocPhanIdAndTrangThai(Long lopHocPhanId, TrangThaiDangKy trangThai);

    boolean existsByLopHocPhanId(Long lopHocPhanId);

    @Query("""
            select coalesce(sum(d.lopHocPhan.monHoc.soTinChi), 0) from DangKyHocPhan d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.hocKy.id = :hocKyId
              and d.trangThai = :trangThai
            """)
    long tinhTongTinChi(@Param("sinhVienId") Long sinhVienId,
                        @Param("hocKyId") Long hocKyId,
                        @Param("trangThai") TrangThaiDangKy trangThai);

    long countByTrangThai(TrangThaiDangKy trangThai);
}
