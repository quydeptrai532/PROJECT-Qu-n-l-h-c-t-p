package com.edu.registration.service;

import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.SinhVienRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ThongKeService {

    public static final int SAP_DAY_PERCENT = 90;
    public static final int IT_DANG_KY_PERCENT = 25;
    public static final int SAP_DONG_TRONG_NGAY = 7;

    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final MonHocRepository monHocRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final HocKyService hocKyService;
    private final KhoaRepository khoaRepository;

    public ThongKeService(SinhVienRepository sinhVienRepository, GiangVienRepository giangVienRepository,
                          MonHocRepository monHocRepository, LopHocPhanRepository lopHocPhanRepository,
                          DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                          HocKyService hocKyService, KhoaRepository khoaRepository) {
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.monHocRepository = monHocRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
        this.khoaRepository = khoaRepository;
    }

    public StatDtos.TongQuanDto tongQuan() {
        HocKy hocKy = hocKyService.hocKyHienTai();
        return new StatDtos.TongQuanDto(
                sinhVienRepository.count(),
                giangVienRepository.count(),
                monHocRepository.count(),
                lopHocPhanRepository.count(),
                dangKyHocPhanRepository.countByTrangThai(TrangThaiDangKy.DA_DANG_KY),
                hocKy != null ? hocKy.getTen() + " - " + hocKy.getNamHoc() : "Chưa có học kỳ");
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId) {
        return theoMon(hocKyId, null);
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId, Long khoaId) {
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<LopHocPhan> danhSachLop = khoaId == null
                ? lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId())
                : lopHocPhanRepository.findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(hocKy.getId(), khoaId);

        Map<MonHoc, List<LopHocPhan>> theoMon = danhSachLop.stream()
                .collect(Collectors.groupingBy(LopHocPhan::getMonHoc, LinkedHashMap::new, Collectors.toList()));

        List<StatDtos.ThongKeMonDto> ketQua = new ArrayList<>();
        theoMon.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getMa()))
                .forEach(entry -> {
                    MonHoc monHoc = entry.getKey();
                    List<LopHocPhan> lopCuaMon = entry.getValue();
                    int tongChiTieu = lopCuaMon.stream().mapToInt(LopHocPhan::getSiSoToiDa).sum();
                    int daDangKy = lopCuaMon.stream().mapToInt(LopHocPhan::getSiSoHienTai).sum();
                    double tiLe = tongChiTieu == 0 ? 0
                            : Math.round((daDangKy * 1000.0 / tongChiTieu)) / 10.0;
                    ketQua.add(new StatDtos.ThongKeMonDto(monHoc.getId(), monHoc.getMa(), monHoc.getTen(),
                            lopCuaMon.size(), tongChiTieu, daDangKy, tiLe));
                });
        return ketQua;
    }

    public StatDtos.HanhDongHocVuDto hanhDong(Long hocKyId, Long khoaId) {
        HocKy hocKy = hocKyId == null ? hocKyService.hocKyHienTai() : hocKyService.lay(hocKyId);
        if (hocKy == null) {
            return new StatDtos.HanhDongHocVuDto(null, null, khoaId, null, LocalDateTime.now(), List.of());
        }
        String tenKhoa = khoaId == null ? null : khoaRepository.findById(khoaId)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")).getTen();
        List<LopHocPhan> lopHocPhans = khoaId == null
                ? lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId())
                : lopHocPhanRepository.findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(hocKy.getId(), khoaId);
        Map<Long, Long> siSoThucTe = lopHocPhanRepository.countDangKyHienHanhTheoLop(
                        hocKy.getId(), khoaId, TrangThaiDangKy.DA_DANG_KY).stream()
                .collect(Collectors.toMap(LopHocPhanRepository.DangKyTheoLop::getLopHocPhanId,
                        LopHocPhanRepository.DangKyTheoLop::getSoDangKy));

        List<StatDtos.CanhBaoHocVuDto> canhBao = new ArrayList<>();
        Map<MonHoc, List<LopHocPhan>> theoMon = lopHocPhans.stream().collect(Collectors.groupingBy(
                LopHocPhan::getMonHoc, LinkedHashMap::new, Collectors.toList()));
        for (LopHocPhan lop : lopHocPhans) {
            long daDangKy = siSoThucTe.getOrDefault(lop.getId(), 0L);
            int chiTieu = lop.getSiSoToiDa();
            double tiLe = chiTieu == 0 ? 0 : daDangKy * 100.0 / chiTieu;
            if (tiLe >= SAP_DAY_PERCENT) {
                canhBao.add(canhBao("LOP_GAN_DAY", "Lớp gần đầy",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng từ " + SAP_DAY_PERCENT + "%.",
                        lop, daDangKy, chiTieu, tiLe, hocKy));
            } else if (tiLe <= IT_DANG_KY_PERCENT) {
                canhBao.add(canhBao("LOP_IT_DANG_KY", "Lớp ít đăng ký",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng không quá "
                                + IT_DANG_KY_PERCENT + "%.", lop, daDangKy, chiTieu, tiLe, hocKy));
            }
        }
        for (Map.Entry<MonHoc, List<LopHocPhan>> entry : theoMon.entrySet()) {
            List<LopHocPhan> sections = entry.getValue();
            long daDangKy = sections.stream().mapToLong(lop -> siSoThucTe.getOrDefault(lop.getId(), 0L)).sum();
            long tongChiTieu = sections.stream().mapToLong(LopHocPhan::getSiSoToiDa).sum();
            if (tongChiTieu > 0 && daDangKy >= tongChiTieu) {
                MonHoc mon = entry.getKey();
                double tiLe = daDangKy * 100.0 / tongChiTieu;
                canhBao.add(new StatDtos.CanhBaoHocVuDto("MON_HET_CHO", "Nhu cầu môn đạt chỉ tiêu",
                        daDangKy + "/" + tongChiTieu + " chỗ đã đăng ký trên " + sections.size()
                                + " lớp; đăng ký đang hiệu lực đạt hoặc vượt tổng chỉ tiêu.",
                        "Tổng đăng ký đang hiệu lực của các lớp môn / tổng chỉ tiêu các lớp môn >= 100%.",
                        null, null, mon.getId(), mon.getMa(), mon.getTen(), sections.size(), daDangKy,
                        tongChiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                        hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                                + "&keyword=" + mon.getMa()));
            }
        }
        LocalDate homNay = LocalDate.now();
        long soNgayConLai = java.time.temporal.ChronoUnit.DAYS.between(homNay, hocKy.getDangKyDen());
        if (hocKy.dangMoDangKy() && soNgayConLai >= 0 && soNgayConLai <= SAP_DONG_TRONG_NGAY) {
            canhBao.add(new StatDtos.CanhBaoHocVuDto("DOT_DANG_KY_SAP_DONG", "Đợt đăng ký sắp đóng",
                    "Còn " + soNgayConLai + " ngày; hạn đăng ký " + hocKy.getDangKyDen() + ".",
                    "Đợt đang mở và ngày kết thúc nằm trong " + SAP_DONG_TRONG_NGAY + " ngày tới.",
                    null, null, null, null, null, lopHocPhans.size(), 0, 0, 0,
                    hocKy.getId(), tenHocKy(hocKy), hocKy.getDangKyDen(),
                    "/quan-tri/hoc-ky"));
        }
        return new StatDtos.HanhDongHocVuDto(hocKy.getId(), tenHocKy(hocKy), khoaId, tenKhoa,
                LocalDateTime.now(), canhBao);
    }

    private StatDtos.CanhBaoHocVuDto canhBao(String loai, String tieuDe, String lyDo, LopHocPhan lop,
                                              long daDangKy, long chiTieu, double tiLe, HocKy hocKy) {
        return new StatDtos.CanhBaoHocVuDto(loai, tieuDe, lyDo,
                "Sĩ số đăng ký đang hiệu lực / chỉ tiêu lớp; " + ("LOP_GAN_DAY".equals(loai)
                        ? "ngưỡng >= " + SAP_DAY_PERCENT + "%" : "ngưỡng <= " + IT_DANG_KY_PERCENT + "%"),
                lop.getId(), lop.getMa(), lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                1, daDangKy, chiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                        + "&keyword=" + java.net.URLEncoder.encode(lop.getMa(), StandardCharsets.UTF_8));
    }

    private String tenHocKy(HocKy hocKy) {
        return hocKy.getTen() + " - " + hocKy.getNamHoc();
    }

    public byte[] xuatDanhSachLop(Long lopHocPhanId) {
        LopHocPhan lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        List<DangKyHocPhan> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, TrangThaiDangKy.DA_DANG_KY);

        Map<Long, Diem> diemMap = danhSach.isEmpty() ? Map.of()
                : diemRepository.findByDangKyIdIn(danhSach.stream().map(DangKyHocPhan::getId).toList())
                .stream().collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("STT,Mã sinh viên,Họ tên,Email,Điểm quá trình,Điểm cuối kỳ,Điểm tổng kết\n");
        int stt = 1;
        for (DangKyHocPhan dangKy : danhSach) {
            Diem diem = diemMap.get(dangKy.getId());
            csv.append(stt++).append(',')
                    .append(escape(dangKy.getSinhVien().getMa())).append(',')
                    .append(escape(dangKy.getSinhVien().getHoTen())).append(',')
                    .append(escape(dangKy.getSinhVien().getEmail())).append(',')
                    .append(diem != null && diem.getDiemQuaTrinh() != null ? diem.getDiemQuaTrinh() : "").append(',')
                    .append(diem != null && diem.getDiemCuoiKy() != null ? diem.getDiemCuoiKy() : "").append(',')
                    .append(diem != null && diem.getDiemTongKet() != null ? diem.getDiemTongKet() : "").append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
