package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/khoa")
    public List<CatalogDtos.KhoaDto> danhSachKhoa() {
        return catalogService.danhSachKhoa();
    }

    @PostMapping("/khoa")
    public CatalogDtos.KhoaDto taoKhoa(@Valid @RequestBody CatalogDtos.KhoaRequest request) {
        return catalogService.taoKhoa(request);
    }

    @PutMapping("/khoa/{id}")
    public CatalogDtos.KhoaDto suaKhoa(@PathVariable Long id, @Valid @RequestBody CatalogDtos.KhoaRequest request) {
        return catalogService.suaKhoa(id, request);
    }

    @DeleteMapping("/khoa/{id}")
    public void xoaKhoa(@PathVariable Long id) {
        catalogService.xoaKhoa(id);
    }

    @GetMapping("/nganh")
    public List<CatalogDtos.NganhDto> danhSachNganh() {
        return catalogService.danhSachNganh();
    }

    @PostMapping("/nganh")
    public CatalogDtos.NganhDto taoNganh(@Valid @RequestBody CatalogDtos.NganhRequest request) {
        return catalogService.taoNganh(request);
    }

    @PutMapping("/nganh/{id}")
    public CatalogDtos.NganhDto suaNganh(@PathVariable Long id,
                                         @Valid @RequestBody CatalogDtos.NganhRequest request) {
        return catalogService.suaNganh(id, request);
    }

    @DeleteMapping("/nganh/{id}")
    public void xoaNganh(@PathVariable Long id) {
        catalogService.xoaNganh(id);
    }

    @GetMapping("/lop-hanh-chinh")
    public List<CatalogDtos.LopHanhChinhDto> danhSachLopHanhChinh() {
        return catalogService.danhSachLopHanhChinh();
    }

    @PostMapping("/lop-hanh-chinh")
    public CatalogDtos.LopHanhChinhDto taoLopHanhChinh(
            @Valid @RequestBody CatalogDtos.LopHanhChinhRequest request) {
        return catalogService.taoLopHanhChinh(request);
    }

    @PutMapping("/lop-hanh-chinh/{id}")
    public CatalogDtos.LopHanhChinhDto suaLopHanhChinh(
            @PathVariable Long id, @Valid @RequestBody CatalogDtos.LopHanhChinhRequest request) {
        return catalogService.suaLopHanhChinh(id, request);
    }

    @DeleteMapping("/lop-hanh-chinh/{id}")
    public void xoaLopHanhChinh(@PathVariable Long id) {
        catalogService.xoaLopHanhChinh(id);
    }

    @GetMapping("/mon-hoc")
    public List<CatalogDtos.MonHocDto> danhSachMonHoc() {
        return catalogService.danhSachMonHoc();
    }

    @PostMapping("/mon-hoc")
    public CatalogDtos.MonHocDto taoMonHoc(@Valid @RequestBody CatalogDtos.MonHocRequest request) {
        return catalogService.taoMonHoc(request);
    }

    @PutMapping("/mon-hoc/{id}")
    public CatalogDtos.MonHocDto suaMonHoc(@PathVariable Long id,
                                           @Valid @RequestBody CatalogDtos.MonHocRequest request) {
        return catalogService.suaMonHoc(id, request);
    }

    @DeleteMapping("/mon-hoc/{id}")
    public void xoaMonHoc(@PathVariable Long id) {
        catalogService.xoaMonHoc(id);
    }

    @GetMapping("/mon-hoc/{id}/tien-quyet")
    public List<CatalogDtos.MonTienQuyetDto> danhSachTienQuyet(@PathVariable Long id) {
        return catalogService.danhSachTienQuyet(id);
    }

    @PostMapping("/mon-hoc/{id}/tien-quyet")
    public CatalogDtos.MonTienQuyetDto themTienQuyet(
            @PathVariable Long id, @Valid @RequestBody CatalogDtos.MonTienQuyetRequest request) {
        return catalogService.themTienQuyet(id, request);
    }

    @DeleteMapping("/mon-hoc/{id}/tien-quyet/{lienKetId}")
    public void xoaTienQuyet(@PathVariable Long id, @PathVariable Long lienKetId) {
        catalogService.xoaTienQuyet(id, lienKetId);
    }

    @GetMapping("/hoc-ky")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return catalogService.danhSachHocKy();
    }

    @PostMapping("/hoc-ky")
    public CatalogDtos.HocKyDto taoHocKy(@Valid @RequestBody CatalogDtos.HocKyRequest request) {
        return catalogService.taoHocKy(request);
    }

    @PutMapping("/hoc-ky/{id}")
    public CatalogDtos.HocKyDto suaHocKy(@PathVariable Long id,
                                         @Valid @RequestBody CatalogDtos.HocKyRequest request) {
        return catalogService.suaHocKy(id, request);
    }

    @DeleteMapping("/hoc-ky/{id}")
    public void xoaHocKy(@PathVariable Long id) {
        catalogService.xoaHocKy(id);
    }
}
