package com.edu.registration.concurrent;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentRegistrationIntegrationTest {

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry properties) {
        String jdbcUrl = System.getProperty("registration.test.jdbc-url",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_URL",
                        "jdbc:h2:mem:registration_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"));
        boolean mysql = jdbcUrl.startsWith("jdbc:mysql:");
        properties.add("spring.datasource.url", () -> jdbcUrl);
        properties.add("spring.datasource.username", () -> System.getProperty("registration.test.db-username",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_USERNAME", "sa")));
        properties.add("spring.datasource.password", () -> System.getProperty("registration.test.db-password",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_PASSWORD", "")));
        properties.add("spring.datasource.driver-class-name", () -> mysql
                ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("app.seed.enabled", () -> "true");
    }

    @Autowired private TestRestTemplate http;
    @Autowired private UserRepository userRepository;
    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private KhoaRepository khoaRepository;
    @Autowired private HocKyRepository hocKyRepository;
    @Autowired private MonHocRepository monHocRepository;
    @Autowired private GiangVienRepository giangVienRepository;
    @Autowired private LopHocPhanRepository lopHocPhanRepository;
    @Autowired private DangKyHocPhanRepository dangKyRepository;
    @Autowired private JwtService jwtService;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;

    @Value("${registration.test.capacity:3}") private int capacity;
    @Value("${registration.test.student-count:12}") private int studentCount;
    @Value("${registration.test.concurrency:12}") private int concurrency;

    private String runId;
    private List<User> students;
    private LopHocPhan primarySection;
    private LopHocPhan secondSection;

    @BeforeEach
    void createIsolatedFixture() {
        assertTrue(capacity > 0, "capacity phải lớn hơn 0");
        assertTrue(studentCount > capacity, "student-count phải lớn hơn capacity để có tranh suất");
        assertTrue(concurrency > 0, "concurrency phải lớn hơn 0");
        runId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Khoa khoa = new Khoa();
        khoa.setMa("T" + runId);
        khoa.setTen("Khoa concurrency test " + runId);
        khoa = khoaRepository.saveAndFlush(khoa);

        HocKy hocKy = new HocKy();
        hocKy.setTen("Concurrency test " + runId);
        hocKy.setNamHoc("2099-2100");
        hocKy.setNgayBatDau(LocalDate.now().minusDays(1));
        hocKy.setNgayKetThuc(LocalDate.now().plusDays(30));
        hocKy.setDangKyTu(LocalDate.now().minusDays(1));
        hocKy.setDangKyDen(LocalDate.now().plusDays(30));
        hocKy.setSoTinChiToiThieu(0);
        hocKy.setSoTinChiToiDa(24);
        hocKy = hocKyRepository.saveAndFlush(hocKy);

        MonHoc mon = new MonHoc();
        mon.setMa("T" + runId);
        mon.setTen("Môn concurrency test " + runId);
        mon.setSoTinChi(3);
        mon.setKhoa(khoa);
        mon = monHocRepository.saveAndFlush(mon);

        User lecturerUser = new User();
        lecturerUser.setUsername("test-gv-" + runId);
        lecturerUser.setPassword("unused");
        lecturerUser.setRole(Role.GIANG_VIEN);
        lecturerUser = userRepository.saveAndFlush(lecturerUser);
        GiangVien lecturer = new GiangVien();
        lecturer.setUser(lecturerUser);
        lecturer.setMa("GV" + runId);
        lecturer.setHoTen("Giảng viên test");
        lecturer = giangVienRepository.saveAndFlush(lecturer);

        primarySection = saveSection("01", mon, hocKy, lecturer, capacity, 2);
        secondSection = saveSection("02", mon, hocKy, lecturer, capacity, 4);

        students = new ArrayList<>();
        int fixtureCount = Math.max(studentCount, 2);
        for (int i = 0; i < fixtureCount; i++) {
            User user = new User();
            user.setUsername("test-sv-" + runId + "-" + i);
            user.setPassword("unused");
            user.setRole(Role.SINH_VIEN);
            user = userRepository.saveAndFlush(user);
            SinhVien student = new SinhVien();
            student.setUser(user);
            student.setMa("SV" + runId + i);
            student.setHoTen("Sinh viên test " + i);
            sinhVienRepository.saveAndFlush(student);
            students.add(user);
        }
    }

    @Test
    void concurrentLastSeatRaceNeverOverbooksAndWritesEvidenceReport() throws Exception {
        int requests = studentCount;
        List<Attempt> attempts = concurrentRequests(requests, index ->
                postRegistration(students.get(index), primarySection.getId()));

        long successes = attempts.stream().filter(Attempt::success).count();
        long conflicts = attempts.stream().filter(attempt -> attempt.status() == 409).count();
        entityManager.clear();
        LopHocPhan storedSection = lopHocPhanRepository.findById(primarySection.getId()).orElseThrow();
        long activeRows = dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), TrangThaiDangKy.DA_DANG_KY);

        assertEquals(Math.min(capacity, requests), successes, "Số đăng ký thành công phải lấp đúng sức chứa");
        assertEquals(requests - successes, conflicts, "Yêu cầu thua cuộc phải nhận HTTP 409 nghiệp vụ");
        assertTrue(successes <= capacity, "Không được vượt sức chứa");
        assertEquals(successes, activeRows, "Số bản ghi đăng ký phải khớp số request thành công");
        assertEquals(activeRows, storedSection.getSiSoHienTai(), "Sĩ số lưu phải khớp bản ghi đang hoạt động");

        writeReport(attempts, requests, successes, conflicts, storedSection.getSiSoHienTai(), activeRows);
    }

    @Test
    void sameStudentCannotRegisterTwoSectionsOfSameCourseConcurrently() throws Exception {
        List<Attempt> attempts = concurrentRequests(2, index -> postRegistration(
                students.get(0), index == 0 ? primarySection.getId() : secondSection.getId()));

        assertEquals(1, attempts.stream().filter(Attempt::success).count());
        assertEquals(1, attempts.stream().filter(attempt -> attempt.status() == 409).count());
        assertEquals(1, dangKyRepository.findTheoHocKy(
                sinhVienRepository.findByUser(students.get(0)).orElseThrow().getId(),
                primarySection.getHocKy().getId(), TrangThaiDangKy.DA_DANG_KY).size());
    }

    @Test
    void duplicateRequestForSameStudentAndSectionReturnsConflict() {
        ResponseEntity<String> first = postRegistration(students.get(0), primarySection.getId());
        ResponseEntity<String> duplicate = postRegistration(students.get(0), primarySection.getId());

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(1, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), TrangThaiDangKy.DA_DANG_KY));
        entityManager.clear();
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void cancellationReopensSeatAndKeepsCancelledRecord() {
        ResponseEntity<String> registered = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, registered.getStatusCode());
        Long registrationId = readId(registered.getBody());
        HttpHeaders headers = headersFor(students.get(0));
        ResponseEntity<Void> cancelled = http.exchange("/api/dang-ky/" + registrationId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        entityManager.clear();
        DangKyHocPhan row = dangKyRepository.findById(registrationId).orElseThrow();
        LopHocPhan section = lopHocPhanRepository.findById(primarySection.getId()).orElseThrow();
        assertEquals(TrangThaiDangKy.DA_HUY, row.getTrangThai());
        assertEquals(0, section.getSiSoHienTai());
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), TrangThaiDangKy.DA_DANG_KY));

        ResponseEntity<String> retried = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, retried.getStatusCode());
        assertEquals(registrationId, readId(retried.getBody()), "Đăng ký lại phải kích hoạt bản ghi đã hủy");
        entityManager.clear();
        assertEquals(TrangThaiDangKy.DA_DANG_KY,
                dangKyRepository.findById(registrationId).orElseThrow().getTrangThai());
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    private LopHocPhan saveSection(String suffix, MonHoc mon, HocKy term, GiangVien lecturer,
                                   int max, int weekday) {
        LopHocPhan section = new LopHocPhan();
        section.setMa("T" + runId + "." + suffix);
        section.setMonHoc(mon);
        section.setHocKy(term);
        section.setGiangVien(lecturer);
        section.setPhong("TEST");
        section.setThu(weekday);
        section.setTietBatDau(1);
        section.setSoTiet(2);
        section.setSiSoToiDa(max);
        section.setSiSoHienTai(0);
        return lopHocPhanRepository.saveAndFlush(section);
    }

    private List<Attempt> concurrentRequests(int requestCount, java.util.function.IntFunction<ResponseEntity<String>> action)
            throws Exception {
        int workers = Math.max(1, Math.min(concurrency, requestCount));
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<CompletableFuture<Attempt>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        long started = System.nanoTime();
                        ResponseEntity<String> response = action.apply(index);
                        return new Attempt(response.getStatusCode().value(),
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), response.getBody());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                }, executor));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<String> postRegistration(User student, Long sectionId) {
        return http.exchange("/api/dang-ky", HttpMethod.POST,
                new HttpEntity<>(Map.of("lopHocPhanId", sectionId), headersFor(student)), String.class);
    }

    private HttpHeaders headersFor(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(user));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Long readId(String response) {
        try {
            return objectMapper.readTree(response).get("id").asLong();
        } catch (Exception ex) {
            throw new AssertionError("Response đăng ký không có id: " + response, ex);
        }
    }

    private void writeReport(List<Attempt> attempts, int requests, long successes, long conflicts,
                             int storedCount, long activeRows) throws Exception {
        List<Long> latencies = attempts.stream().map(Attempt::latencyMs).sorted(Comparator.naturalOrder()).toList();
        String database;
        try (Connection connection = dataSource.getConnection()) {
            database = connection.getMetaData().getDatabaseProductName() + " "
                    + connection.getMetaData().getDatabaseProductVersion();
        }
        Map<String, Object> report = Map.of(
                "environment", Map.of("database", database, "profile", "integration-test",
                        "fixture", "isolated per test run"),
                "input", Map.of("capacity", capacity, "students", requests, "concurrency", Math.min(concurrency, requests),
                        "sectionId", primarySection.getId()),
                "result", Map.of("success", successes, "businessConflicts", conflicts,
                        "overCapacity", successes > capacity, "activeRegistrations", activeRows,
                        "storedClassCount", storedCount, "consistent", activeRows == storedCount),
                "latencyMs", Map.of("p50", percentile(latencies, 50), "p95", percentile(latencies, 95),
                        "p99", percentile(latencies, 99), "max", latencies.get(latencies.size() - 1)),
                "responses", attempts.stream().map(attempt -> Map.of("status", attempt.status(),
                        "latencyMs", attempt.latencyMs(), "body", attempt.body() == null ? "" : attempt.body())).toList());
        Path reportPath = Path.of(System.getProperty("registration.report", "target/concurrent-registration-report.json"));
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(reportPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private long percentile(List<Long> sorted, int percentile) {
        int index = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(percentile * sorted.size() / 100.0) - 1));
        return sorted.get(index);
    }

    private record Attempt(int status, long latencyMs, String body) {
        boolean success() { return status >= 200 && status < 300; }
    }
}
