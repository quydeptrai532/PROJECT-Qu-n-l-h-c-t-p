package com.edu.registration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read models for the lecturer self-service profile and teaching dashboard (QLSV-66).
 * All data is resolved from the authenticated lecturer; no client-supplied lecturer id is accepted.
 */
public final class LecturerDtos {

    private LecturerDtos() {
    }

    /**
     * Lecturer profile. {@code truongDuocPhepSua} lists the only fields the lecturer may change
     * through {@code PUT /api/auth/me}; every other field is managed by administrators and read-only.
     */
    public record LecturerProfileDto(String username, String role, String ma, String hoTen,
                                     String email, String soDienThoai, String khoaMa, String khoaTen,
                                     boolean taiKhoanHoatDong, List<String> truongDuocPhepSua) {
    }

    public record HocKyTomTatDto(Long id, String ten, String namHoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
    }

    public record TongQuanDto(int soLop, int tongSinhVien, int tongSucChua, int soBuoiMoiTuan,
                              int soBangDiemNhap, int soBangDiemChoDuyet, int soBangDiemDaDuyet,
                              int soBangDiemDaCongBo) {
    }

    public record LopPhuTrachDto(Long id, String ma, String maMon, String tenMon, int soTinChi,
                                 String phong, int thu, int tietBatDau, int soTiet,
                                 int siSoHienTai, int siSoToiDa, String trangThaiBangDiem,
                                 int soSinhVienDangHoc, int soSinhVienDaCoDiem,
                                 LocalDateTime guiDuyetBangDiemLuc) {
    }

    /**
     * A pending item derived from real grade-sheet state. {@code loai} is one of
     * NHAP_DIEM, GUI_DUYET, CHO_DUYET, CHO_CONG_BO.
     */
    public record CongViecDto(String loai, Long lopHocPhanId, String maLop, String tenMon,
                              String tieuDe, String moTa, String lienKet) {
    }

    public record LecturerDashboardDto(HocKyTomTatDto hocKy, TongQuanDto tongQuan,
                                       List<LopPhuTrachDto> lopPhuTrach,
                                       List<RegistrationDtos.ThoiKhoaBieuDto> lichDay,
                                       List<CongViecDto> congViec) {
    }
}
