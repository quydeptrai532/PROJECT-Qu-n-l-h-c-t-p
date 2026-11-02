package com.edu.registration.service;

import com.edu.registration.dto.ThongBaoDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.ThongBao;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.ThongBaoRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;
    private final DangKyHocPhanRepository dangKyRepository;
    private final SinhVienRepository sinhVienRepository;
    private final HocKyRepository hocKyRepository;
    private final CurrentUser currentUser;

    public ThongBaoService(ThongBaoRepository thongBaoRepository,
                           DangKyHocPhanRepository dangKyRepository,
                           SinhVienRepository sinhVienRepository,
                           HocKyRepository hocKyRepository, CurrentUser currentUser) {
        this.thongBaoRepository = thongBaoRepository;
        this.dangKyRepository = dangKyRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public ThongBaoDtos.ThongBaoPageDto list(boolean unreadOnly, int page, int size) {
        Long recipientId = currentUser.get().getId();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest request = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<ThongBao> notifications = unreadOnly
                ? thongBaoRepository.findByRecipientIdAndReadAtIsNull(recipientId, request)
                : thongBaoRepository.findByRecipientId(recipientId, request);
        return new ThongBaoDtos.ThongBaoPageDto(
                notifications.getContent().stream().map(this::toDto).toList(),
                notifications.getNumber(), notifications.getSize(), notifications.getTotalElements(),
                notifications.getTotalPages(), thongBaoRepository.countByRecipientIdAndReadAtIsNull(recipientId));
    }

    @Transactional
    public ThongBaoDtos.ThongBaoDto markRead(Long id) {
        Long recipientId = currentUser.get().getId();
        ThongBao notification = thongBaoRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> ApiException.notFound("Thông báo không tồn tại"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return toDto(thongBaoRepository.save(notification));
    }

    @Transactional
    public long markAllRead() {
        return thongBaoRepository.markAllRead(currentUser.get().getId(), LocalDateTime.now());
    }

    /** Runs inside the registration transaction so a committed registration has its in-app notice. */
    @Transactional
    public void registrationChanged(DangKyHocPhan registration, boolean registered) {
        LopHocPhan section = registration.getLopHocPhan();
        String eventType = registered ? "COURSE_REGISTRATION_CONFIRMED" : "COURSE_REGISTRATION_CANCELLED";
        String action = registered ? "đăng ký" : "hủy đăng ký";
        String title = registered ? "Đăng ký học phần thành công" : "Đã hủy đăng ký học phần";
        String message = "Bạn đã " + action + " lớp " + section.getMa() + " — "
                + section.getMonHoc().getTen() + ", " + section.getHocKy().getTen() + ".";
        String idempotencyKey = "registration:" + registration.getId() + ":" + eventType + ":"
                + registration.getThoiGianDangKy();
        LocalDateTime occurredAt = LocalDateTime.now();
        send(registration.getSinhVien().getUser(), eventType, idempotencyKey, title, message,
                "NORMAL", registered ? "/hoc-phan-cua-toi" : "/thoi-khoa-bieu", occurredAt);

        GiangVien lecturer = section.getGiangVien();
        if (lecturer != null) {
            String lecturerTitle = registered ? "Sinh viên đăng ký lớp học phần" : "Sinh viên hủy đăng ký lớp học phần";
            String lecturerMessage = "Có sinh viên " + action + " lớp " + section.getMa() + " — "
                    + section.getMonHoc().getTen() + ".";
            send(lecturer.getUser(), eventType, idempotencyKey, lecturerTitle, lecturerMessage,
                    "NORMAL", "/lop-cua-toi", occurredAt);
        }
    }

    /** Called only when the locked section's schedule or assigned lecturer actually changes. */
    @Transactional
    public void sectionChanged(LopHocPhan section, Map<String, Object> before, Map<String, Object> after,
                               String idempotencyKey) {
        boolean scheduleChanged = !same(before, after, "phong") || !same(before, after, "thu")
                || !same(before, after, "tietBatDau") || !same(before, after, "soTiet");
        boolean lecturerChanged = !same(before, after, "giangVienId");
        if (!scheduleChanged && !lecturerChanged) return;

        String eventType = scheduleChanged ? "COURSE_SECTION_SCHEDULE_CHANGED" : "COURSE_SECTION_ASSIGNMENT_CHANGED";
        String title = scheduleChanged ? "Lịch lớp học phần đã thay đổi" : "Giảng viên phụ trách lớp đã thay đổi";
        String message = scheduleChanged
                ? "Lịch lớp " + section.getMa() + " — " + section.getMonHoc().getTen() + " đã đổi: "
                    + thuLabel(section.getThu()) + ", tiết " + section.getTietBatDau() + "-"
                    + section.getTietKetThuc() + ", phòng " + section.getPhong() + "."
                : "Giảng viên phụ trách lớp " + section.getMa() + " — " + section.getMonHoc().getTen()
                    + " đã được cập nhật.";
        LocalDateTime occurredAt = LocalDateTime.now();

        for (DangKyHocPhan registration : dangKyRepository.findByLopHocPhan(
                section.getId(), TrangThaiDangKy.DA_DANG_KY)) {
            send(registration.getSinhVien().getUser(), eventType, idempotencyKey, title, message,
                    "NORMAL", "/thoi-khoa-bieu", occurredAt);
        }
        if (section.getGiangVien() != null) {
            send(section.getGiangVien().getUser(), eventType, idempotencyKey, title, message,
                    "NORMAL", "/lop-cua-toi/thoi-khoa-bieu", occurredAt);
        }
    }

    /** Idempotent daily reminder; the semester row lock serializes multiple scheduler instances. */
    @Transactional
    public int remindClosingRegistrationWindow(Long semesterId, LocalDate today) {
        HocKy semester = hocKyRepository.findByIdForUpdate(semesterId).orElse(null);
        if (semester == null || semester.getDangKyTu().isAfter(today)
                || semester.getDangKyDen().isBefore(today)
                || semester.getDangKyDen().isAfter(today.plusDays(1))) {
            return 0;
        }
        String key = "registration-window-closing:" + semester.getId() + ":" + semester.getDangKyDen();
        int created = 0;
        LocalDateTime occurredAt = LocalDateTime.now();
        String title = "Đợt đăng ký học phần sắp kết thúc";
        String message = "Đợt đăng ký " + semester.getTen() + " sẽ kết thúc ngày "
                + semester.getDangKyDen() + ". Hãy hoàn tất kế hoạch học tập nếu cần.";
        for (SinhVien student : sinhVienRepository.findAllActiveWithUser()) {
            String recipientKey = key;
            if (!thongBaoRepository.existsByRecipientIdAndIdempotencyKey(student.getUser().getId(), recipientKey)) {
                send(student.getUser(), "REGISTRATION_WINDOW_CLOSING", recipientKey,
                        title, message, "HIGH", "/dang-ky", occurredAt);
                created++;
            }
        }
        return created;
    }

    private void send(User recipient, String eventType, String idempotencyKey, String title,
                      String message, String priority, String targetUrl, LocalDateTime createdAt) {
        if (recipient == null || !recipient.isTrangThai()
                || thongBaoRepository.existsByRecipientIdAndIdempotencyKey(recipient.getId(), idempotencyKey)) {
            return;
        }
        ThongBao notification = new ThongBao();
        notification.setRecipient(recipient);
        notification.setEventType(eventType);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority);
        notification.setTargetUrl(targetUrl);
        notification.setCreatedAt(createdAt);
        thongBaoRepository.save(notification);
    }

    private boolean same(Map<String, Object> before, Map<String, Object> after, String key) {
        return java.util.Objects.equals(before.get(key), after.get(key));
    }

    private String thuLabel(int thu) {
        return thu == 8 ? "Chủ nhật" : "Thứ " + (thu - 1);
    }

    private ThongBaoDtos.ThongBaoDto toDto(ThongBao notification) {
        boolean read = notification.getReadAt() != null;
        return new ThongBaoDtos.ThongBaoDto(notification.getId(), notification.getEventType(),
                notification.getTitle(), notification.getMessage(), notification.getPriority(),
                notification.getTargetUrl(), notification.getCreatedAt(), notification.getReadAt(), read);
    }
}
