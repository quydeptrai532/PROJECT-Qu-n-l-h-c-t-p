package com.edu.registration.controller;

import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.service.DangKyService;
import com.edu.registration.service.DiemService;
import com.edu.registration.service.LopHocPhanService;
import com.edu.registration.service.ThongBaoNghiService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SinhVienController {

    private final LopHocPhanService lopHocPhanService;
    private final DangKyService dangKyService;
    private final DiemService diemService;
    private final ThongBaoNghiService thongBaoNghiService;

    public SinhVienController(LopHocPhanService lopHocPhanService, DangKyService dangKyService,
                              DiemService diemService, ThongBaoNghiService thongBaoNghiService) {
        this.lopHocPhanService = lopHocPhanService;
        this.dangKyService = dangKyService;
        this.diemService = diemService;
        this.thongBaoNghiService = thongBaoNghiService;
    }

    @GetMapping("/lop-hoc-phan")
    public List<LopHocPhanDtos.LopHocPhanDto> danhSachLopHocPhan(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String keyword) {
        return lopHocPhanService.danhSach(hocKyId, keyword);
    }

    @GetMapping("/lop-hoc-phan/{id}")
    public LopHocPhanDtos.LopHocPhanDto chiTietLopHocPhan(@PathVariable Long id) {
        return lopHocPhanService.chiTiet(id);
    }

    @PostMapping("/dang-ky")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.DangKyDto dangKy(@Valid @RequestBody RegistrationDtos.DangKyRequest request) {
        return dangKyService.dangKy(request.lopHocPhanId());
    }

    @PostMapping("/dang-ky/xac-nhan-ke-hoach")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.DangKyDto> xacNhanKeHoach(
            @Valid @RequestBody RegistrationDtos.XacNhanKeHoachRequest request) {
        return dangKyService.xacNhanKeHoach(request.lopHocPhanIds());
    }

    @DeleteMapping("/dang-ky/{id}")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public void huyDangKy(@PathVariable Long id) {
        dangKyService.huyDangKy(id);
    }

    @GetMapping("/dang-ky/me")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.DangKyDto> dangKyCuaToi(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/sinh-vien/me/thoi-khoa-bieu")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.thoiKhoaBieu(hocKyId);
    }

    @GetMapping("/sinh-vien/me/thong-bao-nghi")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaSinhVien(hocKyId);
    }

    @GetMapping("/sinh-vien/me/diem")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.KetQuaHocTapDto ketQuaHocTap() {
        return diemService.ketQuaHocTap();
    }
}
