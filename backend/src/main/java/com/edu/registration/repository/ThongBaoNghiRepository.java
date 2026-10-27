package com.edu.registration.repository;

import com.edu.registration.entity.ThongBaoNghi;
import com.edu.registration.entity.TrangThaiDangKy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ThongBaoNghiRepository extends JpaRepository<ThongBaoNghi, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "giangVien"})
    @Query("""
            select t from ThongBaoNghi t
            where t.lopHocPhan.giangVien.id = :giangVienId
              and t.lopHocPhan.hocKy.id = :hocKyId
            order by t.ngayNghi desc, t.lopHocPhan.ma
            """)
    List<ThongBaoNghi> cuaGiangVien(@Param("giangVienId") Long giangVienId,
                                    @Param("hocKyId") Long hocKyId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "giangVien"})
    @Query("""
            select t from ThongBaoNghi t
            where t.lopHocPhan.hocKy.id = :hocKyId
              and exists (
                select 1 from DangKyHocPhan d
                where d.lopHocPhan = t.lopHocPhan
                  and d.sinhVien.id = :sinhVienId
                  and d.trangThai = :trangThai
              )
            order by t.ngayNghi desc, t.lopHocPhan.ma
            """)
    List<ThongBaoNghi> cuaSinhVien(@Param("sinhVienId") Long sinhVienId,
                                   @Param("hocKyId") Long hocKyId,
                                   @Param("trangThai") TrangThaiDangKy trangThai);

    boolean existsByLopHocPhanIdAndNgayNghi(Long lopHocPhanId, LocalDate ngayNghi);

    Optional<ThongBaoNghi> findByIdAndLopHocPhanGiangVienId(Long id, Long giangVienId);

    void deleteByLopHocPhanId(Long lopHocPhanId);
}
