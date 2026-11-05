package com.edu.registration.service;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SinhVienDashboardService {

    private final SinhVienRepository sinhVienRepository;
    private final DangKyHocPhanRepository dangKyRepository;
    private final DiemRepository diemRepository;
    private final HocKyRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final int upcomingWindowDays;

    public SinhVienDashboardService(SinhVienRepository sinhVienRepository,
                                   DangKyHocPhanRepository dangKyRepository,
                                   DiemRepository diemRepository,
                                   HocKyRepository hocKyRepository,
                                   CurrentUser currentUser,
                                   @Value("${app.dashboard.upcoming-window-days:14}") int upcomingWindowDays) {
        this.sinhVienRepository = sinhVienRepository;
        this.dangKyRepository = dangKyRepository;
        this.diemRepository = diemRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        this.upcomingWindowDays = Math.max(0, upcomingWindowDays);
    }

    public List<CanhBaoDto> canhBao() {
        SinhVien sinhVien = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        List<DangKyHocPhan> dangKy = dangKyRepository.findAllActiveOfStudent(
                sinhVien.getId(), TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemTheoDangKy = dangKy.isEmpty() ? Map.of() : diemRepository
                .findByDangKyIdIn(dangKy.stream().map(DangKyHocPhan::getId).toList()).stream()
                .collect(Collectors.toMap(d -> d.getDangKy().getId(), Function.identity()));

        List<CanhBaoDto> canhBao = new ArrayList<>();
        themMonChuaDat(dangKy, diemTheoDangKy, canhBao);
        themMocDangKy(canhBao);
        return canhBao;
    }

    private void themMonChuaDat(List<DangKyHocPhan> dangKy, Map<Long, Diem> diemTheoDangKy,
                                List<CanhBaoDto> canhBao) {
        Map<Long, DangKyHocPhan> ketQuaMoiNhatTheoMon = new HashMap<>();
        for (DangKyHocPhan dk : dangKy) {
            Diem diem = diemTheoDangKy.get(dk.getId());
            if (diem == null || diem.getDiemTongKet() == null || !dk.getLopHocPhan().isBangDiemDaCongBo()) continue;
            Long monHocId = dk.getLopHocPhan().getMonHoc().getId();
            DangKyHocPhan hienTai = ketQuaMoiNhatTheoMon.get(monHocId);
            if (hienTai == null || soSanhLanHoc(dk, hienTai) > 0) ketQuaMoiNhatTheoMon.put(monHocId, dk);
        }

        ketQuaMoiNhatTheoMon.values().stream()
                .filter(dk -> diemTheoDangKy.get(dk.getId()).getDiemTongKet() < AcademicRules.DIEM_DAT)
                .sorted(Comparator.comparing(dk -> dk.getLopHocPhan().getMonHoc().getMa()))
                .forEach(dk -> {
                    var lop = dk.getLopHocPhan();
                    var mon = lop.getMonHoc();
                    HocKy ky = lop.getHocKy();
                    double diem = diemTheoDangKy.get(dk.getId()).getDiemTongKet();
                    canhBao.add(new CanhBaoDto("MON_CHUA_DAT", "Cần học lại: " + mon.getTen(),
                            "Điểm tổng kết " + diem + " dưới ngưỡng đạt " + AcademicRules.DIEM_DAT + ".",
                            ky.getId(), ky.getTen(), ky.getNamHoc(), "/diem"));
                });
    }

    private int soSanhLanHoc(DangKyHocPhan a, DangKyHocPhan b) {
        int theoKy = a.getLopHocPhan().getHocKy().getNgayBatDau()
                .compareTo(b.getLopHocPhan().getHocKy().getNgayBatDau());
        if (theoKy != 0) return theoKy;
        return a.getThoiGianDangKy().compareTo(b.getThoiGianDangKy());
    }

    private void themMocDangKy(List<CanhBaoDto> canhBao) {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(upcomingWindowDays);
        for (HocKy ky : hocKyRepository.findAllByOrderByNgayBatDauDesc()) {
            LocalDate opens = ky.getDangKyTu();
            LocalDate closes = ky.getDangKyDen();
            String kyName = ky.getTen() + " " + ky.getNamHoc();
            if (opens != null && !opens.isBefore(today) && !opens.isAfter(windowEnd)) {
                canhBao.add(new CanhBaoDto("DANG_KY_SAP_MO", "Sắp mở đăng ký: " + kyName,
                        "Đợt đăng ký dự kiến mở ngày " + opens + ".", ky.getId(), ky.getTen(),
                        ky.getNamHoc(), "/dang-ky"));
            }
            if (opens != null && closes != null && !today.isBefore(opens)
                    && !closes.isBefore(today) && !closes.isAfter(windowEnd)) {
                canhBao.add(new CanhBaoDto("DANG_KY_SAP_DONG", "Sắp đóng đăng ký: " + kyName,
                        "Hạn đăng ký là ngày " + closes + ".", ky.getId(), ky.getTen(),
                        ky.getNamHoc(), "/dang-ky"));
            }
        }
    }

    public record CanhBaoDto(String loai, String tieuDe, String lyDo, Long hocKyId,
                             String tenHocKy, String namHoc, String lienKet) { }
}
