package com.edu.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class SupportRequestDtos {

    private SupportRequestDtos() {
    }

    public record TaoYeuCauRequest(
            @NotBlank(message = "Loại yêu cầu không được để trống")
            @Size(max = 40, message = "Loại yêu cầu không hợp lệ")
            String loaiYeuCau,

            @NotBlank(message = "Tiêu đề không được để trống")
            @Size(min = 5, max = 200, message = "Tiêu đề phải từ 5 đến 200 ký tự")
            String tieuDe,

            @NotBlank(message = "Nội dung không được để trống")
            @Size(min = 10, max = 4000, message = "Nội dung phải từ 10 đến 4000 ký tự")
            String noiDung,

            Long monHocId,
            Long diemId,
            Long hocKyId
    ) {
    }

    public record ThemTraoDoiRequest(
            @NotBlank(message = "Nội dung trao đổi không được để trống")
            @Size(min = 1, max = 2000, message = "Nội dung trao đổi phải từ 1 đến 2000 ký tự")
            String noiDung
    ) {
    }

    public record DoiTrangThaiRequest(
            @NotBlank(message = "Trạng thái mới không được để trống")
            @Size(max = 40, message = "Trạng thái mới không hợp lệ")
            String trangThai,

            @Size(max = 500, message = "Ghi chú xử lý tối đa 500 ký tự")
            String ghiChu
    ) {
    }

    public record DanhMucMucDto(String ma, String ten) {
    }

    public record ChinhSachTepDto(
            long maxFileSizeBytes,
            int maxFilesPerRequest,
            List<String> allowedExtensions,
            String slaNote
    ) {
    }

    public record MonHocLienQuanDto(
            Long id,
            String ma,
            String ten,
            int soTinChi
    ) {
    }

    public record HocKyLienQuanDto(
            Long id,
            String ten,
            String namHoc
    ) {
    }

    public record DiemLienQuanDto(
            Long id,
            Long dangKyId,
            Long monHocId,
            String maMon,
            String tenMon,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            String maLop,
            Double diemQuaTrinh,
            Double diemCuoiKy,
            Double diemTongKet
    ) {
    }

    public record DanhMucHoTroDto(
            List<DanhMucMucDto> loaiYeuCau,
            List<DanhMucMucDto> trangThai,
            ChinhSachTepDto chinhSachTep,
            List<MonHocLienQuanDto> monHoc,
            List<HocKyLienQuanDto> hocKy,
            List<DiemLienQuanDto> diemCuaToi
    ) {
    }

    public record TepDinhKemDto(
            Long id,
            String tenTepGoc,
            String contentType,
            long kichThuoc,
            LocalDateTime createdAt,
            String downloadUrl
    ) {
    }

    public record TraoDoiDto(
            Long id,
            Long nguoiGuiId,
            String tenHienThiNguoiGui,
            String vaiTroNguoiGui,
            String noiDung,
            LocalDateTime createdAt
    ) {
    }

    public record LichSuTrangThaiDto(
            Long id,
            String trangThaiCu,
            String tenTrangThaiCu,
            String trangThaiMoi,
            String tenTrangThaiMoi,
            Long nguoiThucHienId,
            String tenNguoiThucHien,
            String vaiTroNguoiThucHien,
            String ghiChu,
            LocalDateTime thoiDiem
    ) {
    }

    public record YeuCauTomTatDto(
            Long id,
            String maPhieu,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String loaiYeuCau,
            String tenLoaiYeuCau,
            String tieuDe,
            String trangThai,
            String tenTrangThai,
            MonHocLienQuanDto monHoc,
            DiemLienQuanDto diem,
            HocKyLienQuanDto hocKy,
            String nguoiXuLyUsername,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record YeuCauChiTietDto(
            Long id,
            String maPhieu,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String loaiYeuCau,
            String tenLoaiYeuCau,
            String tieuDe,
            String noiDungGoc,
            String trangThai,
            String tenTrangThai,
            MonHocLienQuanDto monHoc,
            DiemLienQuanDto diem,
            HocKyLienQuanDto hocKy,
            String nguoiXuLyUsername,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean coThePhanHoi,
            List<String> trangThaiTiepTheoHopLe,
            List<TepDinhKemDto> tepDinhKem,
            List<TraoDoiDto> traoDoi,
            List<LichSuTrangThaiDto> lichSuTrangThai
    ) {
    }
}
