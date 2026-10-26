package com.edu.registration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record KhoaDto(Long id, String ma, String ten) {
    }

    public record KhoaRequest(
            @NotBlank(message = "Mã khoa không được để trống") String ma,
            @NotBlank(message = "Tên khoa không được để trống") String ten) {
    }

    public record NganhDto(Long id, String ma, String ten, Long khoaId, String khoaTen) {
    }

    public record NganhRequest(
            @NotBlank(message = "Mã ngành không được để trống") String ma,
            @NotBlank(message = "Tên ngành không được để trống") String ten,
            @NotNull(message = "Vui lòng chọn khoa") Long khoaId) {
    }

    public record LopHanhChinhDto(Long id, String ma, String ten, Long nganhId, String nganhTen,
                                  Long khoaId, String khoaTen) {
    }

    public record LopHanhChinhRequest(
            @NotBlank(message = "Mã lớp không được để trống") String ma,
            @NotBlank(message = "Tên lớp không được để trống") String ten,
            @NotNull(message = "Vui lòng chọn ngành") Long nganhId) {
    }

    public record MonHocDto(Long id, String ma, String ten, int soTinChi, Long khoaId, String khoaTen) {
    }

    public record MonHocRequest(
            @NotBlank(message = "Mã môn học không được để trống") String ma,
            @NotBlank(message = "Tên môn học không được để trống") String ten,
            @Min(value = 1, message = "Số tín chỉ tối thiểu là 1")
            @Max(value = 10, message = "Số tín chỉ tối đa là 10") int soTinChi,
            @NotNull(message = "Vui lòng chọn khoa") Long khoaId) {
    }

    public record MonTienQuyetDto(Long id, Long monHocId, Long monTienQuyetId,
                                  String maMon, String tenMon, int soTinChi) {
    }

    public record MonTienQuyetRequest(@NotNull(message = "Vui lòng chọn môn tiên quyết") Long monTienQuyetId) {
    }

    public record HocKyDto(Long id, String ten, String namHoc, LocalDate ngayBatDau, LocalDate ngayKetThuc,
                           LocalDate dangKyTu, LocalDate dangKyDen, int soTinChiToiThieu,
                           int soTinChiToiDa, boolean dangMoDangKy) {
    }

    public record HocKyRequest(
            @NotBlank(message = "Tên học kỳ không được để trống") String ten,
            @NotBlank(message = "Năm học không được để trống") String namHoc,
            @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDate ngayBatDau,
            @NotNull(message = "Vui lòng chọn ngày kết thúc") LocalDate ngayKetThuc,
            @NotNull(message = "Vui lòng chọn ngày mở đăng ký") LocalDate dangKyTu,
            @NotNull(message = "Vui lòng chọn ngày đóng đăng ký") LocalDate dangKyDen,
            @Min(value = 0, message = "Số tín chỉ tối thiểu không hợp lệ") int soTinChiToiThieu,
            @Min(value = 1, message = "Số tín chỉ tối đa không hợp lệ") int soTinChiToiDa) {
    }
}
