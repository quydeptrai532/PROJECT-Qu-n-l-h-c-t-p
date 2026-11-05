package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiBangDiem;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiemService {

    private static final Logger log = LoggerFactory.getLogger(DiemService.class);
    private static final Set<String> TRANG_THAI_KET_QUA_HOP_LE = Set.of("DAT", "CHUA_DAT", "CHUA_CO_DIEM");

    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final HocKyRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;
    private final AuditLogService auditLogService;
    private final ThongBaoService thongBaoService;

    public DiemService(DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                       LopHocPhanRepository lopHocPhanRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, CurrentUser currentUser, DtoMapper mapper) {
        this(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository, sinhVienRepository,
                giangVienRepository, null, currentUser, mapper, null, null);
    }

    public DiemService(DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                       LopHocPhanRepository lopHocPhanRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, HocKyRepository hocKyRepository,
                       CurrentUser currentUser, DtoMapper mapper) {
        this(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository, sinhVienRepository,
                giangVienRepository, hocKyRepository, currentUser, mapper, null, null);
    }

    @Autowired
    public DiemService(DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                       LopHocPhanRepository lopHocPhanRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, HocKyRepository hocKyRepository,
                       CurrentUser currentUser, DtoMapper mapper,
                       AuditLogService auditLogService, ThongBaoService thongBaoService) {
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.thongBaoService = thongBaoService;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DiemDto> danhSachDiemLop(Long lopHocPhanId) {
        return danhSachDiemLop(lopHocPhanId, null);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DiemDto> danhSachDiemLop(Long lopHocPhanId, String tuKhoa) {
        LopHocPhan lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        kiemTraQuyenQuanLyBangDiem(lop);
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String kw = tuKhoa.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(dk -> {
                        SinhVien sv = dk.getSinhVien();
                        return (sv.getMa() != null && sv.getMa().toLowerCase(Locale.ROOT).contains(kw))
                                || (sv.getHoTen() != null && sv.getHoTen().toLowerCase(Locale.ROOT).contains(kw))
                                || (sv.getEmail() != null && sv.getEmail().toLowerCase(Locale.ROOT).contains(kw));
                    })
                    .toList();
        }
        Map<Long, Diem> diemMap = layDiemTheoDangKy(danhSach);
        return danhSach.stream()
                .map(dk -> mapper.diem(dk, diemMap.get(dk.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegistrationDtos.TrangThaiBangDiemDto chiTietTrangThaiBangDiem(Long lopHocPhanId) {
        LopHocPhan lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        kiemTraQuyenQuanLyBangDiem(lop);
        List<DangKyHocPhan> activeRegistrations =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemMap = layDiemTheoDangKy(activeRegistrations);
        int soDuDieuKien = demSinhVienDuDieuKien(activeRegistrations, diemMap);
        return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), soDuDieuKien);
    }

    public RegistrationDtos.DiemDto nhapDiem(Long dangKyId, RegistrationDtos.NhapDiemRequest request) {
        DangKyHocPhan dangKy = dangKyHocPhanRepository.findById(dangKyId)
                .orElseThrow(() -> ApiException.notFound("Đăng ký không tồn tại"));
        if (dangKy.getTrangThai() != TrangThaiDangKy.DA_DANG_KY) {
            throw ApiException.badRequest("Đăng ký này đã bị hủy, không thể nhập điểm");
        }
        LopHocPhan lop = dangKy.getLopHocPhan();
        kiemTraQuyenQuanLyBangDiem(lop);

        if (lop.getTrangThaiBangDiem() == TrangThaiBangDiem.DA_CONG_BO) {
            throw ApiException.conflict("Bảng điểm lớp học phần đã được công bố, không thể chỉnh sửa trực tiếp. Vui lòng thu hồi công bố trước khi điều chỉnh");
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
        diem.setCapNhatLuc(LocalDateTime.now());
        Diem daLuu = diemRepository.save(diem);

        if (lop.getTrangThaiBangDiem() != TrangThaiBangDiem.NHAP) {
            lop.setTrangThaiBangDiem(TrangThaiBangDiem.NHAP);
            lop.setDuyetBangDiemLuc(null);
            lop.setNguoiDuyetBangDiemId(null);
            if (lop.getId() != null) {
                lopHocPhanRepository.save(lop);
            }
        }

        log.info("Đã lưu điểm nháp cho đăng ký id={} thuộc lớp {} (trạng thái bảng điểm={})",
                dangKy.getId(), lop.getMa(), lop.getTrangThaiBangDiemHieuLuc());
        return mapper.diem(dangKy, daLuu);
    }

    /**
     * Submits a class section's draft grade sheet for approval (NHAP -> CHO_DUYET).
     * Does not emit student notifications.
     */
    public RegistrationDtos.TrangThaiBangDiemDto guiDuyetBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_SUBMITTED", "GRADE_SHEET", lopHocPhanId, audit -> {
            LopHocPhan lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            kiemTraQuyenQuanLyBangDiem(lop);

            TrangThaiBangDiem trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == TrangThaiBangDiem.DA_CONG_BO) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được công bố");
            }
            if (trangThaiCu == TrangThaiBangDiem.DA_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được phê duyệt");
            }

            List<DangKyHocPhan> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
            Map<Long, Diem> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<DangKyHocPhan> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(lop, trangThaiCu, activeRegistrations.size(), eligible.size());
            LocalDateTime now = LocalDateTime.now();
            lop.setTrangThaiBangDiem(TrangThaiBangDiem.CHO_DUYET);
            lop.setGuiDuyetBangDiemLuc(now);
            lopHocPhanRepository.save(lop);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, TrangThaiBangDiem.CHO_DUYET, activeRegistrations.size(), eligible.size());
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", TrangThaiBangDiem.CHO_DUYET.name());
            audit.changes(before, after);

            log.info("Đã gửi duyệt bảng điểm lớp {} (id={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Approves a class section's grade sheet (NHAP/CHO_DUYET -> DA_DUYET).
     * Does not emit student notifications until published.
     */
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_APPROVED", "GRADE_SHEET", lopHocPhanId, audit -> {
            LopHocPhan lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenQuanLyBangDiem(lop);

            TrangThaiBangDiem trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == TrangThaiBangDiem.DA_CONG_BO) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được công bố");
            }

            List<DangKyHocPhan> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
            Map<Long, Diem> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<DangKyHocPhan> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(lop, trangThaiCu, activeRegistrations.size(), eligible.size());
            LocalDateTime now = LocalDateTime.now();
            if (lop.getGuiDuyetBangDiemLuc() == null) {
                lop.setGuiDuyetBangDiemLuc(now);
            }
            lop.setTrangThaiBangDiem(TrangThaiBangDiem.DA_DUYET);
            lop.setDuyetBangDiemLuc(now);
            lop.setNguoiDuyetBangDiemId(actor != null ? actor.getId() : null);
            lopHocPhanRepository.save(lop);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, TrangThaiBangDiem.DA_DUYET, activeRegistrations.size(), eligible.size());
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", TrangThaiBangDiem.DA_DUYET.name());
            audit.changes(before, after);

            log.info("Đã phê duyệt bảng điểm lớp {} (id={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Publishes an approved grade sheet (DA_DUYET -> DA_CONG_BO), records an audit log without personal grades,
     * and dispatches post-commit notifications to eligible students of the class section.
     * Re-publishing an already-published grade sheet is idempotent and does not duplicate notifications.
     */
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_PUBLISHED", "GRADE_SHEET", lopHocPhanId, audit -> {
            LopHocPhan lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenQuanLyBangDiem(lop);

            TrangThaiBangDiem trangThaiCu = lop.getTrangThaiBangDiem() == null
                    ? TrangThaiBangDiem.NHAP
                    : lop.getTrangThaiBangDiem();
            if (trangThaiCu == TrangThaiBangDiem.NHAP || trangThaiCu == TrangThaiBangDiem.CHO_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đang ở trạng thái " + trangThaiCu + ", cần được phê duyệt trước khi công bố");
            }

            List<DangKyHocPhan> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
            Map<Long, Diem> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<DangKyHocPhan> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(
                    lop, trangThaiCu, activeRegistrations.size(), eligible.size());

            LocalDateTime publishedAt = lop.getCongBoBangDiemLuc() != null
                    ? lop.getCongBoBangDiemLuc()
                    : LocalDateTime.now();
            int version = Math.max(1, lop.getPhienBanCongBo());

            if (trangThaiCu != TrangThaiBangDiem.DA_CONG_BO || lop.getCongBoBangDiemLuc() == null) {
                publishedAt = LocalDateTime.now();
                lop.setTrangThaiBangDiem(TrangThaiBangDiem.DA_CONG_BO);
                lop.setPhienBanCongBo(version);
                lop.setCongBoBangDiemLuc(publishedAt);
                lop.setNguoiCongBoBangDiemId(actor != null ? actor.getId() : null);
                lopHocPhanRepository.save(lop);
            }

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, TrangThaiBangDiem.DA_CONG_BO, activeRegistrations.size(), eligible.size());
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", TrangThaiBangDiem.DA_CONG_BO.name());
            audit.changes(before, after);

            notifyGradesPublishedSafely(lop, eligible, version, publishedAt);

            log.info("Đã công bố bảng điểm lớp {} (id={}, phienBan={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), version, eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Revokes publication of a grade sheet so adjustments can be made.
     * Preserves publication version so ordinary GRADES_PUBLISHED notifications are not re-sent for the same event.
     */
    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_PUBLICATION_REVOKED", "GRADE_SHEET", lopHocPhanId, audit -> {
            LopHocPhan lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            kiemTraQuyenQuanLyBangDiem(lop);

            TrangThaiBangDiem trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu != TrangThaiBangDiem.DA_CONG_BO) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " chưa ở trạng thái đã công bố");
            }

            List<DangKyHocPhan> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);
            Map<Long, Diem> diemMap = layDiemTheoDangKy(activeRegistrations);
            int soDuDieuKien = demSinhVienDuDieuKien(activeRegistrations, diemMap);

            Map<String, Object> before = gradeSheetAuditSnapshot(
                    lop, trangThaiCu, activeRegistrations.size(), soDuDieuKien);

            lop.setTrangThaiBangDiem(TrangThaiBangDiem.NHAP);
            lop.setDuyetBangDiemLuc(null);
            lop.setNguoiDuyetBangDiemId(null);
            lop.setCongBoBangDiemLuc(null);
            lop.setNguoiCongBoBangDiemId(null);
            lopHocPhanRepository.save(lop);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, TrangThaiBangDiem.NHAP, activeRegistrations.size(), soDuDieuKien);
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", TrangThaiBangDiem.NHAP.name());
            audit.changes(before, after);

            log.info("Đã thu hồi công bố bảng điểm lớp {} (id={})", lop.getMa(), lop.getId());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), soDuDieuKien);
        });
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

    @Transactional(readOnly = true)
    public RegistrationDtos.LichSuHocTapDto lichSuHocTap(Long hocKyId, String trangThai) {
        if (hocKyId != null) {
            if (hocKyId <= 0) {
                throw ApiException.badRequest("Học kỳ không hợp lệ");
            }
            if (hocKyRepository != null && !hocKyRepository.existsById(hocKyId)) {
                throw ApiException.notFound("Học kỳ không tồn tại");
            }
        }
        String statusFilter = chuanHoaTrangThaiKetQua(trangThai);

        SinhVien sinhVien = sinhVienHienTai();
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findAllActiveOfStudent(sinhVien.getId(), TrangThaiDangKy.DA_DANG_KY);
        Map<Long, Diem> diemMap = layDiemTheoDangKy(danhSach);

        Comparator<DangKyHocPhan> thuTuThoiGian = Comparator
                .comparing((DangKyHocPhan dk) -> dk.getLopHocPhan().getHocKy().getNgayBatDau(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getHocKy().getId(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMonHoc().getMa(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMonHoc().getId(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DangKyHocPhan::getThoiGianDangKy,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMa(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DangKyHocPhan::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        List<DangKyHocPhan> daSapXep = danhSach.stream().sorted(thuTuThoiGian).toList();

        Map<Long, Integer> tongSoLanTheoMon = new HashMap<>();
        for (DangKyHocPhan dk : daSapXep) {
            Long monHocId = dk.getLopHocPhan().getMonHoc().getId();
            tongSoLanTheoMon.merge(monHocId, 1, Integer::sum);
        }

        Map<Long, Integer> demLanTheoMon = new HashMap<>();
        List<RegistrationDtos.MucLichSuHocTapDto> tatCaMuc = new ArrayList<>(daSapXep.size());
        for (DangKyHocPhan dk : daSapXep) {
            LopHocPhan lop = dk.getLopHocPhan();
            MonHoc mon = lop.getMonHoc();
            HocKy ky = lop.getHocKy();
            int lanHoc = demLanTheoMon.merge(mon.getId(), 1, Integer::sum);
            int tongSoLanHoc = tongSoLanTheoMon.getOrDefault(mon.getId(), 1);

            Diem diem = diemMap.get(dk.getId());
            boolean daCongBo = lop.isBangDiemDaCongBo() && diem != null && diem.getDiemTongKet() != null;
            Double diemQuaTrinh = daCongBo ? diem.getDiemQuaTrinh() : null;
            Double diemCuoiKy = daCongBo ? diem.getDiemCuoiKy() : null;
            Double diemTongKet = daCongBo ? diem.getDiemTongKet() : null;

            String maTrangThai;
            String tenTrangThai;
            if (!daCongBo) {
                maTrangThai = "CHUA_CO_DIEM";
                tenTrangThai = "Chưa có điểm";
            } else if (diemTongKet >= AcademicRules.DIEM_DAT) {
                maTrangThai = "DAT";
                tenTrangThai = "Đạt";
            } else {
                maTrangThai = "CHUA_DAT";
                tenTrangThai = "Chưa đạt";
            }

            tatCaMuc.add(new RegistrationDtos.MucLichSuHocTapDto(
                    dk.getId(),
                    ky.getId(),
                    ky.getTen(),
                    ky.getNamHoc(),
                    ky.getNgayBatDau(),
                    mon.getId(),
                    mon.getMa(),
                    mon.getTen(),
                    mon.getSoTinChi(),
                    lop.getId(),
                    lop.getMa(),
                    lop.getGiangVien() != null ? lop.getGiangVien().getHoTen() : null,
                    dk.getThoiGianDangKy(),
                    diemQuaTrinh,
                    diemCuoiKy,
                    diemTongKet,
                    maTrangThai,
                    tenTrangThai,
                    lanHoc,
                    tongSoLanHoc));
        }

        boolean coNhieuLanHoc = tatCaMuc.stream().anyMatch(item -> item.tongSoLanHoc() > 1);

        List<RegistrationDtos.MucLichSuHocTapDto> items = tatCaMuc.stream()
                .filter(item -> hocKyId == null || item.hocKyId().equals(hocKyId))
                .filter(item -> statusFilter == null || item.trangThaiKetQua().equals(statusFilter))
                .toList();

        Map<Long, List<RegistrationDtos.MucLichSuHocTapDto>> theoHocKy = items.stream()
                .collect(Collectors.groupingBy(RegistrationDtos.MucLichSuHocTapDto::hocKyId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RegistrationDtos.HocKyTongKetDto> tongKetTheoHocKy = new ArrayList<>();
        theoHocKy.forEach((kyId, danhSachMon) -> {
            RegistrationDtos.MucLichSuHocTapDto dau = danhSachMon.get(0);
            TongHop tongHopKy = tinhTongHopLichSu(danhSachMon);
            tongKetTheoHocKy.add(new RegistrationDtos.HocKyTongKetDto(kyId, dau.tenHocKy(),
                    dau.namHoc(), tongHopKy.tongTinChi(), tongHopKy.tinChiCoDiem(),
                    tongHopKy.gpa10(), tongHopKy.gpa4()));
        });

        TongHop tongHop = tinhTongHopLichSu(items);
        List<RegistrationDtos.MucLichSuHocTapDto> monChuaDat = items.stream()
                .filter(item -> "CHUA_DAT".equals(item.trangThaiKetQua()))
                .toList();
        int soMonDat = (int) items.stream().filter(item -> "DAT".equals(item.trangThaiKetQua())).count();
        int soMonChuaDat = monChuaDat.size();
        int soMonChuaCoDiem = (int) items.stream().filter(item -> "CHUA_CO_DIEM".equals(item.trangThaiKetQua())).count();

        return new RegistrationDtos.LichSuHocTapDto(
                items,
                tongKetTheoHocKy,
                monChuaDat,
                tongHop.tongTinChi(),
                tongHop.tinChiCoDiem(),
                tongHop.tinChiTichLuy(),
                tongHop.gpa10(),
                tongHop.gpa4(),
                soMonDat,
                soMonChuaDat,
                soMonChuaCoDiem,
                coNhieuLanHoc);
    }

    private LopHocPhan layLopDeCapNhatBangDiem(Long lopHocPhanId) {
        return lopHocPhanRepository.findByIdForUpdate(lopHocPhanId)
                .or(() -> lopHocPhanRepository.findWithDetailsById(lopHocPhanId))
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
    }

    private User kiemTraQuyenQuanLyBangDiem(LopHocPhan lop) {
        User actor = currentUser.get();
        if (actor != null && actor.getRole() == Role.QUAN_TRI) {
            return actor;
        }
        GiangVien giangVien = giangVienHienTai();
        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }
        return actor;
    }

    private List<DangKyHocPhan> locDangKyCoDiemHopLe(List<DangKyHocPhan> activeRegistrations, Map<Long, Diem> diemMap) {
        List<DangKyHocPhan> eligible = new ArrayList<>();
        for (DangKyHocPhan dk : activeRegistrations) {
            if (dk.getTrangThai() != TrangThaiDangKy.DA_DANG_KY) {
                continue;
            }
            Diem diem = diemMap.get(dk.getId());
            if (laDiemDayDu(diem)) {
                eligible.add(dk);
            }
        }
        return eligible;
    }

    /** A grade row counts as complete when process, final and total scores are all present. */
    public static boolean laDiemDayDu(Diem diem) {
        return diem != null && diem.getDiemQuaTrinh() != null
                && diem.getDiemCuoiKy() != null && diem.getDiemTongKet() != null;
    }

    private int demSinhVienDuDieuKien(List<DangKyHocPhan> activeRegistrations, Map<Long, Diem> diemMap) {
        return locDangKyCoDiemHopLe(activeRegistrations, diemMap).size();
    }

    private void kiemTraDuDieuKienDuyetVaCongBo(LopHocPhan lop,
                                                List<DangKyHocPhan> activeRegistrations,
                                                List<DangKyHocPhan> eligibleRegistrations) {
        if (activeRegistrations.isEmpty()) {
            throw ApiException.badRequest("Lớp học phần " + lop.getMa() + " chưa có sinh viên đang theo học để duyệt hoặc công bố điểm");
        }
        if (eligibleRegistrations.size() < activeRegistrations.size()) {
            throw ApiException.badRequest("Còn sinh viên đang theo học lớp " + lop.getMa()
                    + " chưa được nhập đủ điểm (" + eligibleRegistrations.size() + "/"
                    + activeRegistrations.size() + "), không thể duyệt hoặc công bố bảng điểm");
        }
    }

    private void notifyGradesPublishedSafely(LopHocPhan lop, List<DangKyHocPhan> eligibleRegistrations,
                                             int version, LocalDateTime publishedAt) {
        if (thongBaoService == null) {
            return;
        }
        try {
            thongBaoService.gradesPublished(lop, eligibleRegistrations, version, publishedAt);
        } catch (RuntimeException ex) {
            log.error("Không thể lập lịch thông báo công bố điểm cho lớp id={} (ma={}); sẽ đồng bộ bù khi sinh viên mở trung tâm thông báo: {}",
                    lop.getId(), lop.getMa(), ex.getClass().getSimpleName());
        }
    }

    private <T> T executeAudited(String action, String entityType, Object entityId,
                                 Function<AuditLogService.AuditContext, T> operation) {
        if (auditLogService == null) {
            return operation.apply(new AuditLogService.AuditContext(entityId == null ? null : String.valueOf(entityId)));
        }
        return auditLogService.execute(action, entityType, entityId, operation);
    }

    private Map<String, Object> gradeSheetAuditSnapshot(LopHocPhan lop, TrangThaiBangDiem trangThaiBangDiem,
                                                        int soSinhVienDangHoc, int soSinhVienDuDieuKien) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("lopHocPhanId", lop.getId());
        snapshot.put("maLop", lop.getMa());
        if (lop.getMonHoc() != null) {
            snapshot.put("monHocId", lop.getMonHoc().getId());
            snapshot.put("maMon", lop.getMonHoc().getMa());
            snapshot.put("tenMon", lop.getMonHoc().getTen());
        }
        if (lop.getHocKy() != null) {
            snapshot.put("hocKyId", lop.getHocKy().getId());
            snapshot.put("tenHocKy", lop.getHocKy().getTen());
            snapshot.put("namHoc", lop.getHocKy().getNamHoc());
        }
        snapshot.put("trangThaiBangDiem", trangThaiBangDiem.name());
        snapshot.put("phienBanCongBo", Math.max(1, lop.getPhienBanCongBo()));
        snapshot.put("soSinhVienDangHoc", soSinhVienDangHoc);
        snapshot.put("soSinhVienDuDieuKien", soSinhVienDuDieuKien);
        return snapshot;
    }

    private RegistrationDtos.TrangThaiBangDiemDto toTrangThaiBangDiemDto(LopHocPhan lop,
                                                                         int soSinhVienDangHoc,
                                                                         int soSinhVienDuDieuKien) {
        MonHoc mon = lop.getMonHoc();
        HocKy ky = lop.getHocKy();
        return new RegistrationDtos.TrangThaiBangDiemDto(
                lop.getId(),
                lop.getMa(),
                mon != null ? mon.getMa() : null,
                mon != null ? mon.getTen() : null,
                ky != null ? ky.getId() : null,
                ky != null ? ky.getTen() : null,
                ky != null ? ky.getNamHoc() : null,
                lop.getTrangThaiBangDiemHieuLuc().name(),
                Math.max(1, lop.getPhienBanCongBo()),
                soSinhVienDangHoc,
                soSinhVienDuDieuKien,
                lop.getGuiDuyetBangDiemLuc(),
                lop.getDuyetBangDiemLuc(),
                lop.getCongBoBangDiemLuc());
    }

    private String chuanHoaTrangThaiKetQua(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return null;
        }
        String normalized = trangThai.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }
        if (!TRANG_THAI_KET_QUA_HOP_LE.contains(normalized)) {
            throw ApiException.badRequest("Trạng thái kết quả không hợp lệ. Chỉ chấp nhận: DAT, CHUA_DAT, CHUA_CO_DIEM");
        }
        return normalized;
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
            if (item.diemTongKet() >= AcademicRules.DIEM_DAT) {
                tinChiTichLuy += item.soTinChi();
            }
        }

        double gpa10 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem10 / tinChiCoDiem) * 100.0) / 100.0;
        double gpa4 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
        return new TongHop(tongTinChi, tinChiCoDiem, gpa10, gpa4, tinChiTichLuy);
    }

    private TongHop tinhTongHopLichSu(List<RegistrationDtos.MucLichSuHocTapDto> items) {
        int tongTinChi = 0;
        int tinChiCoDiem = 0;
        int tinChiTichLuy = 0;
        double tongDiem10 = 0;
        double tongDiem4 = 0;

        for (RegistrationDtos.MucLichSuHocTapDto item : items) {
            tongTinChi += item.soTinChi();
            if (item.diemTongKet() == null) {
                continue;
            }
            tinChiCoDiem += item.soTinChi();
            tongDiem10 += item.diemTongKet() * item.soTinChi();
            tongDiem4 += quyDoi4(item.diemTongKet()) * item.soTinChi();
            if (item.diemTongKet() >= AcademicRules.DIEM_DAT) {
                tinChiTichLuy += item.soTinChi();
            }
        }

        double gpa10 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem10 / tinChiCoDiem) * 100.0) / 100.0;
        double gpa4 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
        return new TongHop(tongTinChi, tinChiCoDiem, gpa10, gpa4, tinChiTichLuy);
    }

    public double quyDoiHe4(double diem10) {
        return quyDoi4(diem10);
    }

    private double quyDoi4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= AcademicRules.DIEM_DAT) return 1.5;
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
