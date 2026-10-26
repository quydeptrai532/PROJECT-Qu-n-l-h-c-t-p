package com.edu.registration.controller;

import com.edu.registration.dto.AuthDtos;
import com.edu.registration.dto.MessageResponse;
import com.edu.registration.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.ProfileDto me() {
        return authService.profile();
    }

    @PutMapping("/me")
    public AuthDtos.ProfileDto capNhatHoSo(@Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }

    @PutMapping("/doi-mat-khau")
    public MessageResponse doiMatKhau(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return new MessageResponse("Đổi mật khẩu thành công");
    }
}
