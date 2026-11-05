package com.edu.registration.repository;

import com.edu.registration.entity.LoaiYeuCauHoTro;
import com.edu.registration.entity.TrangThaiYeuCauHoTro;
import com.edu.registration.entity.YeuCauHoTro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface YeuCauHoTroRepository extends JpaRepository<YeuCauHoTro, Long> {

    @EntityGraph(attributePaths = {
            "sinhVien", "sinhVien.user", "nguoiTao", "nguoiXuLy",
            "monHoc", "hocKy", "diem", "diem.dangKy", "diem.dangKy.lopHocPhan",
            "diem.dangKy.lopHocPhan.monHoc", "diem.dangKy.lopHocPhan.hocKy"
    })
    Optional<YeuCauHoTro> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "sinhVien", "nguoiXuLy", "monHoc", "hocKy",
            "diem", "diem.dangKy", "diem.dangKy.lopHocPhan", "diem.dangKy.lopHocPhan.monHoc"
    })
    @Query("""
            select y from YeuCauHoTro y
            where y.sinhVien.id = :sinhVienId
              and (:trangThai is null or y.trangThai = :trangThai)
              and (:loaiYeuCau is null or y.loaiYeuCau = :loaiYeuCau)
              and (:keyword is null
                   or lower(y.maPhieu) like lower(concat('%', :keyword, '%'))
                   or lower(y.tieuDe) like lower(concat('%', :keyword, '%')))
            """)
    Page<YeuCauHoTro> searchBySinhVien(@Param("sinhVienId") Long sinhVienId,
                                       @Param("trangThai") TrangThaiYeuCauHoTro trangThai,
                                       @Param("loaiYeuCau") LoaiYeuCauHoTro loaiYeuCau,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @EntityGraph(attributePaths = {
            "sinhVien", "nguoiXuLy", "monHoc", "hocKy",
            "diem", "diem.dangKy", "diem.dangKy.lopHocPhan", "diem.dangKy.lopHocPhan.monHoc"
    })
    @Query("""
            select y from YeuCauHoTro y
            where (:trangThai is null or y.trangThai = :trangThai)
              and (:loaiYeuCau is null or y.loaiYeuCau = :loaiYeuCau)
              and (:keyword is null
                   or lower(y.maPhieu) like lower(concat('%', :keyword, '%'))
                   or lower(y.tieuDe) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.ma) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.hoTen) like lower(concat('%', :keyword, '%')))
            """)
    Page<YeuCauHoTro> searchAll(@Param("trangThai") TrangThaiYeuCauHoTro trangThai,
                                @Param("loaiYeuCau") LoaiYeuCauHoTro loaiYeuCau,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    boolean existsByMaPhieu(String maPhieu);
}
