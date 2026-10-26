package com.edu.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record SinhVienDto(Long id, String ma, String hoTen, String email, String soDienThoai,
                              Long khoaId, String khoaTen, Long nganhId, String nganhTen,
                              Long lopId, String lopTen, String username, boolean trangThai) {
    }

    public record SinhVienRequest(
            @NotBlank(message = "Mã sinh viên không được để trống") String ma,
            @NotBlank(message = "Họ tên không được để trống") String hoTen,
            @Email(message = "Email không hợp lệ") String email,
            String soDienThoai,
            Long khoaId,
            Long nganhId,
            Long lopId,
            @NotBlank(message = "Tên đăng nhập không được để trống") String username,
            String matKhau) {
    }

    public record GiangVienDto(Long id, String ma, String hoTen, String email, String soDienThoai,
                               Long khoaId, String khoaTen, String username, boolean trangThai) {
    }

    public record GiangVienRequest(
            @NotBlank(message = "Mã giảng viên không được để trống") String ma,
            @NotBlank(message = "Họ tên không được để trống") String hoTen,
            @Email(message = "Email không hợp lệ") String email,
            String soDienThoai,
            Long khoaId,
            @NotBlank(message = "Tên đăng nhập không được để trống") String username,
            String matKhau) {
    }

    public record TrangThaiRequest(boolean trangThai) {
    }

    public record ImportResultDto(int thanhCong, List<String> loi) {
    }
}
