package com.edu.registration.service;

import com.edu.registration.repository.HocKyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class ThongBaoReminderJob {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final HocKyRepository hocKyRepository;
    private final ThongBaoService thongBaoService;

    public ThongBaoReminderJob(HocKyRepository hocKyRepository, ThongBaoService thongBaoService) {
        this.hocKyRepository = hocKyRepository;
        this.thongBaoService = thongBaoService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void remindClosingRegistrationWindows() {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        hocKyRepository.findAll().stream()
                .filter(semester -> !semester.getDangKyTu().isAfter(today)
                        && !semester.getDangKyDen().isBefore(today)
                        && !semester.getDangKyDen().isAfter(today.plusDays(1)))
                .forEach(semester -> thongBaoService.remindClosingRegistrationWindow(semester.getId(), today));
    }
}
