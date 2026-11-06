package com.edu.registration.service;

import com.edu.registration.config.AttendanceProperties;
import com.edu.registration.dto.AttendanceAlertDtos;
import com.edu.registration.entity.BuoiHoc;
import com.edu.registration.entity.CanhBaoDiemDanh;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.DiemDanh;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.LoaiNguongCanhBao;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.ThongBaoNghi;
import com.edu.registration.entity.TrangThaiBangDiem;
import com.edu.registration.entity.TrangThaiCanhBao;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.TrangThaiDiemDanh;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.BuoiHocRepository;
import com.edu.registration.repository.CanhBaoDiemDanhRepository;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemDanhRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.ThongBaoNghiRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dịch vụ tính toán, cập nhật và quản lý cảnh báo chuyên cần quá ngưỡng (QLSV-71 / TASK-LC-06).
 */
@Service
@Transactional
public class AttendanceAlertService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAlertService.class);

    private final LopHocPhanRepository lopHocPhanRepository;
    private final BuoiHocRepository buoiHocRepository;
    private final DiemDanhRepository diemDanhRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final ThongBaoNghiRepository thongBaoNghiRepository;
    private final CanhBaoDiemDanhRepository canhBaoDiemDanhRepository;
    private final GiangVienRepository giangVienRepository;
    private final SinhVienRepository sinhVienRepository;
    private final CurrentUser currentUser;
    private final AttendanceProperties attendanceProperties;
    private final ThongBaoService thongBaoService;
    private final AuditLogService auditLogService;

    @Autowired
    public AttendanceAlertService(LopHocPhanRepository lopHocPhanRepository,
                                  BuoiHocRepository buoiHocRepository,
                                  DiemDanhRepository diemDanhRepository,
                                  DangKyHocPhanRepository dangKyHocPhanRepository,
                                  ThongBaoNghiRepository thongBaoNghiRepository,
                                  CanhBaoDiemDanhRepository canhBaoDiemDanhRepository,
                                  GiangVienRepository giangVienRepository,
                                  SinhVienRepository sinhVienRepository,
                                  CurrentUser currentUser,
                                  AttendanceProperties attendanceProperties,
                                  ThongBaoService thongBaoService,
                                  AuditLogService auditLogService) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.buoiHocRepository = buoiHocRepository;
        this.diemDanhRepository = diemDanhRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.canhBaoDiemDanhRepository = canhBaoDiemDanhRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.currentUser = currentUser;
        this.attendanceProperties = attendanceProperties;
        this.thongBaoService = thongBaoService;
        this.auditLogService = auditLogService;
    }

    /**
     * Tính toán và đồng bộ lại cảnh báo vắng cho tất cả sinh viên trong lớp học phần.
     * Tính toán trực tiếp từ dữ liệu điểm danh và buổi học chuẩn, không dùng bộ đếm riêng rẽ.
     * Loại trừ các buổi chưa diễn ra hoặc buổi bị hủy theo ThongBaoNghi.
     */
    public int kiemTraVaCapNhatCanhBaoChoLop(Long lopHocPhanId) {
        LopHocPhan lop = lopHocPhanRepository.findById(lopHocPhanId).orElse(null);
        if (lop == null) {
            return 0;
        }

        // 1. Xác định các buổi học hợp lệ (đã hoặc đang diễn ra và không bị hủy)
        LocalDate today = LocalDate.now();
        List<BuoiHoc> allSessions = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        List<ThongBaoNghi> cancellations = thongBaoNghiRepository.findByLopHocPhanId(lopHocPhanId);
        Set<LocalDate> cancelledDates = cancellations.stream()
                .map(ThongBaoNghi::getNgayNghi)
                .collect(Collectors.toSet());

        List<BuoiHoc> validSessions = allSessions.stream()
                .filter(b -> !b.getNgayHoc().isAfter(today) && !cancelledDates.contains(b.getNgayHoc()))
                .toList();
        Set<Long> validSessionIds = validSessions.stream()
                .map(BuoiHoc::getId)
                .collect(Collectors.toSet());
        int tongSoBuoiHopLe = validSessions.size();

        // 2. Xác định cấu hình ngưỡng hiệu lực
        Integer effectiveNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double effectiveNguongTyLe = lop.getNguongTyLeVang();
        boolean hasClassConfig = effectiveNguongSoBuoi != null || effectiveNguongTyLe != null;

        if (!hasClassConfig && attendanceProperties.isCanhBaoKichHoat()) {
            effectiveNguongSoBuoi = attendanceProperties.getNguongSoBuoiVang();
            effectiveNguongTyLe = attendanceProperties.getNguongTyLeVang();
        }

        boolean alertEnabled = (effectiveNguongSoBuoi != null || effectiveNguongTyLe != null);

        // 3. Lấy danh sách sinh viên đăng ký hiệu lực
        List<DangKyHocPhan> dangKys = dangKyHocPhanRepository.findByLopHocPhan(lop.getId(), TrangThaiDangKy.DA_DANG_KY);
        List<DiemDanh> allDiemDanhs = diemDanhRepository.findByBuoiHocLopHocPhanId(lop.getId());
        Map<Long, List<DiemDanh>> studentRecordsMap = allDiemDanhs.stream()
                .filter(dd -> validSessionIds.contains(dd.getBuoiHoc().getId()))
                .collect(Collectors.groupingBy(dd -> dd.getSinhVien().getId()));

        int evaluatedCount = 0;

        for (DangKyHocPhan dk : dangKys) {
            SinhVien sv = dk.getSinhVien();
            if (sv == null || sv.getUser() == null || !sv.getUser().isTrangThai()) {
                continue;
            }

            List<DiemDanh> records = studentRecordsMap.getOrDefault(sv.getId(), Collections.emptyList());

            int vang = 0;
            int muon = 0;
            int coPhep = 0;
            for (DiemDanh r : records) {
                if (r.getTrangThai() == null) continue;
                switch (r.getTrangThai()) {
                    case VANG -> vang++;
                    case MUON -> muon++;
                    case CO_PHEP -> coPhep++;
                    default -> {}
                }
            }

            int quyDoiVangTuMuon = attendanceProperties.getSoLanMuonThanhMotVang() > 0
                    ? (muon / attendanceProperties.getSoLanMuonThanhMotVang()) : 0;
            int soBuoiVang = vang + quyDoiVangTuMuon + (attendanceProperties.isCoPhepTinhVang() ? coPhep : 0);

            double tyLeVang = tongSoBuoiHopLe > 0
                    ? (Math.round(((double) soBuoiVang / tongSoBuoiHopLe) * 1000.0) / 10.0)
                    : 0.0;

            // Đánh giá ngưỡng số buổi vắng
            if (effectiveNguongSoBuoi != null) {
                boolean breach = soBuoiVang >= effectiveNguongSoBuoi;
                evaluateAlertRow(lop, sv, LoaiNguongCanhBao.SO_BUOI_VANG, (double) effectiveNguongSoBuoi,
                        soBuoiVang, tongSoBuoiHopLe, tyLeVang, breach);
                evaluatedCount++;
            } else {
                // Nếu ngưỡng số buổi không còn áp dụng, giải tỏa cảnh báo cũ nếu có
                resolveOldAlertIfPresent(lop, sv, LoaiNguongCanhBao.SO_BUOI_VANG, soBuoiVang, tongSoBuoiHopLe, tyLeVang);
            }

            // Đánh giá ngưỡng tỷ lệ vắng
            if (effectiveNguongTyLe != null) {
                boolean breach = tongSoBuoiHopLe > 0 && tyLeVang >= effectiveNguongTyLe;
                evaluateAlertRow(lop, sv, LoaiNguongCanhBao.TY_LE_VANG, effectiveNguongTyLe,
                        soBuoiVang, tongSoBuoiHopLe, tyLeVang, breach);
                evaluatedCount++;
            } else {
                // Nếu ngưỡng tỷ lệ không còn áp dụng, giải tỏa cảnh báo cũ nếu có
                resolveOldAlertIfPresent(lop, sv, LoaiNguongCanhBao.TY_LE_VANG, soBuoiVang, tongSoBuoiHopLe, tyLeVang);
            }
        }

        return evaluatedCount;
    }

    /**
     * Lấy báo cáo và danh sách cảnh báo của lớp học phần.
     * Giảng viên chỉ được xem của lớp mình phụ trách (kiểm tra quyền tại backend); Quản trị viên xem tất cả.
     */
    public AttendanceAlertDtos.BaoCaoCanhBaoLopDto layBaoCaoCanhBaoLop(Long lopHocPhanId) {
        User actor = currentUser.get();
        LopHocPhan lop = kiemTraQuyenGiangVienVoiLop(lopHocPhanId, actor);

        // Đồng bộ lại cảnh báo mới nhất
        kiemTraVaCapNhatCanhBaoChoLop(lopHocPhanId);

        LocalDate today = LocalDate.now();
        List<BuoiHoc> allSessions = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        List<ThongBaoNghi> cancellations = thongBaoNghiRepository.findByLopHocPhanId(lopHocPhanId);
        Set<LocalDate> cancelledDates = cancellations.stream()
                .map(ThongBaoNghi::getNgayNghi)
                .collect(Collectors.toSet());
        int tongSoBuoiHopLe = (int) allSessions.stream()
                .filter(b -> !b.getNgayHoc().isAfter(today) && !cancelledDates.contains(b.getNgayHoc()))
                .count();

        Integer effectiveNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double effectiveNguongTyLe = lop.getNguongTyLeVang();
        String nguonNguong = "CHUA_CAU_HINH";
        boolean alertEnabled = false;

        if (effectiveNguongSoBuoi != null || effectiveNguongTyLe != null) {
            nguonNguong = "LOP_HOC_PHAN";
            alertEnabled = true;
        } else if (attendanceProperties.isCanhBaoKichHoat() &&
                (attendanceProperties.getNguongSoBuoiVang() != null || attendanceProperties.getNguongTyLeVang() != null)) {
            effectiveNguongSoBuoi = attendanceProperties.getNguongSoBuoiVang();
            effectiveNguongTyLe = attendanceProperties.getNguongTyLeVang();
            nguonNguong = "HE_THONG";
            alertEnabled = true;
        }

        List<CanhBaoDiemDanh> alerts = canhBaoDiemDanhRepository
                .findByLopHocPhanIdOrderByThoiGianCapNhatDescThoiGianTaoDesc(lopHocPhanId);

        List<AttendanceAlertDtos.CanhBaoDiemDanhDto> alertDtos = alerts.stream()
                .map(this::mapToDto)
                .toList();

        long activeCount = alerts.stream()
                .filter(a -> a.getTrangThai() == TrangThaiCanhBao.DANG_CANH_BAO)
                .count();

        return new AttendanceAlertDtos.BaoCaoCanhBaoLopDto(
                lop.getId(),
                lop.getMa(),
                lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                effectiveNguongSoBuoi,
                effectiveNguongTyLe,
                nguonNguong,
                alertEnabled,
                attendanceProperties.getGhiChuNguong(),
                tongSoBuoiHopLe,
                (int) activeCount,
                alertDtos
        );
    }

    /**
     * Giảng viên hoặc Quản trị thiết lập cấu hình ngưỡng cảnh báo cho lớp học phần.
     */
    public AttendanceAlertDtos.BaoCaoCanhBaoLopDto capNhatCauHinhNguongLop(
            Long lopHocPhanId, AttendanceAlertDtos.CauHinhNguongLopRequest request) {
        User actor = currentUser.get();
        LopHocPhan lop = kiemTraQuyenGiangVienVoiLop(lopHocPhanId, actor);

        Integer oldNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double oldNguongTyLe = lop.getNguongTyLeVang();

        lop.setNguongSoBuoiVang(request.nguongSoBuoiVang());
        lop.setNguongTyLeVang(request.nguongTyLeVang());
        lopHocPhanRepository.save(lop);

        auditLogService.execute("ATTENDANCE_THRESHOLD_CONFIG_UPDATED", "LOP_HOC_PHAN", lop.getId(), ctx -> {
            ctx.changes(
                    Map.of(
                            "lopHocPhanId", lop.getId(),
                            "maLop", lop.getMa(),
                            "nguongSoBuoiVang", oldNguongSoBuoi != null ? oldNguongSoBuoi : 0,
                            "nguongTyLeVang", oldNguongTyLe != null ? oldNguongTyLe : 0.0
                    ),
                    Map.of(
                            "lopHocPhanId", lop.getId(),
                            "maLop", lop.getMa(),
                            "nguongSoBuoiVang", request.nguongSoBuoiVang() != null ? request.nguongSoBuoiVang() : 0,
                            "nguongTyLeVang", request.nguongTyLeVang() != null ? request.nguongTyLeVang() : 0.0
                    )
            );
            return null;
        });

        kiemTraVaCapNhatCanhBaoChoLop(lopHocPhanId);
        return layBaoCaoCanhBaoLop(lopHocPhanId);
    }

    /**
     * Danh sách cảnh báo chuyên cần thuộc các lớp do giảng viên phụ trách (hoặc tất cả cho Quản trị).
     */
    @Transactional(readOnly = true)
    public List<AttendanceAlertDtos.CanhBaoDiemDanhDto> danhSachCanhBaoCuaGiangVien(TrangThaiCanhBao trangThai) {
        User actor = currentUser.get();
        if (actor.getRole() == Role.QUAN_TRI) {
            List<CanhBaoDiemDanh> list = (trangThai == null)
                    ? canhBaoDiemDanhRepository.findAll()
                    : canhBaoDiemDanhRepository.findAll().stream().filter(a -> a.getTrangThai() == trangThai).toList();
            return list.stream().map(this::mapToDto).toList();
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            GiangVien gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            return canhBaoDiemDanhRepository.findByGiangVienIdAndTrangThai(gv.getId(), trangThai)
                    .stream().map(this::mapToDto).toList();
        }

        throw ApiException.forbidden("Bạn không có quyền thực hiện thao tác này");
    }

    /**
     * Danh sách cảnh báo chuyên cần của sinh viên đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<AttendanceAlertDtos.CanhBaoDiemDanhDto> danhSachCanhBaoCuaSinhVien(TrangThaiCanhBao trangThai) {
        User actor = currentUser.get();
        SinhVien sv = sinhVienRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        return canhBaoDiemDanhRepository.findBySinhVienIdAndTrangThai(sv.getId(), trangThai)
                .stream().map(this::mapToDto).toList();
    }

    private void evaluateAlertRow(LopHocPhan lop, SinhVien sv,
                                  LoaiNguongCanhBao loaiNguong, double giaTriNguong,
                                  int soBuoiVang, int tongBuoiHopLe, double tyLeVang,
                                  boolean breach) {
        Optional<CanhBaoDiemDanh> existingOpt = canhBaoDiemDanhRepository
                .findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(sv.getId(), lop.getId(), loaiNguong);

        String subjectName = lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "";
        String thresholdDesc = loaiNguong == LoaiNguongCanhBao.SO_BUOI_VANG
                ? ((int) giaTriNguong + " buổi")
                : (giaTriNguong + "%");

        LocalDateTime now = LocalDateTime.now();

        if (breach) {
            String msg = "Sinh viên " + sv.getHoTen() + " (" + sv.getMa() + ")"
                    + " đã vắng " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ ("
                    + String.format(Locale.US, "%.1f", tyLeVang) + "%),"
                    + " đạt/vượt ngưỡng " + thresholdDesc + " của lớp " + lop.getMa() + " (" + subjectName + ").";
            String idempKey = "attendance-alert:" + lop.getId() + ":" + sv.getId() + ":" + loaiNguong.name()
                    + ":" + soBuoiVang + "/" + tongBuoiHopLe + ":ALERT";

            if (existingOpt.isEmpty()) {
                CanhBaoDiemDanh alert = new CanhBaoDiemDanh();
                alert.setLopHocPhan(lop);
                alert.setSinhVien(sv);
                alert.setLoaiNguong(loaiNguong);
                alert.setGiaTriNguong(giaTriNguong);
                alert.setSoBuoiVang(soBuoiVang);
                alert.setTongBuoiHopLe(tongBuoiHopLe);
                alert.setTyLeVang(tyLeVang);
                alert.setTrangThai(TrangThaiCanhBao.DANG_CANH_BAO);
                alert.setThongDiep(msg);
                alert.setIdempotencyKey(idempKey);
                alert.setThoiGianTao(now);
                alert.setThoiGianCapNhat(now);
                CanhBaoDiemDanh saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                auditLogService.execute("ATTENDANCE_ALERT_TRIGGERED", "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                    ctx.changes(Map.of(), Map.of(
                            "canhBaoId", saved.getId(),
                            "lopHocPhanId", lop.getId(),
                            "sinhVienId", sv.getId(),
                            "soBuoiVang", soBuoiVang,
                            "tongBuoiHopLe", tongBuoiHopLe,
                            "tyLeVang", tyLeVang,
                            "loaiNguong", loaiNguong.name(),
                            "giaTriNguong", giaTriNguong,
                            "trangThaiCanhBao", TrangThaiCanhBao.DANG_CANH_BAO.name()
                    ));
                    return null;
                });

                if (thongBaoService != null) {
                    thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                            soBuoiVang, tongBuoiHopLe, tyLeVang, false);
                }
            } else {
                CanhBaoDiemDanh alert = existingOpt.get();
                boolean wasActive = alert.getTrangThai() == TrangThaiCanhBao.DANG_CANH_BAO;
                boolean numbersChanged = alert.getSoBuoiVang() != soBuoiVang || alert.getTongBuoiHopLe() != tongBuoiHopLe
                        || Math.abs(alert.getTyLeVang() - tyLeVang) > 0.01 || Math.abs(alert.getGiaTriNguong() - giaTriNguong) > 0.01;

                if (!wasActive || numbersChanged) {
                    alert.setGiaTriNguong(giaTriNguong);
                    alert.setSoBuoiVang(soBuoiVang);
                    alert.setTongBuoiHopLe(tongBuoiHopLe);
                    alert.setTyLeVang(tyLeVang);
                    alert.setTrangThai(TrangThaiCanhBao.DANG_CANH_BAO);
                    alert.setThongDiep(msg);
                    alert.setIdempotencyKey(idempKey);
                    alert.setThoiGianCapNhat(now);
                    CanhBaoDiemDanh saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                    String action = !wasActive ? "ATTENDANCE_ALERT_TRIGGERED" : "ATTENDANCE_ALERT_UPDATED";
                    auditLogService.execute(action, "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                        ctx.changes(Map.of(), Map.of(
                                "canhBaoId", saved.getId(),
                                "lopHocPhanId", lop.getId(),
                                "sinhVienId", sv.getId(),
                                "soBuoiVang", soBuoiVang,
                                "tongBuoiHopLe", tongBuoiHopLe,
                                "tyLeVang", tyLeVang,
                                "loaiNguong", loaiNguong.name(),
                                "giaTriNguong", giaTriNguong,
                                "trangThaiCanhBao", TrangThaiCanhBao.DANG_CANH_BAO.name()
                        ));
                        return null;
                    });

                    if (thongBaoService != null) {
                        thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                                soBuoiVang, tongBuoiHopLe, tyLeVang, false);
                    }
                }
            }
        } else {
            // Dưới ngưỡng: nếu đang ở DANG_CANH_BAO thì giải tỏa
            if (existingOpt.isPresent()) {
                CanhBaoDiemDanh alert = existingOpt.get();
                if (alert.getTrangThai() == TrangThaiCanhBao.DANG_CANH_BAO) {
                    String msg = "Cảnh báo chuyên cần đã được giải tỏa: Sinh viên " + sv.getHoTen() + " (" + sv.getMa() + ")"
                            + " hiện có số buổi vắng là " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ ("
                            + String.format(Locale.US, "%.1f", tyLeVang) + "%),"
                            + " đã trở về dưới ngưỡng quy định của lớp " + lop.getMa() + " (" + subjectName + ").";
                    String idempKey = "attendance-alert:" + lop.getId() + ":" + sv.getId() + ":" + loaiNguong.name()
                            + ":" + soBuoiVang + "/" + tongBuoiHopLe + ":RESOLVED";

                    alert.setSoBuoiVang(soBuoiVang);
                    alert.setTongBuoiHopLe(tongBuoiHopLe);
                    alert.setTyLeVang(tyLeVang);
                    alert.setTrangThai(TrangThaiCanhBao.DA_GIAI_TOA);
                    alert.setThongDiep(msg);
                    alert.setIdempotencyKey(idempKey);
                    alert.setThoiGianCapNhat(now);
                    CanhBaoDiemDanh saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                    auditLogService.execute("ATTENDANCE_ALERT_RESOLVED", "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                        ctx.changes(Map.of("trangThaiCanhBao", TrangThaiCanhBao.DANG_CANH_BAO.name()), Map.of(
                                "canhBaoId", saved.getId(),
                                "lopHocPhanId", lop.getId(),
                                "sinhVienId", sv.getId(),
                                "soBuoiVang", soBuoiVang,
                                "tongBuoiHopLe", tongBuoiHopLe,
                                "tyLeVang", tyLeVang,
                                "loaiNguong", loaiNguong.name(),
                                "giaTriNguong", giaTriNguong,
                                "trangThaiCanhBao", TrangThaiCanhBao.DA_GIAI_TOA.name()
                        ));
                        return null;
                    });

                    if (thongBaoService != null) {
                        thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                                soBuoiVang, tongBuoiHopLe, tyLeVang, true);
                    }
                }
            }
        }
    }

    private void resolveOldAlertIfPresent(LopHocPhan lop, SinhVien sv, LoaiNguongCanhBao loaiNguong,
                                         int soBuoiVang, int tongBuoiHopLe, double tyLeVang) {
        canhBaoDiemDanhRepository.findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(sv.getId(), lop.getId(), loaiNguong)
                .ifPresent(alert -> {
                    if (alert.getTrangThai() == TrangThaiCanhBao.DANG_CANH_BAO) {
                        alert.setTrangThai(TrangThaiCanhBao.DA_GIAI_TOA);
                        alert.setSoBuoiVang(soBuoiVang);
                        alert.setTongBuoiHopLe(tongBuoiHopLe);
                        alert.setTyLeVang(tyLeVang);
                        alert.setThongDiep("Cảnh báo đã được giải tỏa do ngưỡng không còn áp dụng.");
                        alert.setThoiGianCapNhat(LocalDateTime.now());
                        canhBaoDiemDanhRepository.save(alert);
                    }
                });
    }

    public LopHocPhan kiemTraQuyenGiangVienVoiLop(Long lopHocPhanId, User actor) {
        LopHocPhan lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        if (actor.getRole() == Role.QUAN_TRI) {
            return lop;
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            GiangVien gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(gv.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
            return lop;
        }

        throw ApiException.forbidden("Bạn không có quyền thực hiện thao tác này");
    }

    private AttendanceAlertDtos.CanhBaoDiemDanhDto mapToDto(CanhBaoDiemDanh a) {
        LopHocPhan lop = a.getLopHocPhan();
        SinhVien sv = a.getSinhVien();
        return new AttendanceAlertDtos.CanhBaoDiemDanhDto(
                a.getId(),
                lop != null ? lop.getId() : null,
                lop != null ? lop.getMa() : "",
                (lop != null && lop.getMonHoc() != null) ? lop.getMonHoc().getTen() : "",
                sv != null ? sv.getId() : null,
                sv != null ? sv.getMa() : "",
                sv != null ? sv.getHoTen() : "",
                (sv != null && sv.getLopHanhChinh() != null) ? sv.getLopHanhChinh().getTen() : "",
                a.getLoaiNguong(),
                a.getGiaTriNguong(),
                a.getSoBuoiVang(),
                a.getTongBuoiHopLe(),
                a.getTyLeVang(),
                a.getTrangThai(),
                a.getThongDiep(),
                a.getThoiGianTao(),
                a.getThoiGianCapNhat()
        );
    }
}
