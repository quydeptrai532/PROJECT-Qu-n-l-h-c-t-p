package com.edu.registration.controller;

import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.service.DiemService;
import com.edu.registration.service.LopHocPhanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('GIANG_VIEN')")
public class GiangVienController {

    private final LopHocPhanService lopHocPhanService;
    private final DiemService diemService;

    public GiangVienController(LopHocPhanService lopHocPhanService, DiemService diemService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
    }

    @GetMapping("/giang-vien/me/lop-hoc-phan")
    public List<LopHocPhanDtos.LopHocPhanDto> lopCuaToi(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thoi-khoa-bieu")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.thoiKhoaBieuCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien")
    public List<RegistrationDtos.DiemDto> danhSachSinhVien(@PathVariable Long id) {
        return diemService.danhSachDiemLop(id);
    }

    @PutMapping("/diem/{dangKyId}")
    public RegistrationDtos.DiemDto nhapDiem(@PathVariable Long dangKyId,
                                            @Valid @RequestBody RegistrationDtos.NhapDiemRequest request) {
        return diemService.nhapDiem(dangKyId, request);
    }
}
