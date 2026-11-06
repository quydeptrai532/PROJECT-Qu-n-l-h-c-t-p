package com.edu.registration.service;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.TrangThaiBangDiem;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecturer self-service profile and teaching dashboard (QLSV-66).
 * Every query is scoped to the lecturer bound to the authenticated user; no lecturer id is
 * accepted from the client.
 */
@Service
@Transactional(readOnly = true)
public class GiangVienDashboardService {

    private static final String CHUA_CAP_NHAT = "Chưa cập nhật";

    private final GiangVienRepository giangVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final HocKyService hocKyService;
    private final CurrentUser currentUser;

    public GiangVienDashboardService(GiangVienRepository giangVienRepository,
                                     LopHocPhanRepository lopHocPhanRepository,
                                     DangKyHocPhanRepository dangKyHocPhanRepository,
                                     DiemRepository diemRepository,
                                     HocKyService hocKyService,
                                     CurrentUser currentUser) {
        this.giangVienRepository = giangVienRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
    }

    public LecturerDtos.LecturerProfileDto hoSo() {
        User user = currentUser.get();
        GiangVien gv = giangVienHienTai(user);
        Khoa khoa = gv.getKhoa();
        return new LecturerDtos.LecturerProfileDto(
                user.getUsername(), user.getRole().name(), gv.getMa(), gv.getHoTen(),
                gv.getEmail(), gv.getSoDienThoai(),
                khoa != null ? khoa.getMa() : CHUA_CAP_NHAT,
                khoa != null ? khoa.getTen() : CHUA_CAP_NHAT,
                user.isTrangThai(),
                AuthService.SELF_EDITABLE_PROFILE_FIELDS);
    }

    public LecturerDtos.LecturerDashboardDto dashboard(Long hocKyId) {
        GiangVien gv = giangVienHienTai(currentUser.get());
        HocKy hocKy = hocKyId != null ? hocKyService.lay(hocKyId) : hocKyService.hocKyHienTai();
        if (hocKy == null) {
            return new LecturerDtos.LecturerDashboardDto(null,
                    new LecturerDtos.TongQuanDto(0, 0, 0, 0, 0, 0, 0, 0), List.of(), List.of(), List.of());
        }

        List<LopHocPhan> danhSachLop = lopHocPhanRepository
                .findByGiangVienIdAndHocKyIdOrderByMaAsc(gv.getId(), hocKy.getId());

        Map<Long, List<DangKyHocPhan>> dangKyTheoLop = new HashMap<>();
        List<Long> tatCaDangKyId = new ArrayList<>();
        for (LopHocPhan lop : danhSachLop) {
            List<DangKyHocPhan> dangKy = dangKyHocPhanRepository.findByLopHocPhan(lop.getId(), TrangThaiDangKy.DA_DANG_KY);
            dangKyTheoLop.put(lop.getId(), dangKy);
            dangKy.forEach(dk -> tatCaDangKyId.add(dk.getId()));
        }
        Map<Long, Diem> diemTheoDangKy = tatCaDangKyId.isEmpty() ? Map.of() : diemRepository
                .findByDangKyIdIn(tatCaDangKyId).stream()
                .collect(Collectors.toMap(d -> d.getDangKy().getId(), Function.identity(), (a, b) -> a));

        List<LecturerDtos.LopPhuTrachDto> lopPhuTrach = new ArrayList<>();
        List<LecturerDtos.CongViecDto> congViec = new ArrayList<>();
        int tongSinhVien = 0;
        int tongSucChua = 0;
        Map<TrangThaiBangDiem, Integer> demTrangThai = new HashMap<>();

        for (LopHocPhan lop : danhSachLop) {
            List<DangKyHocPhan> dangKy = dangKyTheoLop.getOrDefault(lop.getId(), List.of());
            int soDangHoc = dangKy.size();
            int soDaCoDiem = (int) dangKy.stream()
                    .filter(dk -> DiemService.laDiemDayDu(diemTheoDangKy.get(dk.getId())))
                    .count();
            TrangThaiBangDiem trangThai = lop.getTrangThaiBangDiemHieuLuc();

            tongSinhVien += lop.getSiSoHienTai();
            tongSucChua += lop.getSiSoToiDa();
            demTrangThai.merge(trangThai, 1, Integer::sum);

            lopPhuTrach.add(new LecturerDtos.LopPhuTrachDto(lop.getId(), lop.getMa(),
                    lop.getMonHoc().getMa(), lop.getMonHoc().getTen(), lop.getMonHoc().getSoTinChi(),
                    lop.getPhong(), lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                    lop.getSiSoHienTai(), lop.getSiSoToiDa(), trangThai.name(),
                    soDangHoc, soDaCoDiem, lop.getGuiDuyetBangDiemLuc()));

            LecturerDtos.CongViecDto viec = congViecCuaLop(lop, trangThai, soDangHoc, soDaCoDiem);
            if (viec != null) {
                congViec.add(viec);
            }
        }

        List<RegistrationDtos.ThoiKhoaBieuDto> lichDay = danhSachLop.stream()
                .sorted(Comparator.comparingInt(LopHocPhan::getThu).thenComparingInt(LopHocPhan::getTietBatDau))
                .map(lop -> new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(),
                        lop.getMonHoc().getTen(), lop.getPhong(), lop.getThu(), lop.getTietBatDau(),
                        lop.getSoTiet(), gv.getHoTen()))
                .toList();

