package com.edu.registration.controller;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/sinh-vien")
    public List<UserDtos.SinhVienDto> danhSachSinhVien(@RequestParam(required = false) String keyword) {
        return userService.danhSachSinhVien(keyword);
    }

    @PostMapping("/sinh-vien")
    public UserDtos.SinhVienDto taoSinhVien(@Valid @RequestBody UserDtos.SinhVienRequest request) {
        return userService.taoSinhVien(request);
    }

    @PutMapping("/sinh-vien/{id}")
    public UserDtos.SinhVienDto suaSinhVien(@PathVariable Long id,
                                            @Valid @RequestBody UserDtos.SinhVienRequest request) {
        return userService.suaSinhVien(id, request);
    }

    @PatchMapping("/sinh-vien/{id}/trang-thai")
    public UserDtos.SinhVienDto doiTrangThaiSinhVien(@PathVariable Long id,
                                                     @RequestBody UserDtos.TrangThaiRequest request) {
        return userService.doiTrangThaiSinhVien(id, request.trangThai());
    }

    @PostMapping("/sinh-vien/import")
    public UserDtos.ImportResultDto importSinhVien(@RequestPart("file") MultipartFile file) {
        return userService.importSinhVien(file);
    }

    @GetMapping("/giang-vien")
    public List<UserDtos.GiangVienDto> danhSachGiangVien(@RequestParam(required = false) String keyword) {
        return userService.danhSachGiangVien(keyword);
    }

    @PostMapping("/giang-vien")
    public UserDtos.GiangVienDto taoGiangVien(@Valid @RequestBody UserDtos.GiangVienRequest request) {
        return userService.taoGiangVien(request);
    }

    @PutMapping("/giang-vien/{id}")
    public UserDtos.GiangVienDto suaGiangVien(@PathVariable Long id,
                                              @Valid @RequestBody UserDtos.GiangVienRequest request) {
        return userService.suaGiangVien(id, request);
    }

    @PatchMapping("/giang-vien/{id}/trang-thai")
    public UserDtos.GiangVienDto doiTrangThaiGiangVien(@PathVariable Long id,
                                                       @RequestBody UserDtos.TrangThaiRequest request) {
        return userService.doiTrangThaiGiangVien(id, request.trangThai());
    }
}
