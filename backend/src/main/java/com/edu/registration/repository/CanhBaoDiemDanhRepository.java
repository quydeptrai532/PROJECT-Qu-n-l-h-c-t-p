package com.edu.registration.repository;

import com.edu.registration.entity.CanhBaoDiemDanh;
import com.edu.registration.entity.LoaiNguongCanhBao;
import com.edu.registration.entity.TrangThaiCanhBao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CanhBaoDiemDanhRepository extends JpaRepository<CanhBaoDiemDanh, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    Optional<CanhBaoDiemDanh> findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(
            Long sinhVienId, Long lopHocPhanId, LoaiNguongCanhBao loaiNguong);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    List<CanhBaoDiemDanh> findByLopHocPhanIdOrderByThoiGianCapNhatDescThoiGianTaoDesc(Long lopHocPhanId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    List<CanhBaoDiemDanh> findByLopHocPhanIdAndTrangThaiOrderByThoiGianCapNhatDesc(
            Long lopHocPhanId, TrangThaiCanhBao trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    @Query("""
            select c from CanhBaoDiemDanh c
            where c.lopHocPhan.giangVien.id = :giangVienId
              and (:trangThai is null or c.trangThai = :trangThai)
            order by coalesce(c.thoiGianCapNhat, c.thoiGianTao) desc
            """)
    List<CanhBaoDiemDanh> findByGiangVienIdAndTrangThai(
            @Param("giangVienId") Long giangVienId,
            @Param("trangThai") TrangThaiCanhBao trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    @Query("""
            select c from CanhBaoDiemDanh c
            where c.sinhVien.id = :sinhVienId
              and (:trangThai is null or c.trangThai = :trangThai)
            order by coalesce(c.thoiGianCapNhat, c.thoiGianTao) desc
            """)
    List<CanhBaoDiemDanh> findBySinhVienIdAndTrangThai(
            @Param("sinhVienId") Long sinhVienId,
            @Param("trangThai") TrangThaiCanhBao trangThai);

    long countByLopHocPhanIdAndTrangThai(Long lopHocPhanId, TrangThaiCanhBao trangThai);

    void deleteByLopHocPhanId(Long lopHocPhanId);
}
