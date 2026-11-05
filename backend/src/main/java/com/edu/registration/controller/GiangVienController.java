package com.edu.registration.controller;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.service.DiemService;
import com.edu.registration.service.GiangVienDashboardService;
import com.edu.registration.service.LopHocPhanService;
import com.edu.registration.service.LopSinhVienExportService;
import com.edu.registration.service.StudentAcademicProfileService;
import com.edu.registration.service.ThongBaoNghiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('GIANG_VIEN')")
public class GiangVienController {

    private final LopHocPhanService lopHocPhanService;
    private final DiemService diemService;
    private final ThongBaoNghiService thongBaoNghiService;
    private final LopSinhVienExportService lopSinhVienExportService;
    private final GiangVienDashboardService giangVienDashboardService;
    private final StudentAcademicProfileService studentAcademicProfileService;

    public GiangVienController(LopHocPhanService lopHocPhanService, DiemService diemService,
                               ThongBaoNghiService thongBaoNghiService, LopSinhVienExportService lopSinhVienExportService,
                               GiangVienDashboardService giangVienDashboardService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, null);
    }

    @Autowired
    public GiangVienController(LopHocPhanService lopHocPhanService, DiemService diemService,
                               ThongBaoNghiService thongBaoNghiService, LopSinhVienExportService lopSinhVienExportService,
                               GiangVienDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
        this.thongBaoNghiService = thongBaoNghiService;
        this.lopSinhVienExportService = lopSinhVienExportService;
        this.giangVienDashboardService = giangVienDashboardService;
        this.studentAcademicProfileService = studentAcademicProfileService;
    }

    @GetMapping("/giang-vien/me/ho-so")
    public LecturerDtos.LecturerProfileDto hoSoCuaToi() {
        return giangVienDashboardService.hoSo();
    }

    @GetMapping("/giang-vien/me/dashboard")
    public LecturerDtos.LecturerDashboardDto dashboard(@RequestParam(required = false) Long hocKyId) {
        return giangVienDashboardService.dashboard(hocKyId);
    }

    @GetMapping("/giang-vien/me/lop-hoc-phan")
    public List<LopHocPhanDtos.LopHocPhanDto> lopCuaToi(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thoi-khoa-bieu")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.thoiKhoaBieuCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thong-bao-nghi")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaGiangVien(hocKyId);
    }

    @PostMapping("/giang-vien/me/thong-bao-nghi")
    public RegistrationDtos.ThongBaoNghiDto taoThongBaoNghi(
            @Valid @RequestBody RegistrationDtos.ThongBaoNghiRequest request) {
        return thongBaoNghiService.tao(request);
    }

    @DeleteMapping("/giang-vien/me/thong-bao-nghi/{id}")
    public void xoaThongBaoNghi(@PathVariable Long id) {
        thongBaoNghiService.xoa(id);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien")
    public List<RegistrationDtos.DiemDto> danhSachSinhVien(
            @PathVariable Long id,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return diemService.danhSachDiemLop(id, kw);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}")
    public LecturerDtos.StudentMinimalProfileDto chiTietSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, lopId);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so",
                 "/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so-dao-tao"})
    public StudentAcademicProfileDto hoSoDaoTaoSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, lopId);
    }

    @GetMapping("/giang-vien/sinh-vien")
    public List<LecturerDtos.StudentMinimalProfileDto> danhSachSinhVienPhuTrach(
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return studentAcademicProfileService.getAssignedStudentsForLecturer(lopHocPhanId, kw);
    }

    @GetMapping("/giang-vien/sinh-vien/{studentId}")
    public LecturerDtos.StudentMinimalProfileDto hoSoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, null);
    }

    @GetMapping({"/giang-vien/sinh-vien/{studentId}/ho-so", "/giang-vien/sinh-vien/{studentId}/ho-so-dao-tao"})
    public StudentAcademicProfileDto hoSoDaoTaoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, null);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv")
    public ResponseEntity<byte[]> xuatDanhSachSinhVien(@PathVariable Long id,
                                                       @RequestParam(required = false) String tuKhoa,
                                                       @RequestParam(required = false) String sapXep) {
        LopSinhVienExportService.CsvFile tep = lopSinhVienExportService.xuatDanhSach(id, tuKhoa, sapXep);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(tep.tenTep(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @PutMapping("/diem/{dangKyId}")
    public RegistrationDtos.DiemDto nhapDiem(@PathVariable Long dangKyId,
                                            @Valid @RequestBody RegistrationDtos.NhapDiemRequest request) {
        return diemService.nhapDiem(dangKyId, request);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/trang-thai-bang-diem", "/diem/lop-hoc-phan/{id}/trang-thai"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem", "/diem/lop-hoc-phan/{id}/gui-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto guiDuyetBangDiem(@PathVariable Long id) {
        return diemService.guiDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/phe-duyet-diem", "/diem/lop-hoc-phan/{id}/phe-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/cong-bo-diem", "/diem/lop-hoc-phan/{id}/cong-bo"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem", "/diem/lop-hoc-phan/{id}/thu-hoi-cong-bo"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(@PathVariable Long id) {
        return diemService.thuHoiCongBoBangDiem(id);
    }
}
