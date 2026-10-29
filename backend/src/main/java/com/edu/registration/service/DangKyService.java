package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.MonTienQuyet;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonTienQuyetRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class DangKyService {

    private static final Logger log = LoggerFactory.getLogger(DangKyService.class);
    private static final double DIEM_DAT = 5.0;

    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DiemRepository diemRepository;
    private final MonTienQuyetRepository monTienQuyetRepository;
    private final SinhVienRepository sinhVienRepository;
    private final CurrentUser currentUser;
    private final HocKyService hocKyService;
    private final DtoMapper mapper;
    private final AuditLogService auditLogService;

    public DangKyService(DangKyHocPhanRepository dangKyHocPhanRepository,
                         LopHocPhanRepository lopHocPhanRepository, DiemRepository diemRepository,
                         MonTienQuyetRepository monTienQuyetRepository, SinhVienRepository sinhVienRepository,
                         CurrentUser currentUser, HocKyService hocKyService, DtoMapper mapper,
                         AuditLogService auditLogService) {
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.diemRepository = diemRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.currentUser = currentUser;
        this.hocKyService = hocKyService;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    // Serialize one student's cross-section checks; READ_COMMITTED refreshes MySQL reads after lock waits.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RegistrationDtos.DangKyDto dangKy(Long lopHocPhanId) {
        SinhVien sinhVien = sinhVienHienTaiForUpdate();
        return dangKyTrongGiaoDich(sinhVien, lopHocPhanId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<RegistrationDtos.DangKyDto> xacNhanKeHoach(List<Long> lopHocPhanIds) {
        if (lopHocPhanIds == null || lopHocPhanIds.isEmpty()) {
            return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", null,
                    audit -> { throw ApiException.badRequest("Giỏ học phần đang trống"); });
        }
        if (new HashSet<>(lopHocPhanIds).size() != lopHocPhanIds.size()) {
            Long duplicateId = lopHocPhanIds.stream().filter(id -> java.util.Collections.frequency(lopHocPhanIds, id) > 1)
                    .findFirst().orElse(null);
            return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", duplicateId,
                    audit -> { throw ApiException.badRequest("Giỏ học phần có lớp bị lặp"); });
        }

        SinhVien sinhVien = sinhVienHienTaiForUpdate();
        List<RegistrationDtos.DangKyDto> ketQua = new ArrayList<>();
        // Lock section rows in a stable order so overlapping multi-class plans do not deadlock.
        for (Long lopHocPhanId : lopHocPhanIds.stream().sorted().toList()) {
            ketQua.add(dangKyTrongGiaoDich(sinhVien, lopHocPhanId));
        }
        return ketQua;
    }

    private RegistrationDtos.DangKyDto dangKyTrongGiaoDich(SinhVien sinhVien, Long lopHocPhanId) {
        return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", lopHocPhanId, audit -> {
        LopHocPhan lop = lopHocPhanRepository.findByIdForUpdate(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        HocKy hocKy = lop.getHocKy();
        MonHoc monHoc = lop.getMonHoc();

        if (!hocKy.dangMoDangKy()) {
            throw ApiException.badRequest("Hiện không trong thời gian đăng ký của " + hocKy.getTen());
        }

        List<DangKyHocPhan> dangKyHienTai =
                dangKyHocPhanRepository.findTheoHocKy(sinhVien.getId(), hocKy.getId(), TrangThaiDangKy.DA_DANG_KY);

        for (DangKyHocPhan daDangKy : dangKyHienTai) {
            if (daDangKy.getLopHocPhan().getMonHoc().getId().equals(monHoc.getId())) {
                throw ApiException.conflict("Bạn đã đăng ký môn " + monHoc.getTen() + " trong học kỳ này");
            }
        }

        for (DangKyHocPhan daDangKy : dangKyHienTai) {
            if (daDangKy.getLopHocPhan().trungLichVoi(lop)) {
                LopHocPhan trung = daDangKy.getLopHocPhan();
                throw ApiException.conflict("Trùng lịch học với lớp " + trung.getMa() + " (thứ "
                        + trung.getThu() + ", tiết " + trung.getTietBatDau() + "-" + trung.getTietKetThuc() + ")");
            }
        }

        kiemTraTienQuyet(sinhVien, monHoc);

        if (lop.getSiSoHienTai() >= lop.getSiSoToiDa()) {
            throw ApiException.conflict("Lớp " + lop.getMa() + " đã đủ sĩ số");
        }

        int tinChiDaDangKy = dangKyHienTai.stream()
                .mapToInt(dk -> dk.getLopHocPhan().getMonHoc().getSoTinChi())
                .sum();
        if (tinChiDaDangKy + monHoc.getSoTinChi() > hocKy.getSoTinChiToiDa()) {
            throw ApiException.badRequest("Vượt quá số tín chỉ tối đa (" + hocKy.getSoTinChiToiDa()
                    + " tín chỉ) của " + hocKy.getTen());
        }

        DangKyHocPhan dangKy = dangKyHocPhanRepository
                .findBySinhVienIdAndLopHocPhanId(sinhVien.getId(), lop.getId())
                .orElseGet(() -> {
                    DangKyHocPhan moi = new DangKyHocPhan();
                    moi.setSinhVien(sinhVien);
                    moi.setLopHocPhan(lop);
                    return moi;
                });
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        dangKy.setThoiGianDangKy(LocalDateTime.now());
        lop.setSiSoHienTai(lop.getSiSoHienTai() + 1);

        DangKyHocPhan daLuu = dangKyHocPhanRepository.save(dangKy);
        Map<String, Object> before = Map.of("maLop", lop.getMa(), "siSoToiDa", lop.getSiSoToiDa(),
                "siSoHienTai", lop.getSiSoHienTai() - 1);
        audit.changes(before, Map.of("maLop", lop.getMa(), "siSoToiDa", lop.getSiSoToiDa(),
                "siSoHienTai", lop.getSiSoHienTai(), "trangThai", dangKy.getTrangThai().name(),
                "dangKyId", daLuu.getId()));
        log.info("Sinh viên {} đăng ký lớp {} (môn {})", sinhVien.getMa(), lop.getMa(), monHoc.getMa());
        return mapper.dangKy(daLuu, null);
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void huyDangKy(Long dangKyId) {
        auditLogService.execute("COURSE_REGISTRATION_CANCELLED", "COURSE_REGISTRATION", dangKyId, audit -> {
        SinhVien sinhVien = sinhVienHienTaiForUpdate();
        DangKyHocPhan dangKy = dangKyHocPhanRepository.findByIdAndSinhVienId(dangKyId, sinhVien.getId())
                .orElseThrow(() -> ApiException.notFound("Đăng ký không tồn tại"));
        if (dangKy.getTrangThai() == TrangThaiDangKy.DA_HUY) {
            throw ApiException.badRequest("Đăng ký này đã được hủy trước đó");
        }

        LopHocPhan lop = lopHocPhanRepository.findByIdForUpdate(dangKy.getLopHocPhan().getId())
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (!lop.getHocKy().dangMoDangKy()) {
            throw ApiException.badRequest("Đã hết thời gian hủy đăng ký của " + lop.getHocKy().getTen());
        }

        long tongTinChiHienTai = dangKyHocPhanRepository.tinhTongTinChi(
                sinhVien.getId(), lop.getHocKy().getId(), TrangThaiDangKy.DA_DANG_KY);
        int tinChiConLai = (int) Math.max(0, tongTinChiHienTai - lop.getMonHoc().getSoTinChi());
        if (tinChiConLai > 0 && tinChiConLai < lop.getHocKy().getSoTinChiToiThieu()) {
            throw ApiException.badRequest("Không thể hủy: tổng tín chỉ còn lại (" + tinChiConLai
                    + ") thấp hơn mức tối thiểu " + lop.getHocKy().getSoTinChiToiThieu()
                    + ". Hãy đăng ký thêm học phần hoặc hủy toàn bộ học phần của học kỳ.");
        }

        Map<String, Object> before = Map.of("dangKyId", dangKy.getId(), "lopHocPhanId", lop.getId(),
                "maLop", lop.getMa(), "trangThai", dangKy.getTrangThai().name(),
                "siSoHienTai", lop.getSiSoHienTai());
        dangKy.setTrangThai(TrangThaiDangKy.DA_HUY);
        lop.setSiSoHienTai(Math.max(0, lop.getSiSoHienTai() - 1));
        audit.changes(before, Map.of("dangKyId", dangKy.getId(), "lopHocPhanId", lop.getId(),
                "maLop", lop.getMa(), "trangThai", dangKy.getTrangThai().name(),
                "siSoHienTai", lop.getSiSoHienTai()));
        log.info("Sinh viên {} hủy đăng ký lớp {}", sinhVien.getMa(), lop.getMa());
        return null;
        });
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DangKyDto> danhSachCuaToi(Long hocKyId) {
        SinhVien sinhVien = sinhVienHienTai();
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findTheoHocKy(sinhVien.getId(), hocKy.getId(), TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemMap = layDiemTheoDangKy(danhSach);
        return danhSach.stream()
                .map(dk -> mapper.dangKy(dk, diemMap.get(dk.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(Long hocKyId) {
        SinhVien sinhVien = sinhVienHienTai();
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        return dangKyHocPhanRepository
                .findTheoHocKy(sinhVien.getId(), hocKy.getId(), TrangThaiDangKy.DA_DANG_KY)
                .stream().map(mapper::thoiKhoaBieu).toList();
    }

    private void kiemTraTienQuyet(SinhVien sinhVien, MonHoc monHoc) {
        List<MonTienQuyet> danhSachTienQuyet =
                monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHoc.getId());
        if (danhSachTienQuyet.isEmpty()) {
            return;
        }
        Set<Long> monDaDat = new HashSet<>(diemRepository.findMonDaDat(sinhVien.getId(), DIEM_DAT));
        for (MonTienQuyet lienKet : danhSachTienQuyet) {
            MonHoc tienQuyet = lienKet.getMonTienQuyet();
            if (!monDaDat.contains(tienQuyet.getId())) {
                throw ApiException.badRequest("Chưa đạt môn tiên quyết: "
                        + tienQuyet.getMa() + " - " + tienQuyet.getTen());
            }
        }
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

    private SinhVien sinhVienHienTaiForUpdate() {
        return sinhVienRepository.findByUserIdForUpdate(currentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }
}
