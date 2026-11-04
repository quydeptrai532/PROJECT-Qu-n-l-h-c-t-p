package com.edu.registration.service;

import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAcademicProfileService {

    private static final String NOT_ASSIGNED = "Chưa cập nhật";

    private final CurrentUser currentUser;
    private final SinhVienRepository sinhVienRepository;

    public StudentAcademicProfileService(CurrentUser currentUser, SinhVienRepository sinhVienRepository) {
        this.currentUser = currentUser;
        this.sinhVienRepository = sinhVienRepository;
    }

    @Transactional(readOnly = true)
    public StudentAcademicProfileDto getMyProfile() {
        SinhVien student = sinhVienRepository.findWithAcademicProfileByUserId(currentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        return new StudentAcademicProfileDto(
                student.getKhoa() != null ? student.getKhoa().getMa() : NOT_ASSIGNED,
                student.getKhoa() != null ? student.getKhoa().getTen() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getMa() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getMa() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getPhienBan() : NOT_ASSIGNED,
                NOT_ASSIGNED);
    }
}
