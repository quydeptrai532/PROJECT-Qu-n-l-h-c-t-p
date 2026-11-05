package com.edu.registration.service;

import com.edu.registration.dto.ThongBaoDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LichSuDangKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.ThongBao;
import com.edu.registration.entity.ThongBaoNghi;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.LichSuDangKyRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.ThongBaoRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ThongBaoService {

    private static final Logger log = LoggerFactory.getLogger(ThongBaoService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final String SECTION_CANCELLED_REASON_PREFIX = "Lớp học phần bị hủy";

    private final ThongBaoRepository thongBaoRepository;
    private final DangKyHocPhanRepository dangKyRepository;
    private final LichSuDangKyRepository lichSuDangKyRepository;
    private final SinhVienRepository sinhVienRepository;
    private final HocKyRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final TransactionTemplate requiresNewTemplate;

    public ThongBaoService(ThongBaoRepository thongBaoRepository,
                           DangKyHocPhanRepository dangKyRepository,
                           SinhVienRepository sinhVienRepository,
                           HocKyRepository hocKyRepository,
                           CurrentUser currentUser) {
        this(thongBaoRepository, dangKyRepository, null, sinhVienRepository, hocKyRepository, currentUser, null);
    }

    @Autowired
    public ThongBaoService(ThongBaoRepository thongBaoRepository,
                           DangKyHocPhanRepository dangKyRepository,
                           LichSuDangKyRepository lichSuDangKyRepository,
                           SinhVienRepository sinhVienRepository,
                           HocKyRepository hocKyRepository,
                           CurrentUser currentUser,
                           PlatformTransactionManager transactionManager) {
        this.thongBaoRepository = thongBaoRepository;
        this.dangKyRepository = dangKyRepository;
        this.lichSuDangKyRepository = lichSuDangKyRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    @Transactional
    public ThongBaoDtos.ThongBaoPageDto list(boolean unreadOnly, int page, int size) {
        Long recipientId = currentUser.get().getId();
        reconcileForUserSafely(recipientId);
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
        Long recipientId = currentUser.get().getId();
        reconcileForUserSafely(recipientId);
        return thongBaoRepository.markAllRead(recipientId, LocalDateTime.now());
    }

    /**
     * Emits registration/cancellation notices only after the business transaction commits,
     * isolating any notification persistence error from the committed registration state.
     */
    public void registrationChanged(DangKyHocPhan registration, boolean registered) {
        registrationChanged(registration, registered, LocalDateTime.now(), null);
    }

    public void registrationChanged(DangKyHocPhan registration, boolean registered,
                                    LocalDateTime occurredAt, String reason) {
        if (registration == null || registration.getLopHocPhan() == null || registration.getSinhVien() == null) {
            return;
        }
        LopHocPhan section = registration.getLopHocPhan();
        User studentUser = registration.getSinhVien().getUser();
        GiangVien lecturer = section.getGiangVien();
        User lecturerUser = lecturer != null ? lecturer.getUser() : null;
        Long registrationId = registration.getId();
        LocalDateTime registeredAt = registration.getThoiGianDangKy();
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt
                : (registeredAt != null ? registeredAt : LocalDateTime.now());
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";

        String eventType = registered ? "COURSE_REGISTRATION_CONFIRMED" : "COURSE_REGISTRATION_CANCELLED";
        String idempotencyKey = buildRegistrationEventKey(registrationId, eventType, registeredAt);
        String title = registered ? "Đăng ký học phần thành công" : "Đã hủy đăng ký học phần";
        String message = buildStudentRegistrationMessage(
                registered, false, sectionCode, subjectCode, subjectName, semesterName, effectiveTime, reason);
        String targetUrl = registered ? "/hoc-phan-cua-toi" : "/thoi-khoa-bieu";

        String action = registered ? "đăng ký" : "hủy đăng ký";
        String lecturerTitle = registered ? "Sinh viên đăng ký lớp học phần" : "Sinh viên hủy đăng ký lớp học phần";
        String lecturerMessage = "Có sinh viên " + action + " lớp " + sectionCode + " — "
                + subjectName + " lúc " + formatTime(effectiveTime) + ".";

        dispatchAfterCommit(eventType + ":" + idempotencyKey, () -> {
            send(studentUser, eventType, idempotencyKey, title, message, "NORMAL", targetUrl, effectiveTime);
            if (lecturerUser != null) {
                send(lecturerUser, eventType, idempotencyKey, lecturerTitle, lecturerMessage,
                        "NORMAL", "/lop-cua-toi", effectiveTime);
            }
        });
    }

    /**
     * Emits high-priority notices to only the students whose registrations were active at the moment
     * a course section was cancelled and transitioned to DA_HUY.
     */
    public void sectionCancelled(LopHocPhan section, List<DangKyHocPhan> affectedRegistrations,
                                 LocalDateTime occurredAt, String reason) {
        if (section == null) {
            return;
        }
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt : LocalDateTime.now();
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
        String cleanReason = cleanSectionCancelReason(reason);

        String eventType = "COURSE_SECTION_CANCELLED";
        String title = "Lớp học phần bị hủy";
        String studentMessage = buildStudentRegistrationMessage(
                false, true, sectionCode, subjectCode, subjectName, semesterName, effectiveTime, cleanReason);

        record RecipientNotice(User user, String key) {}
        List<RecipientNotice> studentNotices = new ArrayList<>();
        if (affectedRegistrations != null) {
            for (DangKyHocPhan reg : affectedRegistrations) {
                if (reg != null && reg.getSinhVien() != null && reg.getSinhVien().getUser() != null) {
                    String key = buildRegistrationEventKey(reg.getId(), eventType, reg.getThoiGianDangKy());
                    studentNotices.add(new RecipientNotice(reg.getSinhVien().getUser(), key));
                }
            }
        }

        User lecturerUser = section.getGiangVien() != null ? section.getGiangVien().getUser() : null;
        String lecturerKey = "section-cancelled:" + section.getId() + ":"
                + (cleanReason == null ? "default" : cleanReason);
        String lecturerMessage = "Lớp học phần " + sectionCode + " — " + subjectName + " (" + semesterName
                + ") đã bị hủy lúc " + formatTime(effectiveTime) + formatReason(cleanReason)
                + ". Bước tiếp theo: kiểm tra danh sách lớp phụ trách.";

        dispatchAfterCommit(eventType + ":section:" + section.getId(), () -> {
            for (RecipientNotice item : studentNotices) {
                send(item.user(), eventType, item.key(), title, studentMessage, "HIGH", "/dang-ky", effectiveTime);
            }
            if (lecturerUser != null) {
                send(lecturerUser, eventType, lecturerKey, title, lecturerMessage,
                        "HIGH", "/lop-cua-toi", effectiveTime);
            }
        });
    }

    /**
     * Emits notices to currently enrolled students when a lecturer announces a class session cancellation.
     */
    public void classSessionCancelled(ThongBaoNghi thongBaoNghi) {
        if (thongBaoNghi == null || thongBaoNghi.getLopHocPhan() == null) {
            return;
        }
        LopHocPhan section = thongBaoNghi.getLopHocPhan();
        Long sectionId = section.getId();
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
        LocalDate ngayNghi = thongBaoNghi.getNgayNghi();
        LocalDateTime effectiveTime = thongBaoNghi.getThoiGianTao() != null
                ? thongBaoNghi.getThoiGianTao() : LocalDateTime.now();
        String reason = thongBaoNghi.getLyDo();

        List<User> recipients = dangKyRepository.findByLopHocPhan(sectionId, TrangThaiDangKy.DA_DANG_KY)
                .stream()
                .filter(reg -> reg.getSinhVien() != null && reg.getSinhVien().getUser() != null)
                .map(reg -> reg.getSinhVien().getUser())
                .toList();

        String eventType = "COURSE_SESSION_CANCELLED";
        String idempotencyKey = "session-cancelled:" + sectionId + ":" + ngayNghi;
        String title = "Thông báo nghỉ học lớp " + sectionCode;
        String subjectLabel = subjectCode.isBlank() ? subjectName : subjectCode + " — " + subjectName;
        String message = "Lớp " + sectionCode + " (" + subjectLabel + ", " + semesterName
                + ") tạm nghỉ buổi học ngày " + ngayNghi + " (cập nhật lúc " + formatTime(effectiveTime) + "). "
                + "Kết quả: Buổi học tạm nghỉ" + formatReason(reason)
                + ". Bước tiếp theo: theo dõi lịch học bù và thời khóa biểu cá nhân.";

        dispatchAfterCommit(eventType + ":" + idempotencyKey, () -> {
            for (User recipient : recipients) {
                send(recipient, eventType, idempotencyKey, title, message,
                        "HIGH", "/thoi-khoa-bieu", effectiveTime);
            }
        });
    }

    /** Called only when the locked section's schedule or assigned lecturer actually changes. */
    public void sectionChanged(LopHocPhan section, Map<String, Object> before, Map<String, Object> after,
                               String idempotencyKey) {
        boolean scheduleChanged = !same(before, after, "phong") || !same(before, after, "thu")
                || !same(before, after, "tietBatDau") || !same(before, after, "soTiet");
        boolean lecturerChanged = !same(before, after, "giangVienId");
        if (!scheduleChanged && !lecturerChanged) return;

        String eventType = scheduleChanged ? "COURSE_SECTION_SCHEDULE_CHANGED" : "COURSE_SECTION_ASSIGNMENT_CHANGED";
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : deterministicSectionChangeKey(section.getId(), eventType, after);
        LocalDateTime occurredAt = LocalDateTime.now();
        String title = scheduleChanged ? "Lịch lớp học phần đã thay đổi" : "Giảng viên phụ trách lớp đã thay đổi";
        String message = scheduleChanged
                ? "Lịch lớp " + section.getMa() + " — " + section.getMonHoc().getTen() + " đã đổi lúc "
                    + formatTime(occurredAt) + ": " + thuLabel(section.getThu()) + ", tiết "
                    + section.getTietBatDau() + "-" + section.getTietKetThuc() + ", phòng " + section.getPhong()
                    + ". Bước tiếp theo: kiểm tra lại thời khóa biểu."
                : "Giảng viên phụ trách lớp " + section.getMa() + " — " + section.getMonHoc().getTen()
                    + " đã được cập nhật lúc " + formatTime(occurredAt)
                    + ". Bước tiếp theo: kiểm tra thông tin lớp học phần.";

        List<User> enrolledStudents = dangKyRepository.findByLopHocPhan(
                        section.getId(), TrangThaiDangKy.DA_DANG_KY)
                .stream()
                .filter(reg -> reg.getSinhVien() != null && reg.getSinhVien().getUser() != null)
                .map(reg -> reg.getSinhVien().getUser())
                .toList();
        User lecturerUser = section.getGiangVien() != null ? section.getGiangVien().getUser() : null;

        dispatchAfterCommit(eventType + ":" + effectiveKey, () -> {
            for (User studentUser : enrolledStudents) {
                send(studentUser, eventType, effectiveKey, title, message,
                        "NORMAL", "/thoi-khoa-bieu", occurredAt);
            }
            if (lecturerUser != null) {
                send(lecturerUser, eventType, effectiveKey, title, message,
                        "NORMAL", "/lop-cua-toi/thoi-khoa-bieu", occurredAt);
            }
        });
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
            if (send(student.getUser(), "REGISTRATION_WINDOW_CLOSING", key,
                    title, message, "HIGH", "/dang-ky", occurredAt)) {
                created++;
            }
        }
        return created;
    }

    /**
     * Reconciles durable LichSuDangKy events for a user so any notification whose post-commit write
     * failed transiently is deterministically recovered without duplication.
     */
    public int reconcileForUser(Long userId) {
        if (userId == null || lichSuDangKyRepository == null) {
            return 0;
        }
        List<LichSuDangKy> events = lichSuDangKyRepository.findByStudentUserIdWithDetails(userId);
        int recovered = 0;
        for (LichSuDangKy event : events) {
            if (event.getDangKyId() == null || event.getLopHocPhan() == null || event.getSinhVien() == null) {
                continue;
            }
            User studentUser = event.getSinhVien().getUser();
            if (studentUser == null || !studentUser.isTrangThai()) {
                continue;
            }
            boolean registered = event.getTrangThai() == TrangThaiDangKy.DA_DANG_KY;
            boolean sectionCancelled = !registered && isSectionCancelledReason(event.getLyDoHuy());
            String eventType = registered
                    ? "COURSE_REGISTRATION_CONFIRMED"
                    : (sectionCancelled ? "COURSE_SECTION_CANCELLED" : "COURSE_REGISTRATION_CANCELLED");
            String idempotencyKey = buildRegistrationEventKey(
                    event.getDangKyId(), eventType, event.getThoiGianDangKy());
            LopHocPhan section = event.getLopHocPhan();
            String sectionCode = section.getMa();
            String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
            String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
            String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
            LocalDateTime occurredAt = event.getThoiDiem() != null
                    ? event.getThoiDiem() : event.getThoiGianDangKy();
            String reason = sectionCancelled ? cleanSectionCancelReason(event.getLyDoHuy()) : event.getLyDoHuy();
            String title = registered
                    ? "Đăng ký học phần thành công"
                    : (sectionCancelled ? "Lớp học phần bị hủy" : "Đã hủy đăng ký học phần");
            String message = buildStudentRegistrationMessage(
                    registered, sectionCancelled, sectionCode, subjectCode, subjectName, semesterName, occurredAt, reason);
            String priority = sectionCancelled ? "HIGH" : "NORMAL";
            String targetUrl = registered
                    ? "/hoc-phan-cua-toi"
                    : (sectionCancelled ? "/dang-ky" : "/thoi-khoa-bieu");
            if (send(studentUser, eventType, idempotencyKey, title, message, priority, targetUrl, occurredAt)) {
                recovered++;
            }
        }
        return recovered;
    }

    private void reconcileForUserSafely(Long userId) {
        if (userId == null || lichSuDangKyRepository == null) {
            return;
        }
        try {
            if (requiresNewTemplate != null) {
                requiresNewTemplate.executeWithoutResult(status -> reconcileForUser(userId));
            } else {
                reconcileForUser(userId);
            }
        } catch (RuntimeException ex) {
            log.warn("Không thể đồng bộ bù thông báo đăng ký cho userId={}: {}",
                    userId, ex.getClass().getSimpleName());
        }
    }

    private void dispatchAfterCommit(String context, Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeSafelyInNewTransaction(context, task);
                }
            });
        } else {
            executeSafelyInNewTransaction(context, task);
        }
    }

    private void executeSafelyInNewTransaction(String context, Runnable task) {
        try {
            if (requiresNewTemplate != null) {
                requiresNewTemplate.executeWithoutResult(status -> task.run());
            } else {
                task.run();
            }
        } catch (RuntimeException ex) {
            log.error("Không thể lưu thông báo nội bộ [{}]; giao dịch nghiệp vụ đã commit không bị ảnh hưởng: {}",
                    context, ex.getClass().getSimpleName());
        }
    }

    private boolean send(User recipient, String eventType, String idempotencyKey, String title,
                         String message, String priority, String targetUrl, LocalDateTime createdAt) {
        if (recipient == null || !recipient.isTrangThai()
                || thongBaoRepository.existsByRecipientIdAndIdempotencyKey(recipient.getId(), idempotencyKey)) {
            return false;
        }
        ThongBao notification = new ThongBao();
        notification.setRecipient(recipient);
        notification.setEventType(eventType);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority);
        notification.setTargetUrl(targetUrl);
        notification.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        try {
            thongBaoRepository.save(notification);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Bỏ qua thông báo trùng idempotencyKey={} cho recipientId={}",
                    idempotencyKey, recipient.getId());
            return false;
        }
    }

    public static String buildRegistrationEventKey(Long registrationId, String eventType, LocalDateTime registeredAt) {
        LocalDateTime normalized = registeredAt != null ? registeredAt.truncatedTo(ChronoUnit.SECONDS) : null;
        return "registration:" + registrationId + ":" + eventType + ":" + normalized;
    }

    public static String deterministicSectionChangeKey(Long sectionId, String eventType, Map<String, Object> after) {
        if (after == null) {
            return "section-update:" + sectionId + ":" + eventType;
        }
        return "section-update:" + sectionId + ":" + eventType + ":"
                + after.get("phong") + ":" + after.get("thu") + ":"
                + after.get("tietBatDau") + ":" + after.get("soTiet") + ":"
                + after.get("giangVienId");
    }

    private String buildStudentRegistrationMessage(boolean registered, boolean sectionCancelled,
                                                   String sectionCode, String subjectCode,
                                                   String subjectName, String semesterName,
                                                   LocalDateTime occurredAt, String reason) {
        String subjectLabel = (subjectCode == null || subjectCode.isBlank())
                ? subjectName
                : subjectCode + " - " + subjectName;
        String timeStr = formatTime(occurredAt);
        if (registered) {
            return "Bạn đã đăng ký lớp " + sectionCode + " — " + subjectLabel
                    + ", " + semesterName + " lúc " + timeStr
                    + ". Kết quả: Đã đăng ký. Bước tiếp theo: kiểm tra danh sách học phần đã đăng ký và thời khóa biểu.";
        }
        if (sectionCancelled) {
            return "Lớp học phần " + sectionCode + " — " + subjectLabel
                    + ", " + semesterName + " đã bị hủy lúc " + timeStr
                    + ". Kết quả: Trạng thái đăng ký chuyển sang Đã hủy" + formatReason(reason)
                    + ". Bước tiếp theo: mở trang đăng ký học phần để chọn lớp thay thế.";
        }
        return "Bạn đã hủy đăng ký lớp " + sectionCode + " — " + subjectLabel
                + ", " + semesterName + " lúc " + timeStr
                + ". Kết quả: Đã hủy đăng ký" + formatReason(reason)
                + ". Bước tiếp theo: kiểm tra lại thời khóa biểu hoặc đăng ký lớp học phần khác nếu cần.";
    }

    private boolean isSectionCancelledReason(String reason) {
        return reason != null && reason.startsWith(SECTION_CANCELLED_REASON_PREFIX);
    }

    private String cleanSectionCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.equals(SECTION_CANCELLED_REASON_PREFIX)) {
            return null;
        }
        if (trimmed.startsWith(SECTION_CANCELLED_REASON_PREFIX + ":")) {
            String detail = trimmed.substring((SECTION_CANCELLED_REASON_PREFIX + ":").length()).trim();
            return detail.isEmpty() ? null : detail;
        }
        return trimmed;
    }

    private String formatReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " (Lý do: " + reason.trim() + ")";
    }

    private String formatTime(LocalDateTime time) {
        return (time != null ? time : LocalDateTime.now()).format(TIME_FORMATTER);
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
