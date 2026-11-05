package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
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
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.security.CurrentUser;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentTranscriptPdfServiceTest {

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
    private StudentAcademicProfileService studentAcademicProfileService;

    private DiemService diemService;
    private Clock fixedClock;
    private User user;
    private SinhVien sinhVien;
    private GiangVien giangVien;

    @BeforeEach
    void setUp() {
        diemService = new DiemService(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository,
                sinhVienRepository, giangVienRepository, hocKyRepository, currentUser, new DtoMapper());
        fixedClock = Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        user = new User();
        user.setId(10L);
        user.setUsername("sv01");

        sinhVien = new SinhVien();
        sinhVien.setId(1L);
        sinhVien.setUser(user);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");
        sinhVien.setEmail("anh.tm@student.edu.vn");

        giangVien = new GiangVien();
        giangVien.setId(7L);
        giangVien.setMa("GV01");
        giangVien.setHoTen("TS. Nguyễn Văn An");
    }

    private void dangNhapSinhVien() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findWithAcademicProfileByUserId(10L)).thenReturn(Optional.of(sinhVien));
        when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "K2024", "CNPM01", "Kỹ thuật phần mềm 01", "K2024", "2024.1", "Chưa cập nhật"));
    }

    @Test
    void xuatBangDiemPdfChiChuaDiemDaCongBoVaKhopGpaTiengViet() throws Exception {
        dangNhapSinhVien();

        HocKy hk1 = taoHocKy(1L, "Học kỳ 1", "2025-2026", LocalDate.of(2025, 9, 1));
        HocKy hk2 = taoHocKy(2L, "Học kỳ 2", "2025-2026", LocalDate.of(2026, 2, 1));
        HocKy hk3 = taoHocKy(3L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));

        MonHoc int101 = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        MonHoc mat101 = taoMon(12L, "MAT101", "Toán rời rạc", 3);
        MonHoc eco101 = taoMon(13L, "ECO101", "Kinh tế vi mô", 3);

        LopHocPhan lopLan1 = taoLop(201L, "INT101.01-HK1", int101, hk1);
        LopHocPhan lopLan2 = taoLop(202L, "INT101.01-HK2", int101, hk2);
        LopHocPhan lopNhapChuaCongBo = taoLop(203L, "MAT101.01-HK3", mat101, hk3);
        LopHocPhan lopChuaCoDiem = taoLop(204L, "ECO101.01-HK3", eco101, hk3);

        DangKyHocPhan dkLan1 = taoDangKy(401L, lopLan1, LocalDateTime.of(2025, 8, 20, 8, 0));
        DangKyHocPhan dkLan2 = taoDangKy(402L, lopLan2, LocalDateTime.of(2026, 1, 20, 8, 0));
        DangKyHocPhan dkNhap = taoDangKy(403L, lopNhapChuaCongBo, LocalDateTime.of(2026, 8, 20, 8, 0));
        DangKyHocPhan dkChuaDiem = taoDangKy(404L, lopChuaCoDiem, LocalDateTime.of(2026, 8, 21, 8, 0));

        Diem diemTruot = taoDiem(dkLan1, 4.0, 4.0, 4.0);
        Diem diemDat = taoDiem(dkLan2, 7.5, 8.0, 7.8);
        Diem diemNhap = taoDiem(dkNhap, 9.5, null, null);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(dkChuaDiem, dkNhap, dkLan2, dkLan1));
        when(diemRepository.findByDangKyIdIn(any()))
                .thenReturn(List.of(diemTruot, diemDat, diemNhap));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(null);

        assertEquals("bang-diem-SV001.pdf", file.tenTep());
        assertEquals(2, file.soMonDaCongBo());
        assertNotNull(file.noiDung());
        assertTrue(file.noiDung().length > 1000);
        assertEquals("%PDF-", new String(file.noiDung(), 0, 5, StandardCharsets.US_ASCII));

        RegistrationDtos.BangDiemPdfDto duLieu = file.duLieu();
        assertNull(duLieu.donViPhatHanh());
        assertEquals(2, duLieu.hocKy().size());
        assertEquals(6, duLieu.tongTinChiCoDiem());
        assertEquals(3, duLieu.tinChiTichLuy());
        assertEquals(5.9, duLieu.gpa10());
        assertEquals(2.0, duLieu.gpa4());
        assertTrue(duLieu.coNhieuLanHoc());

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            assertEquals(1, reader.getNumberOfPages());
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");

            assertTrue(text.contains("BẢNG ĐIỂM HỌC TẬP CÁ NHÂN"), text);
            assertTrue(text.contains("Trần Minh Anh"), text);
            assertTrue(text.contains("SV001"), text);
            assertTrue(text.contains("Công nghệ thông tin (CNTT)"), text);
            assertTrue(text.contains("Kỹ thuật phần mềm (CNPM)"), text);
            assertTrue(text.contains("Lập trình cơ bản"), text);
            assertTrue(text.contains("INT101.01-HK1 (Lần 1/2)"), text);
            assertTrue(text.contains("INT101.01-HK2 (Lần 2/2)"), text);
            assertTrue(text.contains("Chưa đạt"), text);
            assertTrue(text.contains("Đạt"), text);
            assertTrue(text.contains("5.90"), text);
            assertTrue(text.contains("2.00"), text);
            assertTrue(text.contains("05/11/2026 07:00:00"), text);

            // Điểm nháp/chưa công bố tuyệt đối không xuất hiện trong PDF
            assertFalse(text.contains("MAT101"), text);
            assertFalse(text.contains("Toán rời rạc"), text);
            assertFalse(text.contains("9.5"), text);
            assertFalse(text.contains("ECO101"), text);
            assertFalse(text.contains("Kinh tế vi mô"), text);
            assertFalse(text.contains("Học kỳ 1 - Năm học 2026-2027"), text);
        }
    }

    @Test
    void xuatBangDiemPdfHienThiDonViPhatHanhKhiDaCauHinhVaNgatTrangNhieuMon() throws Exception {
        dangNhapSinhVien();

        HocKy hk1 = taoHocKy(1L, "Học kỳ 1", "2025-2026", LocalDate.of(2025, 9, 1));
        List<DangKyHocPhan> danhSachDangKy = new ArrayList<>();
        List<Diem> danhSachDiem = new ArrayList<>();

        for (int i = 1; i <= 38; i++) {
            MonHoc mon = taoMon((long) i, String.format("INT%03d", i),
                    "Chuyên đề công nghệ phần mềm nâng cao số " + i, 3);
            LopHocPhan lop = taoLop((long) (100 + i), String.format("INT%03d.01", i), mon, hk1);
            DangKyHocPhan dk = taoDangKy((long) (500 + i), lop, LocalDateTime.of(2025, 8, 15, 8, 0).plusMinutes(i));
            Diem diem = taoDiem(dk, 8.0, 8.5, 8.3);
            danhSachDangKy.add(dk);
            danhSachDiem.add(diem);
        }

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(danhSachDangKy);
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(danhSachDiem);

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService,
                "Trường Đại học Công nghệ", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(null);

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            assertTrue(reader.getNumberOfPages() >= 2);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String page1 = extractor.getTextFromPage(1).replaceAll("\\s+", " ");
            String page2 = extractor.getTextFromPage(2).replaceAll("\\s+", " ");

            assertTrue(page1.contains("TRƯỜNG ĐẠI HỌC CÔNG NGHỆ"), page1);
            assertTrue(page1.contains("Chuyên đề công nghệ phần mềm nâng cao số 1"), page1);
            assertTrue(page2.contains("Tên môn học"), page2);
            assertTrue(page2.contains("Chuyên đề công nghệ phần mềm nâng cao số 38"), page2);
        }
    }

    @Test
    void xuatBangDiemPdfBaoLoiRoRangKhiChuaCoKetQuaDaCongBo() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findWithAcademicProfileByUserId(10L)).thenReturn(Optional.of(sinhVien));
        when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "Chưa cập nhật", "CNPM01", "Kỹ thuật phần mềm 01", "Chưa cập nhật", "Chưa cập nhật", "Chưa cập nhật"));

        HocKy hk1 = taoHocKy(1L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));
        MonHoc mon = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        LopHocPhan lop = taoLop(201L, "INT101.01", mon, hk1);
        DangKyHocPhan dk = taoDangKy(401L, lop, LocalDateTime.of(2026, 8, 20, 8, 0));
        Diem diemNhap = taoDiem(dk, 8.0, null, null);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(dk));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(diemNhap));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        ApiException ex = assertThrows(ApiException.class, () -> service.xuatBangDiemPdf(null));
        assertEquals("Chưa có kết quả học tập đã công bố để xuất bảng điểm PDF", ex.getMessage());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void xuatBangDiemPdfTheoHocKyVaChiLayDuLieuCuaSinhVienHienTai() throws Exception {
        User userSv2 = new User();
        userSv2.setId(20L);
        userSv2.setUsername("sv02");

        SinhVien sv2 = new SinhVien();
        sv2.setId(2L);
        sv2.setUser(userSv2);
        sv2.setMa("SV002");
        sv2.setHoTen("Phạm Thu Hà");
        sv2.setEmail("ha.pt@student.edu.vn");

        when(currentUser.get()).thenReturn(userSv2);
        when(sinhVienRepository.findWithAcademicProfileByUserId(20L)).thenReturn(Optional.of(sv2));
        when(sinhVienRepository.findByUser(userSv2)).thenReturn(Optional.of(sv2));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "Chưa cập nhật", "CNPM01", "Kỹ thuật phần mềm 01", "Chưa cập nhật", "Chưa cập nhật", "Chưa cập nhật"));
        when(hocKyRepository.existsById(2L)).thenReturn(true);

        HocKy hk2 = taoHocKy(2L, "Học kỳ 2", "2025-2026", LocalDate.of(2026, 2, 1));
        MonHoc int101 = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        LopHocPhan lop = taoLop(202L, "INT101.01-HK2", int101, hk2);
        DangKyHocPhan dkSv2 = new DangKyHocPhan();
        dkSv2.setId(501L);
        dkSv2.setSinhVien(sv2);
        dkSv2.setLopHocPhan(lop);
        dkSv2.setThoiGianDangKy(LocalDateTime.of(2026, 1, 20, 8, 0));
        dkSv2.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        Diem diemSv2 = taoDiem(dkSv2, 7.0, 8.0, 7.6);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(2L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(dkSv2));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(diemSv2));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(2L);
        assertEquals("bang-diem-SV002-hk2.pdf", file.tenTep());

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            assertTrue(text.contains("Phạm Thu Hà"), text);
            assertTrue(text.contains("SV002"), text);
            assertFalse(text.contains("Trần Minh Anh"), text);
            assertFalse(text.contains("SV001"), text);
        }
    }

    private HocKy taoHocKy(Long id, String ten, String namHoc, LocalDate ngayBatDau) {
        HocKy hocKy = new HocKy();
        hocKy.setId(id);
        hocKy.setTen(ten);
        hocKy.setNamHoc(namHoc);
        hocKy.setNgayBatDau(ngayBatDau);
        return hocKy;
    }

    private MonHoc taoMon(Long id, String ma, String ten, int soTinChi) {
        MonHoc mon = new MonHoc();
        mon.setId(id);
        mon.setMa(ma);
        mon.setTen(ten);
        mon.setSoTinChi(soTinChi);
        return mon;
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

    private Diem taoDiem(DangKyHocPhan dk, Double qt, Double ck, Double tk) {
        Diem diem = new Diem();
        diem.setDangKy(dk);
        diem.setDiemQuaTrinh(qt);
        diem.setDiemCuoiKy(ck);
        diem.setDiemTongKet(tk);
        return diem;
    }
}
