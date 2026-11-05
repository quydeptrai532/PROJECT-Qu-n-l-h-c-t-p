package com.edu.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập tên đăng nhập") String username,
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    public record LoginResponse(String token, String role, String hoTen, String ma, String username) {
    }

    public record ProfileDto(String username, String role, String hoTen, String ma,
                             String email, String soDienThoai) {
    }

    /**
     * Self-service profile update. Only these fields are accepted (see
     * {@code AuthService.SELF_EDITABLE_PROFILE_FIELDS}); unknown properties such as role or code are ignored.
     */
    public record UpdateProfileRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String hoTen,
            @Email(message = "Email không hợp lệ")
            @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
            @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự") String soDienThoai) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String matKhauCu,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 ký tự") String matKhauMoi) {
    }
}
