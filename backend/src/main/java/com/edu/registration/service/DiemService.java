package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiemService {

    private static final Logger log = LoggerFactory.getLogger(DiemService.class);
    private static final double DIEM_DAT = 5.0;

    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;

    public DiemService(DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                       LopHocPhanRepository lopHocPhanRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, CurrentUser currentUser, DtoMapper mapper) {
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DiemDto> danhSachDiemLop(Long lopHocPhanId) {
        GiangVien giangVien = giangVienHienTai();
        LopHocPhan lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (!lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemMap = layDiemTheoDangKy(danhSach);
        return danhSach.stream()
                .map(dk -> mapper.diem(dk, diemMap.get(dk.getId())))
                .toList();
    }

    public RegistrationDtos.DiemDto nhapDiem(Long dangKyId, RegistrationDtos.NhapDiemRequest request) {
        GiangVien giangVien = giangVienHienTai();
        DangKyHocPhan dangKy = dangKyHocPhanRepository.findById(dangKyId)
                .orElseThrow(() -> ApiException.notFound("Đăng ký không tồn tại"));
        if (dangKy.getTrangThai() != TrangThaiDangKy.DA_DANG_KY) {
            throw ApiException.badRequest("Đăng ký này đã bị hủy, không thể nhập điểm");
        }
        LopHocPhan lop = dangKy.getLopHocPhan();
        if (!lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }

        double tongKet = Math.round((0.4 * request.diemQuaTrinh() + 0.6 * request.diemCuoiKy()) * 10.0) / 10.0;

        Diem diem = diemRepository.findByDangKyId(dangKyId).orElseGet(() -> {
            Diem moi = new Diem();
            moi.setDangKy(dangKy);
            return moi;
        });
        diem.setDiemQuaTrinh(request.diemQuaTrinh());
        diem.setDiemCuoiKy(request.diemCuoiKy());
        diem.setDiemTongKet(tongKet);
        diem.setCapNhatLuc(java.time.LocalDateTime.now());
        Diem daLuu = diemRepository.save(diem);

        log.info("Giảng viên {} nhập điểm cho sinh viên {} lớp {}: QT={}, CK={}, TK={}",
                giangVien.getMa(), dangKy.getSinhVien().getMa(), lop.getMa(),
                request.diemQuaTrinh(), request.diemCuoiKy(), tongKet);
        return mapper.diem(dangKy, daLuu);
    }

    @Transactional(readOnly = true)
    public RegistrationDtos.KetQuaHocTapDto ketQuaHocTap() {
        SinhVien sinhVien = sinhVienHienTai();
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findAllActiveOfStudent(sinhVien.getId(), TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemMap = layDiemTheoDangKy(danhSach);

        List<RegistrationDtos.DangKyDto> items = danhSach.stream()
                .map(dk -> mapper.dangKy(dk, diemMap.get(dk.getId())))
                .toList();

        Map<Long, List<RegistrationDtos.DangKyDto>> theoHocKy = items.stream()
                .collect(Collectors.groupingBy(RegistrationDtos.DangKyDto::hocKyId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RegistrationDtos.HocKyTongKetDto> tongKetTheoHocKy = new ArrayList<>();
        theoHocKy.forEach((hocKyId, danhSachMon) -> {
            RegistrationDtos.DangKyDto dau = danhSachMon.get(0);
            TongHop tongHop = tinhTongHop(danhSachMon);
            tongKetTheoHocKy.add(new RegistrationDtos.HocKyTongKetDto(hocKyId, dau.tenHocKy(),
                    dau.namHoc(), tongHop.tongTinChi(), tongHop.tinChiCoDiem(), tongHop.gpa10(), tongHop.gpa4()));
        });

        TongHop tongHop = tinhTongHop(items);
        return new RegistrationDtos.KetQuaHocTapDto(items, tongKetTheoHocKy, tongHop.tongTinChi(),
                tongHop.gpa10(), tongHop.gpa4(), tongHop.tinChiTichLuy());
    }

    private TongHop tinhTongHop(List<RegistrationDtos.DangKyDto> items) {
        int tongTinChi = 0;
        int tinChiCoDiem = 0;
        int tinChiTichLuy = 0;
        double tongDiem10 = 0;
        double tongDiem4 = 0;

        for (RegistrationDtos.DangKyDto item : items) {
            tongTinChi += item.soTinChi();
            if (item.diemTongKet() == null) {
                continue;
            }
            tinChiCoDiem += item.soTinChi();
            tongDiem10 += item.diemTongKet() * item.soTinChi();
            tongDiem4 += quyDoi4(item.diemTongKet()) * item.soTinChi();
            if (item.diemTongKet() >= DIEM_DAT) {
                tinChiTichLuy += item.soTinChi();
            }
        }

        double gpa10 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem10 / tinChiCoDiem) * 100.0) / 100.0;
        double gpa4 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
        return new TongHop(tongTinChi, tinChiCoDiem, gpa10, gpa4, tinChiTichLuy);
    }

    private double quyDoi4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= 5.0) return 1.5;
        if (diem10 >= 4.0) return 1.0;
        return 0.0;
    }

    private Map<Long, Diem> layDiemTheoDangKy(List<DangKyHocPhan> danhSach) {
        if (danhSach.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = danhSach.stream().map(DangKyHocPhan::getId).toList();
        return diemRepository.findByDangKyIdIn(ids).stream()
                .collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));
    }

    private SinhVien sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private GiangVien giangVienHienTai() {
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    private record TongHop(int tongTinChi, int tinChiCoDiem, double gpa10, double gpa4, int tinChiTichLuy) {
    }
}
