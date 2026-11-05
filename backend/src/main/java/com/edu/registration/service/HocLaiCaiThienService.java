package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Eligibility is derived from this authenticated student's persisted attempts on every read and write. */
@Service
@Transactional(readOnly = true)
public class HocLaiCaiThienService {
    private static final double DIEM_DAT_HIEN_TAI = 5.0;

    private final CurrentUser currentUser;
    private final SinhVienRepository sinhVienRepository;
    private final DangKyHocPhanRepository dangKyRepository;
    private final DiemRepository diemRepository;
    private final LopHocPhanRepository lopRepository;
    private final HocKyService hocKyService;

    public HocLaiCaiThienService(CurrentUser currentUser, SinhVienRepository sinhVienRepository,
                                 DangKyHocPhanRepository dangKyRepository, DiemRepository diemRepository,
                                 LopHocPhanRepository lopRepository, HocKyService hocKyService) {
        this.currentUser = currentUser;
        this.sinhVienRepository = sinhVienRepository;
        this.dangKyRepository = dangKyRepository;
        this.diemRepository = diemRepository;
        this.lopRepository = lopRepository;
        this.hocKyService = hocKyService;
    }

    public List<RegistrationDtos.DangKyHocLaiDto> danhSach(Long hocKyId) {
        SinhVien student = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<DangKyHocPhan> attempts = dangKyRepository.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(student.getId());
        Map<Long, Diem> grades = gradeMap(attempts);
        Map<Long, List<DangKyHocPhan>> bySubject = attempts.stream()
                .filter(attempt -> attempt.getLopHocPhan().isBangDiemDaCongBo()
                        && grades.get(attempt.getId()) != null
                        && grades.get(attempt.getId()).getDiemTongKet() != null)
                .collect(Collectors.groupingBy(attempt -> attempt.getLopHocPhan().getMonHoc().getId()));

        return lopRepository.findByHocKyIdOrderByMaAsc(hocKy.getId()).stream()
                .filter(section -> bySubject.containsKey(section.getMonHoc().getId()))
                .map(section -> toDto(section, bySubject.get(section.getMonHoc().getId()), grades))
                .toList();
    }

    private RegistrationDtos.DangKyHocLaiDto toDto(LopHocPhan section, List<DangKyHocPhan> attempts,
                                                    Map<Long, Diem> grades) {
        DangKyHocPhan latestAttempt = attempts.get(0);
        double latest = grades.get(latestAttempt.getId()).getDiemTongKet();
        String type = latest < DIEM_DAT_HIEN_TAI ? "HOC_LAI" : "HOC_CAI_THIEN";
        List<RegistrationDtos.LanHocTruocDto> previous = attempts.stream()
                .sorted(Comparator.comparing((DangKyHocPhan a) -> a.getLopHocPhan().getHocKy().getNgayBatDau())
                        .thenComparing(DangKyHocPhan::getId))
                .map(attempt -> new RegistrationDtos.LanHocTruocDto(attempt.getId(),
                        attempt.getLopHocPhan().getMa(), attempt.getLopHocPhan().getHocKy().getTen(),
                        attempt.getThoiGianDangKy(), grades.get(attempt.getId()).getDiemTongKet()))
                .toList();
        return new RegistrationDtos.DangKyHocLaiDto(section.getId(), section.getMa(),
                section.getMonHoc().getId(), section.getMonHoc().getMa(), section.getMonHoc().getTen(),
                section.getMonHoc().getSoTinChi(), section.getGiangVien().getHoTen(), section.getPhong(),
                section.getThu(), section.getTietBatDau(), section.getSoTiet(), section.getSiSoHienTai(),
                section.getSiSoToiDa(), section.getHocKy().dangMoDangKy(), type, previous);
    }

    private Map<Long, Diem> gradeMap(List<DangKyHocPhan> attempts) {
        if (attempts.isEmpty()) return Map.of();
        return diemRepository.findByDangKyIdIn(attempts.stream().map(DangKyHocPhan::getId).toList()).stream()
                .collect(Collectors.toMap(grade -> grade.getDangKy().getId(), Function.identity()));
    }
}
