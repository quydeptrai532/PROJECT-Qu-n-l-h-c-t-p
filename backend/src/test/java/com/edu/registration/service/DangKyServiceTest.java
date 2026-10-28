package com.edu.registration.service;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.MonTienQuyet;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonTienQuyetRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DangKyServiceTest {

    @Mock
    private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Mock
    private LopHocPhanRepository lopHocPhanRepository;
    @Mock
    private DiemRepository diemRepository;
    @Mock
    private MonTienQuyetRepository monTienQuyetRepository;
    @Mock
    private SinhVienRepository sinhVienRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private HocKyService hocKyService;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private DangKyService dangKyService;

    private User user;
    private SinhVien sinhVien;
    private HocKy hocKy;
    private MonHoc monHoc;
    private LopHocPhan lopHocPhan;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("sv01");

        sinhVien = new SinhVien();
        sinhVien.setId(1L);
        sinhVien.setUser(user);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");

        hocKy = new HocKy();
        hocKy.setId(1L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2026-2027");
        hocKy.setNgayBatDau(LocalDate.now().minusDays(14));
        hocKy.setNgayKetThuc(LocalDate.now().plusDays(90));
        hocKy.setDangKyTu(LocalDate.now().minusDays(5));
        hocKy.setDangKyDen(LocalDate.now().plusDays(20));
        hocKy.setSoTinChiToiThieu(12);
        hocKy.setSoTinChiToiDa(24);

        monHoc = new MonHoc();
        monHoc.setId(10L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu và giải thuật");
        monHoc.setSoTinChi(3);

        lopHocPhan = new LopHocPhan();
        lopHocPhan.setId(100L);
        lopHocPhan.setMa("INT201.01");
        lopHocPhan.setMonHoc(monHoc);
        lopHocPhan.setHocKy(hocKy);
        lopHocPhan.setGiangVien(new GiangVien());
        lopHocPhan.setPhong("A101");
        lopHocPhan.setThu(2);
        lopHocPhan.setTietBatDau(1);
        lopHocPhan.setSoTiet(3);
        lopHocPhan.setSiSoToiDa(40);
        lopHocPhan.setSiSoHienTai(0);
    }

    private void dangNhapVoiSinhVien() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(sinhVien));
    }

    private void stubLopHocPhan() {
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
    }

    private LopHocPhan taoLopKhac(long id, String ma, MonHoc mon, int thu, int tietBatDau, int soTiet) {
        LopHocPhan lop = new LopHocPhan();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hocKy);
        lop.setGiangVien(new GiangVien());
        lop.setPhong("B202");
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(40);
        return lop;
    }

    @Test
    void dangKyThanhCongTangSiSoVaTaoDangKyMoi() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L)).thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(DangKyHocPhan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dangKyService.dangKy(100L);

        ArgumentCaptor<DangKyHocPhan> captor = ArgumentCaptor.forClass(DangKyHocPhan.class);
        verify(dangKyHocPhanRepository).save(captor.capture());
        assertEquals(TrangThaiDangKy.DA_DANG_KY, captor.getValue().getTrangThai());
        assertEquals(sinhVien, captor.getValue().getSinhVien());
        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void dangKyLaiKichHoatLaiDangKyDaHuy() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        DangKyHocPhan daHuy = new DangKyHocPhan();
        daHuy.setId(200L);
        daHuy.setSinhVien(sinhVien);
        daHuy.setLopHocPhan(lopHocPhan);
        daHuy.setTrangThai(TrangThaiDangKy.DA_HUY);

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L))
                .thenReturn(Optional.of(daHuy));
        when(dangKyHocPhanRepository.save(any(DangKyHocPhan.class))).thenAnswer(inv -> inv.getArgument(0));

        dangKyService.dangKy(100L);

        assertEquals(TrangThaiDangKy.DA_DANG_KY, daHuy.getTrangThai());
        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void dangKyThatBaiKhiNgoaiThoiGianDangKy() {
        hocKy.setDangKyTu(LocalDate.now().minusDays(10));
        hocKy.setDangKyDen(LocalDate.now().minusDays(1));
        dangNhapVoiSinhVien();
        stubLopHocPhan();

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("thời gian đăng ký"));
        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void dangKyThatBaiKhiKhongTimThayLopHocPhan() {
        dangNhapVoiSinhVien();

        when(lopHocPhanRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.dangKy(999L));

        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void dangKyThatBaiKhiSinhVienChuaCoHoSo() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.dangKy(100L));

        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void huyDangKyThatBaiKhiDangKyKhongThuocSinhVien() {
        dangNhapVoiSinhVien();

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.huyDangKy(200L));

        verify(lopHocPhanRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void huyDangKyThatBaiKhiDangKyDaHuy() {
        dangNhapVoiSinhVien();

        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(TrangThaiDangKy.DA_HUY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L))
                .thenReturn(Optional.of(dangKy));

        assertThrows(ApiException.class,
                () -> dangKyService.huyDangKy(200L));

        assertEquals(TrangThaiDangKy.DA_HUY, dangKy.getTrangThai());
        verify(lopHocPhanRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void dangKyThatBaiKhiDaDangKyMonTrongHocKy() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        DangKyHocPhan daDangKy = new DangKyHocPhan();
        daDangKy.setLopHocPhan(lopHocPhan);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("đã đăng ký môn"));
    }

    @Test
    void dangKyThatBaiKhiTrungLichHoc() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        MonHoc monKhac = new MonHoc();
        monKhac.setId(11L);
        monKhac.setMa("INT303");
        monKhac.setTen("Mạng máy tính");
        monKhac.setSoTinChi(3);
        LopHocPhan trungLich = taoLopKhac(101L, "INT303.01", monKhac, 2, 2, 3);

        DangKyHocPhan daDangKy = new DangKyHocPhan();
        daDangKy.setLopHocPhan(trungLich);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("Trùng lịch học"));
    }

    @Test
    void dangKyThatBaiKhiLopDay() {
        lopHocPhan.setSiSoHienTai(40);
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("đủ sĩ số"));
    }

    @Test
    void dangKyThatBaiKhiVuotQuaTinChiToiDa() {
        hocKy.setSoTinChiToiDa(5);
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        MonHoc monKhac = new MonHoc();
        monKhac.setId(11L);
        monKhac.setMa("INT303");
        monKhac.setTen("Mạng máy tính");
        monKhac.setSoTinChi(3);
        LopHocPhan lopKhac = taoLopKhac(101L, "INT303.01", monKhac, 3, 1, 3);

        DangKyHocPhan daDangKy = new DangKyHocPhan();
        daDangKy.setLopHocPhan(lopKhac);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("tín chỉ tối đa"));
    }


    @Test
    void dangKyThanhCongKhiTongTinChiBangMucToiDa() {
        hocKy.setSoTinChiToiDa(3);
        dangNhapVoiSinhVien();
        stubLopHocPhan();

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L))
                .thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L))
                .thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(DangKyHocPhan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        dangKyService.dangKy(100L);

        assertEquals(1, lopHocPhan.getSiSoHienTai());
        verify(dangKyHocPhanRepository).save(any(DangKyHocPhan.class));
    }

    @Test
    void dangKyThatBaiKhiChuaDatMonTienQuyet() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        MonHoc tienQuyet = new MonHoc();
        tienQuyet.setId(9L);
        tienQuyet.setMa("INT101");
        tienQuyet.setTen("Lập trình cơ bản");
        MonTienQuyet lienKet = new MonTienQuyet(monHoc, tienQuyet);

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of(lienKet));
        when(diemRepository.findMonDaDat(1L, 5.0)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("INT101"));
        assertTrue(ex.getMessage().contains("tiên quyết"));
    }

    @Test
    void dangKyThanhCongKhiDaDatMonTienQuyet() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        MonHoc tienQuyet = new MonHoc();
        tienQuyet.setId(9L);
        tienQuyet.setMa("INT101");
        tienQuyet.setTen("Lập trình cơ bản");

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L))
                .thenReturn(List.of(new MonTienQuyet(monHoc, tienQuyet)));
        when(diemRepository.findMonDaDat(1L, 5.0)).thenReturn(List.of(9L));
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L)).thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(DangKyHocPhan.class))).thenAnswer(inv -> inv.getArgument(0));

        dangKyService.dangKy(100L);

        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void huyDangKyThanhCongHoanLaiCho() {
        lopHocPhan.setSiSoHienTai(5);
        dangNhapVoiSinhVien();
        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
        when(dangKyHocPhanRepository.tinhTongTinChi(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(3L);

        dangKyService.huyDangKy(200L);

        assertEquals(TrangThaiDangKy.DA_HUY, dangKy.getTrangThai());
        assertEquals(4, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void huyDangKyThatBaiKhiConLaiDuoiTinChiToiThieu() {
        dangNhapVoiSinhVien();
        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
        when(dangKyHocPhanRepository.tinhTongTinChi(1L, 1L, TrangThaiDangKy.DA_DANG_KY)).thenReturn(6L);

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.huyDangKy(200L));

        assertTrue(ex.getMessage().contains("tối thiểu"));
        assertEquals(TrangThaiDangKy.DA_DANG_KY, dangKy.getTrangThai());
    }

    @Test
    void huyDangKyThatBaiKhiHetThoiGian() {
        hocKy.setDangKyDen(LocalDate.now().minusDays(1));
        dangNhapVoiSinhVien();
        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.huyDangKy(200L));

        assertTrue(ex.getMessage().contains("hết thời gian"));
        assertEquals(TrangThaiDangKy.DA_DANG_KY, dangKy.getTrangThai());
    }
}
