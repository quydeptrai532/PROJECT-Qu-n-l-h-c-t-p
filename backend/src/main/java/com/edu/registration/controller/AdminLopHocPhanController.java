package com.edu.registration.controller;

import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.service.DiemService;
import com.edu.registration.service.LopHocPhanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminLopHocPhanController {

    private final LopHocPhanService lopHocPhanService;
    private final DiemService diemService;

    public AdminLopHocPhanController(LopHocPhanService lopHocPhanService) {
        this(lopHocPhanService, null);
    }

    @Autowired
    public AdminLopHocPhanController(LopHocPhanService lopHocPhanService, DiemService diemService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
    }

    @GetMapping("/lop-hoc-phan")
    public List<LopHocPhanDtos.LopHocPhanDto> danhSach(@RequestParam(required = false) Long hocKyId,
                                                       @RequestParam(required = false) String keyword) {
        return lopHocPhanService.danhSach(hocKyId, keyword);
    }

    @PostMapping("/lop-hoc-phan")
    public LopHocPhanDtos.LopHocPhanResponse tao(
            @Valid @RequestBody LopHocPhanDtos.LopHocPhanRequest request) {
        return lopHocPhanService.tao(request);
    }

    @PutMapping("/lop-hoc-phan/{id}")
    public LopHocPhanDtos.LopHocPhanResponse sua(@PathVariable Long id,
                                                 @Valid @RequestBody LopHocPhanDtos.LopHocPhanRequest request) {
        return lopHocPhanService.sua(id, request);
    }

    @PostMapping("/lop-hoc-phan/{id}/huy")
    public LopHocPhanDtos.HuyLopHocPhanResponse huy(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LopHocPhanDtos.HuyLopHocPhanRequest request) {
        return lopHocPhanService.huyLopHocPhan(id, request != null ? request.lyDo() : null);
    }

    @GetMapping("/lop-hoc-phan/{id}/trang-thai-bang-diem")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/phe-duyet-diem")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/cong-bo-diem")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem")
    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(@PathVariable Long id) {
        return diemService.thuHoiCongBoBangDiem(id);
    }

    @DeleteMapping("/lop-hoc-phan/{id}")
    public void xoa(@PathVariable Long id) {
        lopHocPhanService.xoa(id);
    }
}