        LecturerDtos.TongQuanDto tongQuan = new LecturerDtos.TongQuanDto(
                danhSachLop.size(), tongSinhVien, tongSucChua, lichDay.size(),
                demTrangThai.getOrDefault(TrangThaiBangDiem.NHAP, 0),
                demTrangThai.getOrDefault(TrangThaiBangDiem.CHO_DUYET, 0),
                demTrangThai.getOrDefault(TrangThaiBangDiem.DA_DUYET, 0),
                demTrangThai.getOrDefault(TrangThaiBangDiem.DA_CONG_BO, 0));

        LecturerDtos.HocKyTomTatDto hocKyDto = new LecturerDtos.HocKyTomTatDto(hocKy.getId(), hocKy.getTen(),
                hocKy.getNamHoc(), hocKy.getNgayBatDau(), hocKy.getNgayKetThuc());
        return new LecturerDtos.LecturerDashboardDto(hocKyDto, tongQuan, lopPhuTrach, lichDay, congViec);
    }

    private LecturerDtos.CongViecDto congViecCuaLop(LopHocPhan lop, TrangThaiBangDiem trangThai,
                                                    int soDangHoc, int soDaCoDiem) {
        String lienKet = "/lop-cua-toi/" + lop.getId() + "/nhap-diem";
        String tenMon = lop.getMonHoc().getTen();
        return switch (trangThai) {
            case NHAP -> {
                if (soDangHoc == 0) {
                    yield null;
                }
                if (soDaCoDiem < soDangHoc) {
                    yield new LecturerDtos.CongViecDto("NHAP_DIEM", lop.getId(), lop.getMa(), tenMon,
                            "Bảng điểm nháp còn thiếu điểm",
                            "Đã nhập đủ điểm " + soDaCoDiem + "/" + soDangHoc + " sinh viên đang học.", lienKet);
                }
                yield new LecturerDtos.CongViecDto("GUI_DUYET", lop.getId(), lop.getMa(), tenMon,
                        "Bảng điểm nháp đã đủ điểm, chưa gửi duyệt",
                        "Đã nhập đủ điểm " + soDangHoc + "/" + soDangHoc + " sinh viên đang học.", lienKet);
            }
            case CHO_DUYET -> new LecturerDtos.CongViecDto("CHO_DUYET", lop.getId(), lop.getMa(), tenMon,
                    "Bảng điểm đang chờ duyệt", "Bảng điểm đã được gửi duyệt và chưa được phê duyệt.", lienKet);
            case DA_DUYET -> new LecturerDtos.CongViecDto("CHO_CONG_BO", lop.getId(), lop.getMa(), tenMon,
                    "Bảng điểm đã duyệt, chưa công bố", "Sinh viên chưa xem được điểm cho đến khi bảng điểm được công bố.",
                    lienKet);
            case DA_CONG_BO -> null;
        };
    }

    private GiangVien giangVienHienTai(User user) {
        return giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }
}
