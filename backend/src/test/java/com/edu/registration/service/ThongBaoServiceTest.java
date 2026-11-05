package com.edu.registration.service;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LichSuDangKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.ThongBao;
import com.edu.registration.entity.ThongBaoNghi;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.LichSuDangKyRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.ThongBaoRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThongBaoServiceTest {

    @Mock
    private ThongBaoRepository thongBaoRepository;
    @Mock
    private DangKyHocPhanRepository dangKyRepository;
    @Mock
    private LichSuDangKyRepository lichSuDangKyRepository;
    @Mock
    private SinhVienRepository sinhVienRepository;
    @Mock
    private HocKyRepository hocKyRepository;
    @Mock
    private CurrentUser currentUser;

    private ThongBaoService thongBaoService;

    private User studentUser;
    private SinhVien sinhVien;
    private User lecturerUser;
    private GiangVien giangVien;
    private HocKy hocKy;
    private MonHoc monHoc;
    private LopHocPhan lopHocPhan;

    @BeforeEach
    void setUp() {
        thongBaoService = new ThongBaoService(
                thongBaoRepository,
                dangKyRepository,
                lichSuDangKyRepository,
                sinhVienRepository,
                hocKyRepository,
                currentUser,
                null
        );

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setUsername("sv01");
        studentUser.setTrangThai(true);

        sinhVien = new SinhVien();
        sinhVien.setId(100L);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");
        sinhVien.setUser(studentUser);

        lecturerUser = new User();
        lecturerUser.setId(20L);
        lecturerUser.setUsername("gv01");
        lecturerUser.setTrangThai(true);

        giangVien = new GiangVien();
        giangVien.setId(200L);
        giangVien.setMa("GV001");
        giangVien.setHoTen("Nguyễn Văn B");
        giangVien.setUser(lecturerUser);

        hocKy = new HocKy();
        hocKy.setId(1L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2026-2027");

        monHoc = new MonHoc();
        monHoc.setId(300L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu và giải thuật");
        monHoc.setSoTinChi(3);

        lopHocPhan = new LopHocPhan();
        lopHocPhan.setId(400L);
        lopHocPhan.setMa("INT201.01");
        lopHocPhan.setMonHoc(monHoc);
        lopHocPhan.setHocKy(hocKy);
        lopHocPhan.setGiangVien(giangVien);
        lopHocPhan.setPhong("A101");
        lopHocPhan.setThu(2);
        lopHocPhan.setTietBatDau(1);
        lopHocPhan.setSoTiet(3);
    }

    @Test
    void registrationChangedIncludesCourseDetailsTimestampResultNextStepAndIsIdempotent() {
        DangKyHocPhan registration = new DangKyHocPhan();
        registration.setId(501L);
        registration.setSinhVien(sinhVien);
        registration.setLopHocPhan(lopHocPhan);
        registration.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        LocalDateTime registeredAt = LocalDateTime.of(2026, 9, 15, 8, 30);
        registration.setThoiGianDangKy(registeredAt);

        String expectedKey = "registration:501:COURSE_REGISTRATION_CONFIRMED:" + registeredAt;
        when(thongBaoRepository.existsByRecipientIdAndIdempotencyKey(10L, expectedKey))
                .thenReturn(false)
                .thenReturn(true);
        when(thongBaoRepository.existsByRecipientIdAndIdempotencyKey(20L, expectedKey))
                .thenReturn(false)
                .thenReturn(true);

        thongBaoService.registrationChanged(registration, true, registeredAt, null);
        // Retry with the same registration event must be a no-op
        thongBaoService.registrationChanged(registration, true, registeredAt, null);

        ArgumentCaptor<ThongBao> captor = ArgumentCaptor.forClass(ThongBao.class);
        verify(thongBaoRepository, times(2)).save(captor.capture());

        ThongBao studentNotice = captor.getAllValues().stream()
                .filter(n -> n.getRecipient().getId().equals(10L))
                .findFirst().orElseThrow();
        assertEquals("COURSE_REGISTRATION_CONFIRMED", studentNotice.getEventType());
        assertEquals(expectedKey, studentNotice.getIdempotencyKey());
        assertEquals("Đăng ký học phần thành công", studentNotice.getTitle());
        assertEquals("/hoc-phan-cua-toi", studentNotice.getTargetUrl());
        assertTrue(studentNotice.getMessage().contains("INT201.01"));
        assertTrue(studentNotice.getMessage().contains("INT201 - Cấu trúc dữ liệu và giải thuật"));
        assertTrue(studentNotice.getMessage().contains("15/09/2026 08:30"));
        assertTrue(studentNotice.getMessage().contains("Kết quả: Đã đăng ký"));
        assertTrue(studentNotice.getMessage().contains("Bước tiếp theo:"));
        assertFalse(studentNotice.getMessage().contains("sv01"));
    }

    @Test
    void sectionCancelledNotifiesOnlyAffectedRegistrationsAndIncludesReasonAndNextStep() {
        DangKyHocPhan activeReg = new DangKyHocPhan();
        activeReg.setId(601L);
        activeReg.setSinhVien(sinhVien);
        activeReg.setLopHocPhan(lopHocPhan);
        activeReg.setTrangThai(TrangThaiDangKy.DA_HUY);
        LocalDateTime registeredAt = LocalDateTime.of(2026, 9, 10, 9, 0);
        activeReg.setThoiGianDangKy(registeredAt);

        LocalDateTime cancelledAt = LocalDateTime.of(2026, 9, 18, 14, 15);
        thongBaoService.sectionCancelled(lopHocPhan, List.of(activeReg), cancelledAt, "Không đủ sĩ số mở lớp");

        ArgumentCaptor<ThongBao> captor = ArgumentCaptor.forClass(ThongBao.class);
        verify(thongBaoRepository, times(2)).save(captor.capture());

        ThongBao studentNotice = captor.getAllValues().stream()
                .filter(n -> n.getRecipient().getId().equals(10L))
                .findFirst().orElseThrow();
        assertEquals("COURSE_SECTION_CANCELLED", studentNotice.getEventType());
        assertEquals("Lớp học phần bị hủy", studentNotice.getTitle());
        assertEquals("HIGH", studentNotice.getPriority());
        assertEquals("/dang-ky", studentNotice.getTargetUrl());
        assertTrue(studentNotice.getMessage().contains("INT201.01"));
        assertTrue(studentNotice.getMessage().contains("Trạng thái đăng ký chuyển sang Đã hủy"));
        assertTrue(studentNotice.getMessage().contains("Không đủ sĩ số mở lớp"));
        assertTrue(studentNotice.getMessage().contains("Bước tiếp theo: mở trang đăng ký học phần để chọn lớp thay thế."));
    }

    @Test
    void classSessionCancelledNotifiesOnlyCurrentlyEnrolledStudents() {
        DangKyHocPhan activeReg = new DangKyHocPhan();
        activeReg.setId(701L);
        activeReg.setSinhVien(sinhVien);
        activeReg.setLopHocPhan(lopHocPhan);
        activeReg.setTrangThai(TrangThaiDangKy.DA_DANG_KY);

        when(dangKyRepository.findByLopHocPhan(400L, TrangThaiDangKy.DA_DANG_KY))
                .thenReturn(List.of(activeReg));

        ThongBaoNghi sessionCancel = new ThongBaoNghi();
        sessionCancel.setLopHocPhan(lopHocPhan);
        sessionCancel.setGiangVien(giangVien);
        sessionCancel.setNgayNghi(LocalDate.of(2026, 10, 5));
        sessionCancel.setLyDo("Giảng viên công tác");
        sessionCancel.setThoiGianTao(LocalDateTime.of(2026, 10, 1, 10, 0));

        thongBaoService.classSessionCancelled(sessionCancel);

        ArgumentCaptor<ThongBao> captor = ArgumentCaptor.forClass(ThongBao.class);
        verify(thongBaoRepository).save(captor.capture());
        ThongBao notice = captor.getValue();
        assertEquals("COURSE_SESSION_CANCELLED", notice.getEventType());
        assertEquals("session-cancelled:400:2026-10-05", notice.getIdempotencyKey());
        assertEquals("/thoi-khoa-bieu", notice.getTargetUrl());
        assertTrue(notice.getMessage().contains("2026-10-05"));
        assertTrue(notice.getMessage().contains("Giảng viên công tác"));
    }

    @Test
    void notificationWriteFailureDoesNotPropagateAndReconcileRecoversFromRegistrationHistory() {
        DangKyHocPhan registration = new DangKyHocPhan();
        registration.setId(801L);
        registration.setSinhVien(sinhVien);
        registration.setLopHocPhan(lopHocPhan);
        registration.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        LocalDateTime registeredAt = LocalDateTime.of(2026, 9, 20, 11, 0);
        registration.setThoiGianDangKy(registeredAt);

        doThrow(new DataAccessResourceFailureException("notification table temporarily unavailable"))
                .when(thongBaoRepository).save(any(ThongBao.class));

        // Failure is isolated and never propagates to caller
        assertDoesNotThrow(() -> thongBaoService.registrationChanged(registration, true, registeredAt, null));

        // Later, storage recovers and reconcileForUser backfills from durable LichSuDangKy
        org.mockito.Mockito.reset(thongBaoRepository);
        LichSuDangKy historyEvent = new LichSuDangKy();
        historyEvent.setDangKyId(801L);
        historyEvent.setSinhVien(sinhVien);
        historyEvent.setLopHocPhan(lopHocPhan);
        historyEvent.setHocKyId(1L);
        historyEvent.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        historyEvent.setThoiGianDangKy(registeredAt);
        historyEvent.setThoiDiem(registeredAt);

        String expectedKey = "registration:801:COURSE_REGISTRATION_CONFIRMED:" + registeredAt;
        when(lichSuDangKyRepository.findByStudentUserIdWithDetails(10L)).thenReturn(List.of(historyEvent));
        when(thongBaoRepository.existsByRecipientIdAndIdempotencyKey(eq(10L), eq(expectedKey)))
                .thenReturn(false)
                .thenReturn(true);

        int recovered = thongBaoService.reconcileForUser(10L);
        int secondRun = thongBaoService.reconcileForUser(10L);

        assertEquals(1, recovered);
        assertEquals(0, secondRun);
        verify(thongBaoRepository, times(1)).save(any(ThongBao.class));
    }
}
