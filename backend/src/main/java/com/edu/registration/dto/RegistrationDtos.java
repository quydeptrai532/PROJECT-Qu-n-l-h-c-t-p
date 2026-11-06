package com.edu.registration.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RegistrationDtos {

    private RegistrationDtos() {
    }

    public record DangKyRequest(@NotNull(message = "Vui lòng chọn lớp học phần") Long lopHocPhanId,
                                String loaiDangKy) {
    }

    public record XacNhanKeHoachRequest(
            @NotEmpty(message = "Giỏ học phần đang trống") List<@NotNull Long> lopHocPhanIds) {
    }

    public record DangKyDto(Long id, Long lopHocPhanId, String maLop, Long monHocId, String maMon, String tenMon,
                            int soTinChi, String tenGiangVien, String phong, int thu, int tietBatDau, int soTiet,
                            Long hocKyId, String tenHocKy, String namHoc, LocalDateTime thoiGianDangKy, String trangThai,
                            Double diemQuaTrinh, Double diemCuoiKy, Double diemTongKet) {
    }

    public record LichSuDangKyDto(Long id, Long dangKyId, Long hocKyId, String maLop, String maMon,
                                  String tenMon, LocalDateTime thoiGianDangKy, String trangThai,
                                  LocalDateTime thoiDiem, LocalDateTime thoiGianHuy, String lyDoHuy) { }

    public record LanHocTruocDto(Long dangKyId, String maLop, String tenHocKy,
                                 LocalDateTime thoiGianDangKy, Double diemTongKet) { }

    public record DangKyHocLaiDto(Long lopHocPhanId, String maLop, Long monHocId, String maMon, String tenMon,
                                  int soTinChi, String tenGiangVien, String phong, int thu, int tietBatDau,
                                  int soTiet, int siSoHienTai, int siSoToiDa, boolean dangMoDangKy,
                                  String loaiDangKy, List<LanHocTruocDto> cacLanHocTruoc) { }

    public record NhapDiemRequest(
            @NotNull(message = "Vui lòng nhập điểm quá trình")
            @DecimalMin(value = "0", message = "Điểm phải từ 0 đến 10")
            @DecimalMax(value = "10", message = "Điểm phải từ 0 đến 10") Double diemQuaTrinh,
            @NotNull(message = "Vui lòng nhập điểm cuối kỳ")
            @DecimalMin(value = "0", message = "Điểm phải từ 0 đến 10")
            @DecimalMax(value = "10", message = "Điểm phải từ 0 đến 10") Double diemCuoiKy) {
    }

    public record DiemDto(Long dangKyId, Long sinhVienId, String maSinhVien, String hoTen, String email,
                          Double diemQuaTrinh, Double diemCuoiKy, Double diemTongKet) {
    }

    public record TrangThaiBangDiemDto(Long lopHocPhanId, String maLop, String maMon, String tenMon,
                                       Long hocKyId, String tenHocKy, String namHoc,
                                       String trangThaiBangDiem, int phienBanCongBo,
                                       int soSinhVienDangHoc, int soSinhVienDuDieuKien,
                                       LocalDateTime guiDuyetLuc, LocalDateTime duyetLuc, LocalDateTime congBoLuc) {
    }

    public record HocKyTongKetDto(Long hocKyId, String tenHocKy, String namHoc, int tongTinChi,
                                  int tinChiCoDiem, double gpa10, double gpa4) {
    }

    public record KetQuaHocTapDto(List<DangKyDto> items, List<HocKyTongKetDto> hocKy,
                                  int tongTinChi, double gpa10, double gpa4, int tinChiTichLuy) {
    }

    public record MucLichSuHocTapDto(Long dangKyId, Long hocKyId, String tenHocKy, String namHoc,
                                     LocalDate ngayBatDauHocKy, Long monHocId, String maMon, String tenMon,
                                     int soTinChi, Long lopHocPhanId, String maLop, String tenGiangVien,
                                     LocalDateTime thoiGianDangKy, Double diemQuaTrinh, Double diemCuoiKy,
                                     Double diemTongKet, String trangThaiKetQua, String tenTrangThaiKetQua,
                                     int lanHoc, int tongSoLanHoc) {
    }

    public record LichSuHocTapDto(List<MucLichSuHocTapDto> items, List<HocKyTongKetDto> hocKy,
                                  List<MucLichSuHocTapDto> monChuaDat, int tongTinChi, int tinChiCoDiem,
                                  int tinChiTichLuy, double gpa10, double gpa4, int soMonDat,
                                  int soMonChuaDat, int soMonChuaCoDiem, boolean coNhieuLanHoc) {
    }

    public record ThoiKhoaBieuDto(Long lopHocPhanId, String maLop, String tenMon, String phong,
                                  int thu, int tietBatDau, int soTiet, String giangVien) {
    }

    public record ThongBaoNghiRequest(
            @NotNull(message = "Vui lòng chọn lớp học phần") Long lopHocPhanId,
            @NotNull(message = "Vui lòng chọn ngày nghỉ") LocalDate ngayNghi,
            @Size(max = 255, message = "Lý do tối đa 255 ký tự") String lyDo) {
    }

    public record ThongBaoNghiDto(Long id, Long lopHocPhanId, String maLop, String tenMon,
                                  LocalDate ngayNghi, String lyDo, String tenGiangVien,
                                  LocalDateTime thoiGianTao) {
    }

    public record ThongTinSinhVienPdfDto(String maSinhVien, String hoTen, String email,
                                         String khoa, String nganh, String lopHanhChinh,
                                         String khoaNhapHoc, String phienBanChuongTrinh) {
    }

    public record MucBangDiemPdfDto(int stt, Long dangKyId, Long hocKyId, String tenHocKy, String namHoc,
                                    String maMon, String tenMon, int soTinChi, String maLop,
                                    int lanHoc, int tongSoLanHoc, Double diemQuaTrinh, Double diemCuoiKy,
                                    Double diemTongKet, double diemHe4, String trangThaiKetQua,
                                    String tenTrangThaiKetQua) {
    }

    public record HocKyBangDiemPdfDto(Long hocKyId, String tenHocKy, String namHoc,
                                      int tinChiCoDiem, int tinChiDat, double gpa10, double gpa4,
                                      List<MucBangDiemPdfDto> items) {
    }

    public record BangDiemPdfDto(ThongTinSinhVienPdfDto sinhVien, String donViPhatHanh,
                                 LocalDateTime thoiDiemPhatHanh, List<HocKyBangDiemPdfDto> hocKy,
                                 int tongSoMonDaCongBo, int tongTinChiCoDiem, int tinChiTichLuy,
                                 double gpa10, double gpa4, boolean coNhieuLanHoc) {
    }
}
