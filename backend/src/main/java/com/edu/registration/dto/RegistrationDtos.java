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

    public record DangKyRequest(@NotNull(message = "Vui lòng chọn lớp học phần") Long lopHocPhanId) {
    }

    public record XacNhanKeHoachRequest(
            @NotEmpty(message = "Giỏ học phần đang trống") List<@NotNull Long> lopHocPhanIds) {
    }

    public record DangKyDto(Long id, Long lopHocPhanId, String maLop, Long monHocId, String maMon, String tenMon,
                            int soTinChi, String tenGiangVien, String phong, int thu, int tietBatDau, int soTiet,
                            Long hocKyId, String tenHocKy, String namHoc, LocalDateTime thoiGianDangKy, String trangThai,
                            Double diemQuaTrinh, Double diemCuoiKy, Double diemTongKet) {
    }

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

    public record HocKyTongKetDto(Long hocKyId, String tenHocKy, String namHoc, int tongTinChi,
                                  int tinChiCoDiem, double gpa10, double gpa4) {
    }

    public record KetQuaHocTapDto(List<DangKyDto> items, List<HocKyTongKetDto> hocKy,
                                  int tongTinChi, double gpa10, double gpa4, int tinChiTichLuy) {
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
}
