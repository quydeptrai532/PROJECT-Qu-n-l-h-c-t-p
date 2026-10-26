package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.service.HocKyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommonController {

    private final HocKyService hocKyService;

    public CommonController(HocKyService hocKyService) {
        this.hocKyService = hocKyService;
    }

    @GetMapping("/hoc-ky")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyService.danhSach();
    }
}
