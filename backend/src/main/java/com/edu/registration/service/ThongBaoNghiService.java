package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.ThongBaoNghi;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.ThongBaoNghiRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ThongBaoNghiService {

    private final ThongBaoNghiRepository thongBaoNghiRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final GiangVienRepository giangVienRepository;
    private final SinhVienRepository sinhVienRepository;
    private final HocKyService hocKyService;
    private final CurrentUser currentUser;

    public ThongBaoNghiService(ThongBaoNghiRepository thongBaoNghiRepository,
                               LopHocPhanRepository lopHocPhanRepository,
                               GiangVienRepository giangVienRepository,
                               SinhVienRepository sinhVienRepository,
                               HocKyService hocKyService, CurrentUser currentUser) {
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThongBaoNghiDto> cuaGiangVien(Long hocKyId) {
        GiangVien giangVien = giangVienHienTai();
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        return thongBaoNghiRepository.cuaGiangVien(giangVien.getId(), hocKy.getId())
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThongBaoNghiDto> cuaSinhVien(Long hocKyId) {
        SinhVien sinhVien = sinhVienHienTai();
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        return thongBaoNghiRepository
                .cuaSinhVien(sinhVien.getId(), hocKy.getId(), TrangThaiDangKy.DA_DANG_KY)
                .stream().map(this::map).toList();
    }

    public RegistrationDtos.ThongBaoNghiDto tao(RegistrationDtos.ThongBaoNghiRequest request) {
        GiangVien giangVien = giangVienHienTai();
        LopHocPhan lop = lopHocPhanRepository.findWithDetailsById(request.lopHocPhanId())
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (!lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }

        LocalDate ngayNghi = request.ngayNghi();
        HocKy hocKy = lop.getHocKy();
        if (ngayNghi.isBefore(hocKy.getNgayBatDau()) || ngayNghi.isAfter(hocKy.getNgayKetThuc())) {
            throw ApiException.badRequest("Ngày nghỉ phải nằm trong thời gian của học kỳ");
        }
        if (ngayNghi.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Không thể thông báo nghỉ cho buổi học đã diễn ra");
        }

        int thu = ngayNghi.getDayOfWeek().getValue() + 1;
        if (thu != lop.getThu()) {
            throw ApiException.badRequest("Ngày nghỉ phải rơi vào " + thuLabel(lop.getThu())
                    + " (lịch học của lớp " + lop.getMa() + ")");
        }

        if (thongBaoNghiRepository.existsByLopHocPhanIdAndNgayNghi(lop.getId(), ngayNghi)) {
            throw ApiException.conflict("Đã có thông báo nghỉ cho buổi học này");
        }

        ThongBaoNghi thongBao = new ThongBaoNghi();
        thongBao.setLopHocPhan(lop);
        thongBao.setGiangVien(giangVien);
        thongBao.setNgayNghi(ngayNghi);
        thongBao.setLyDo(request.lyDo() == null ? null : request.lyDo().trim());
        thongBao.setThoiGianTao(LocalDateTime.now());
        return map(thongBaoNghiRepository.save(thongBao));
    }

    public void xoa(Long id) {
        GiangVien giangVien = giangVienHienTai();
        ThongBaoNghi thongBao = thongBaoNghiRepository
                .findByIdAndLopHocPhanGiangVienId(id, giangVien.getId())
                .orElseThrow(() -> ApiException.notFound("Thông báo nghỉ không tồn tại"));
        thongBaoNghiRepository.delete(thongBao);
    }

    private RegistrationDtos.ThongBaoNghiDto map(ThongBaoNghi thongBao) {
        LopHocPhan lop = thongBao.getLopHocPhan();
        return new RegistrationDtos.ThongBaoNghiDto(
                thongBao.getId(), lop.getId(), lop.getMa(), lop.getMonHoc().getTen(),
                thongBao.getNgayNghi(), thongBao.getLyDo(),
                thongBao.getGiangVien() != null ? thongBao.getGiangVien().getHoTen() : lop.getGiangVien().getHoTen(),
                thongBao.getThoiGianTao());
    }

    private String thuLabel(int thu) {
        return switch (thu) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            case 8 -> "Chủ nhật";
            default -> "thứ " + thu;
        };
    }

    private GiangVien giangVienHienTai() {
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    private SinhVien sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }
}
