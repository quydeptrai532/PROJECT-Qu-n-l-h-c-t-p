package com.edu.registration.service;

import com.edu.registration.dto.AuthDtos;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthService(UserRepository userRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        }
        if (!user.isTrangThai()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo");
        }

        String hoTen = "";
        String ma = "";
        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            SinhVien sv = sinhVienRepository.findByUser(user).orElse(null);
            if (sv != null) {
                hoTen = sv.getHoTen();
                ma = sv.getMa();
            }
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            GiangVien gv = giangVienRepository.findByUser(user).orElse(null);
            if (gv != null) {
                hoTen = gv.getHoTen();
                ma = gv.getMa();
            }
        } else {
            hoTen = "Quản trị viên";
            ma = "ADMIN";
        }

        return new AuthDtos.LoginResponse(jwtService.generateToken(user), user.getRole().name(),
                hoTen, ma, user.getUsername());
    }

    @Transactional(readOnly = true)
    public AuthDtos.ProfileDto profile() {
        User user = currentUser.get();
        String hoTen = "";
        String ma = "";
        String email = null;
        String soDienThoai = null;

        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            SinhVien sv = sinhVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
            hoTen = sv.getHoTen();
            ma = sv.getMa();
            email = sv.getEmail();
            soDienThoai = sv.getSoDienThoai();
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            GiangVien gv = giangVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            hoTen = gv.getHoTen();
            ma = gv.getMa();
            email = gv.getEmail();
            soDienThoai = gv.getSoDienThoai();
        } else {
            hoTen = "Quản trị viên";
            ma = "ADMIN";
        }

        return new AuthDtos.ProfileDto(user.getUsername(), user.getRole().name(), hoTen, ma, email, soDienThoai);
    }

    @Transactional
    public AuthDtos.ProfileDto updateProfile(AuthDtos.UpdateProfileRequest request) {
        User user = currentUser.get();
        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            SinhVien sv = sinhVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
            sv.setHoTen(request.hoTen().trim());
            sv.setEmail(request.email());
            sv.setSoDienThoai(request.soDienThoai());
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            GiangVien gv = giangVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            gv.setHoTen(request.hoTen().trim());
            gv.setEmail(request.email());
            gv.setSoDienThoai(request.soDienThoai());
        }
        return profile();
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        User user = currentUser.get();
        if (!passwordEncoder.matches(request.matKhauCu(), user.getPassword())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(request.matKhauMoi()));
    }
}
