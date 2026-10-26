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
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.SinhVienRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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

    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final MonHocRepository monHocRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final HocKyService hocKyService;

    public ThongKeService(SinhVienRepository sinhVienRepository, GiangVienRepository giangVienRepository,
                          MonHocRepository monHocRepository, LopHocPhanRepository lopHocPhanRepository,
                          DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                          HocKyService hocKyService) {
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.monHocRepository = monHocRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
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
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<LopHocPhan> danhSachLop = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId());

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
