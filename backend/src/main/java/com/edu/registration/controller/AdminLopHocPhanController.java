package com.edu.registration.controller;

import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.service.LopHocPhanService;
import jakarta.validation.Valid;
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

    public AdminLopHocPhanController(LopHocPhanService lopHocPhanService) {
        this.lopHocPhanService = lopHocPhanService;
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

    @DeleteMapping("/lop-hoc-phan/{id}")
    public void xoa(@PathVariable Long id) {
        lopHocPhanService.xoa(id);
    }
}
