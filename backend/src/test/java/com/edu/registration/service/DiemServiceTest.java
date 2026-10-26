package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiemServiceTest {

    @Mock
    private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Mock
    private DiemRepository diemRepository;
    @Mock
    private LopHocPhanRepository lopHocPhanRepository;
    @Mock
    private SinhVienRepository sinhVienRepository;
    @Mock
    private GiangVienRepository giangVienRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private DiemService diemService;

    private User user;
    private GiangVien giangVien;
    private LopHocPhan lopHocPhan;
    private DangKyHocPhan dangKy;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("gv01");

        giangVien = new GiangVien();
        giangVien.setId(7L);
        giangVien.setUser(user);
        giangVien.setMa("GV01");
        giangVien.setHoTen("TS. Nguyễn Văn An");

        HocKy hocKy = new HocKy();
        hocKy.setId(1L);
        hocKy.setTen("Học kỳ 1");

        MonHoc monHoc = new MonHoc();
        monHoc.setId(10L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu");
        monHoc.setSoTinChi(3);

        lopHocPhan = new LopHocPhan();
        lopHocPhan.setId(100L);
        lopHocPhan.setMa("INT201.01");
        lopHocPhan.setMonHoc(monHoc);
        lopHocPhan.setHocKy(hocKy);
        lopHocPhan.setGiangVien(giangVien);

        SinhVien sinhVien = new SinhVien();
        sinhVien.setId(1L);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");

        dangKy = new DangKyHocPhan();
        dangKy.setId(300L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
    }

    private void dangNhapVoiGiangVien() {
        when(currentUser.get()).thenReturn(user);
        when(giangVienRepository.findByUser(user)).thenReturn(Optional.of(giangVien));
    }

    @Test
    void nhapDiemTinhDiemTongKetTheoTrongSo() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));
        when(diemRepository.findByDangKyId(300L)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Diem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0));

        ArgumentCaptor<Diem> captor = ArgumentCaptor.forClass(Diem.class);
        verify(diemRepository).save(captor.capture());
        assertEquals(8.6, captor.getValue().getDiemTongKet());
    }

    @Test
    void khongTheNhapDiemChoLopKhongPhuTrach() {
        GiangVien nguoiKhac = new GiangVien();
        nguoiKhac.setId(8L);
        nguoiKhac.setHoTen("ThS. Trần Thị Bình");
        lopHocPhan.setGiangVien(nguoiKhac);

        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));

        ApiException ex = assertThrows(ApiException.class, () -> diemService.nhapDiem(
                300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0)));

        assertEquals("Bạn không phụ trách lớp học phần này", ex.getMessage());
        verify(diemRepository, never()).save(any());
    }
}
