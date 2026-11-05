package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private HocKyRepository hocKyRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private DtoMapper mapper;
    @Mock
    private ThongBaoService thongBaoService;

    private DiemService diemService;

    private User user;
    private GiangVien giangVien;
    private SinhVien sinhVien;
    private LopHocPhan lopHocPhan;
    private DangKyHocPhan dangKy;

    @BeforeEach
    void setUp() {
        diemService = new DiemService(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository,
                sinhVienRepository, giangVienRepository, hocKyRepository, currentUser, mapper,
                null, thongBaoService);

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
        hocKy.setNamHoc("2025-2026");
        hocKy.setNgayBatDau(LocalDate.of(2025, 9, 1));

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

        sinhVien = new SinhVien();
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

    private void dangNhapVoiSinhVien() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
    }

    @Test
    void nhapDiemTinhDiemTongKetTheoTrongSoVaGiuTrangThaiNhapKhongPhatThongBao() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));
        when(diemRepository.findByDangKyId(300L)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Diem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0));

        ArgumentCaptor<Diem> captor = ArgumentCaptor.forClass(Diem.class);
        verify(diemRepository).save(captor.capture());
        assertEquals(8.6, captor.getValue().getDiemTongKet());
        assertEquals(TrangThaiBangDiem.NHAP, lopHocPhan.getTrangThaiBangDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());
    }

    @Test
    void khongTheCongBoKhiBangDiemConNhapHoacChoDuyetVaChiThongBaoKhiDaDuyetSangDaCongBo() {
        dangNhapVoiGiangVien();
        lopHocPhan.setTrangThaiBangDiem(TrangThaiBangDiem.NHAP);
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        // 1. Cannot publish while in NHAP
        ApiException draftEx = assertThrows(ApiException.class, () -> diemService.congBoBangDiem(100L));
        assertTrue(draftEx.getMessage().contains("cần được phê duyệt trước khi công bố"));
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 2. Submit for approval -> CHO_DUYET (no notification)
        Diem diem = new Diem();
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(8.0);
        diem.setDiemCuoiKy(9.0);
        diem.setDiemTongKet(8.6);
        when(dangKyHocPhanRepository.findByLopHocPhan(100L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(dangKy));
        when(diemRepository.findByDangKyIdIn(List.of(300L))).thenReturn(List.of(diem));

        RegistrationDtos.TrangThaiBangDiemDto submitted = diemService.guiDuyetBangDiem(100L);
        assertEquals("CHO_DUYET", submitted.trangThaiBangDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 3. Cannot publish while in CHO_DUYET
        ApiException pendingEx = assertThrows(ApiException.class, () -> diemService.congBoBangDiem(100L));
        assertTrue(pendingEx.getMessage().contains("cần được phê duyệt trước khi công bố"));
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 4. Approve -> DA_DUYET (still no notification)
        RegistrationDtos.TrangThaiBangDiemDto approved = diemService.pheDuyetBangDiem(100L);
        assertEquals("DA_DUYET", approved.trangThaiBangDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 5. Publish after approval -> DA_CONG_BO (emits notification with version 1)
        RegistrationDtos.TrangThaiBangDiemDto published = diemService.congBoBangDiem(100L);
        assertEquals("DA_CONG_BO", published.trangThaiBangDiem());
        assertEquals(1, published.phienBanCongBo());
        verify(thongBaoService, times(1)).gradesPublished(eq(lopHocPhan), eq(List.of(dangKy)), eq(1), any());
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

    @Test
    void lichSuHocTapBaoToanNhieuLanHocVaAnDiemChuaCongBo() {
        dangNhapVoiSinhVien();

        HocKy hk1 = new HocKy();
        hk1.setId(1L);
        hk1.setTen("Học kỳ 1");
        hk1.setNamHoc("2025-2026");
        hk1.setNgayBatDau(LocalDate.of(2025, 9, 1));

        HocKy hk2 = new HocKy();
        hk2.setId(2L);
        hk2.setTen("Học kỳ 2");
        hk2.setNamHoc("2025-2026");
        hk2.setNgayBatDau(LocalDate.of(2026, 2, 1));

        HocKy hk3 = new HocKy();
        hk3.setId(3L);
        hk3.setTen("Học kỳ 1");
        hk3.setNamHoc("2026-2027");
        hk3.setNgayBatDau(LocalDate.of(2026, 9, 1));

        MonHoc int101 = new MonHoc();
        int101.setId(11L);
        int101.setMa("INT101");
        int101.setTen("Lập trình cơ bản");
        int101.setSoTinChi(3);

        MonHoc mat101 = new MonHoc();
        mat101.setId(12L);
        mat101.setMa("MAT101");
        mat101.setTen("Toán rời rạc");
        mat101.setSoTinChi(3);

        LopHocPhan lopLan1 = taoLop(201L, "INT101.01-HK1", int101, hk1);
        LopHocPhan lopLan2 = taoLop(202L, "INT101.01-HK2", int101, hk2);
        LopHocPhan lopChuaCongBo = taoLop(203L, "MAT101.01-HK3", mat101, hk3);
        lopChuaCongBo.setTrangThaiBangDiem(TrangThaiBangDiem.CHO_DUYET);

        DangKyHocPhan dkLan1 = taoDangKy(401L, lopLan1, LocalDateTime.of(2025, 8, 20, 8, 0));
        DangKyHocPhan dkLan2 = taoDangKy(402L, lopLan2, LocalDateTime.of(2026, 1, 20, 8, 0));
        DangKyHocPhan dkChuaCongBo = taoDangKy(403L, lopChuaCongBo, LocalDateTime.of(2026, 8, 20, 8, 0));

        Diem diemTruot = new Diem();
        diemTruot.setDangKy(dkLan1);
        diemTruot.setDiemQuaTrinh(4.0);
        diemTruot.setDiemCuoiKy(4.0);
        diemTruot.setDiemTongKet(4.0);

        Diem diemDat = new Diem();
        diemDat.setDangKy(dkLan2);
        diemDat.setDiemQuaTrinh(7.5);
        diemDat.setDiemCuoiKy(8.0);
        diemDat.setDiemTongKet(7.8);

        Diem diemNhapChuaCongBo = new Diem();
        diemNhapChuaCongBo.setDangKy(dkChuaCongBo);
        diemNhapChuaCongBo.setDiemQuaTrinh(9.0);
        diemNhapChuaCongBo.setDiemCuoiKy(8.5);
        diemNhapChuaCongBo.setDiemTongKet(8.7);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(dkChuaCongBo, dkLan2, dkLan1));
        when(diemRepository.findByDangKyIdIn(any()))
                .thenReturn(List.of(diemTruot, diemDat, diemNhapChuaCongBo));

        RegistrationDtos.LichSuHocTapDto ketQua = diemService.lichSuHocTap(null, null);

        assertEquals(3, ketQua.items().size());
        assertTrue(ketQua.coNhieuLanHoc());
        assertEquals(1, ketQua.soMonDat());
        assertEquals(1, ketQua.soMonChuaDat());
        assertEquals(1, ketQua.soMonChuaCoDiem());
        assertEquals(1, ketQua.monChuaDat().size());

        RegistrationDtos.MucLichSuHocTapDto muc1 = ketQua.items().get(0);
        assertEquals(401L, muc1.dangKyId());
        assertEquals(1, muc1.lanHoc());
        assertEquals(2, muc1.tongSoLanHoc());
        assertEquals("CHUA_DAT", muc1.trangThaiKetQua());
        assertEquals("Chưa đạt", muc1.tenTrangThaiKetQua());
        assertEquals(4.0, muc1.diemTongKet());

        RegistrationDtos.MucLichSuHocTapDto muc2 = ketQua.items().get(1);
        assertEquals(402L, muc2.dangKyId());
        assertEquals(2, muc2.lanHoc());
        assertEquals(2, muc2.tongSoLanHoc());
        assertEquals("DAT", muc2.trangThaiKetQua());
        assertEquals("Đạt", muc2.tenTrangThaiKetQua());
        assertEquals(7.8, muc2.diemTongKet());

        RegistrationDtos.MucLichSuHocTapDto muc3 = ketQua.items().get(2);
        assertEquals(403L, muc3.dangKyId());
        assertEquals(1, muc3.lanHoc());
        assertEquals(1, muc3.tongSoLanHoc());
        assertEquals("CHUA_CO_DIEM", muc3.trangThaiKetQua());
        assertEquals("Chưa có điểm", muc3.tenTrangThaiKetQua());
        assertNull(muc3.diemQuaTrinh());
        assertNull(muc3.diemCuoiKy());
        assertNull(muc3.diemTongKet());

        // Filter by HK2 and status DAT still preserves lanHoc = 2 for the retake record
        when(hocKyRepository.existsById(2L)).thenReturn(true);
        RegistrationDtos.LichSuHocTapDto locHk2 = diemService.lichSuHocTap(2L, "DAT");
        assertEquals(1, locHk2.items().size());
        assertEquals(2, locHk2.items().get(0).lanHoc());
        assertEquals(2, locHk2.items().get(0).tongSoLanHoc());
    }

    @Test
    void lichSuHocTapTuChoiThamSoLocKhongHopLe() {
        ApiException invalidStatus = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(null, "KHONG_HOP_LE"));
        assertTrue(invalidStatus.getMessage().contains("Trạng thái kết quả không hợp lệ"));

        ApiException negativeSemester = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(0L, null));
        assertEquals("Học kỳ không hợp lệ", negativeSemester.getMessage());

        when(hocKyRepository.existsById(999L)).thenReturn(false);
        ApiException missingSemester = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(999L, null));
        assertEquals("Học kỳ không tồn tại", missingSemester.getMessage());
    }

    private LopHocPhan taoLop(Long id, String ma, MonHoc mon, HocKy hocKy) {
        LopHocPhan lop = new LopHocPhan();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        return lop;
    }

    private DangKyHocPhan taoDangKy(Long id, LopHocPhan lop, LocalDateTime thoiGian) {
        DangKyHocPhan dk = new DangKyHocPhan();
        dk.setId(id);
        dk.setSinhVien(sinhVien);
        dk.setLopHocPhan(lop);
        dk.setThoiGianDangKy(thoiGian);
        dk.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        return dk;
    }
}
