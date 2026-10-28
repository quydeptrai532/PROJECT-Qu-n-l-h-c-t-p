# Kế hoạch kiểm thử

Tài liệu này tổng hợp test case theo FR/NFR, cách chạy và kết quả thực tế của dự án.

## 1. Môi trường kiểm thử

| Thành phần | Cấu hình                                                                              |
|---|---------------------------------------------------------------------------------------|
| Backend | Spring Boot 3.4, profile `h2` (in-memory), chạy trên JDK 21                                            |
| Frontend | Vite dev server `http://127.0.0.1:5173`                                               |
| Trình duyệt | Google Chrome (tự động), Microsoft Edge (tự động), Firefox/Safari (kiểm thử thủ công) |
| Dữ liệu | Seed tự động: `admin/admin123`, `gv01..gv03/123456`, `sv01..sv20/123456`              |
| Công cụ | JUnit 5 + Mockito, Spring Boot HTTP integration test, Playwright, Node.js load test |

- Máy chạy test: Windows, Intel Core i5-8350U 1,70 GHz, RAM 7,8 GB.
- Java: OpenJDK Corretto 22.0.2; Node.js v22.18.0.
- Backend: Spring Boot, profile h2 (database trong bộ nhớ), chạy cùng máy với công cụ test và IntelliJ.
## 2. Unit test backend

Chạy: `cd backend && ./mvnw test` — **19 unit test pass**.

| Nhóm | Nội dung kiểm thử | Kết quả |
|---|---|---|
| Đăng ký | Đăng ký thành công, tăng sĩ số, tạo bản ghi `DA_DANG_KY` | Pass |
| Đăng ký | Chặn ngoài thời gian đăng ký | Pass |
| Đăng ký | Chặn đăng ký trùng môn trong học kỳ | Pass |
| Đăng ký | Chặn trùng thứ/tiết | Pass |
| Đăng ký | Chặn khi lớp đủ sĩ số | Pass |
| Đăng ký | Chặn vượt tín chỉ tối đa | Pass |
| Đăng ký | Chặn thiếu môn tiên quyết | Pass |
| Đăng ký | Cho phép khi đã đạt môn tiên quyết | Pass |
| Đăng ký | Đăng ký lại sau khi hủy (kích hoạt bản ghi cũ) | Pass |
| Hủy | Hủy thành công, hoàn lại chỗ | Pass |
| Hủy | Chặn hủy khi hết thời gian | Pass |
| Hủy | Chặn hủy nếu tổng tín chỉ còn lại dưới mức tối thiểu | Pass |
| Nhập điểm | Tính điểm tổng kết 40% quá trình + 60% cuối kỳ | Pass |
| Nhập điểm | Chặn giảng viên nhập điểm lớp không phụ trách | Pass |

## 3. Kiểm thử tích hợp đăng ký đồng thời

Chạy `cd backend && ./mvnw test` để chạy cùng suite hoặc xem cấu hình capacity, student-count, concurrency, MySQL và report tại [docs/CONCURRENT_REGISTRATION_TEST.md](CONCURRENT_REGISTRATION_TEST.md). Suite tạo fixture mới cho mỗi test, gửi HTTP request đồng thời, xác nhận tranh chỗ cuối, hai lớp cùng môn, request trùng và hủy trả chỗ. Kiểm tra cả HTTP status, bản ghi đang hoạt động và sĩ số lưu. Report JSON ghi database, đầu vào, correctness và latency.

Kết quả hiện tại: **4/4 integration test pass trên H2** và **4/4 trên MySQL 8.4**. Bộ test tạo fixture riêng và kiểm tra đăng ký cùng môn đồng thời, yêu cầu trùng, hủy/đăng ký lại, tranh chỗ cuối; đối chiếu sĩ số với số bản ghi đang hoạt động.

## 4. E2E (Playwright)

Chạy: `cd frontend && npx playwright test`. Có thể chọn trình duyệt bằng biến `BROWSER_CHANNEL` (`chrome` mặc định, `msedge` cho Edge).

| Mã | Kịch bản | Kỳ vọng | Chrome | Edge | Firefox | WebKit |
|---|---|---|---|---|---|---|
| E2E-01 | Mở trang đăng nhập | Hiển thị form và tài khoản dùng thử | Pass | Pass | Pass | Pass |
| E2E-02 | Đăng nhập sai | Hiển thị lỗi, vẫn ở trang login | Pass | Pass | Pass | Pass |
| E2E-03 | Sinh viên đăng nhập | Chuyển đến `/tong-quan`, thấy dashboard | Pass | Pass | Pass | Pass |
| E2E-04 | Chưa đăng nhập vào trang bảo vệ | Chuyển về `/login` | Pass | Pass | Pass | Pass |
| E2E-05 | Cập nhật họ tên hồ sơ | Lưu thành công, tên mới hiển thị | Pass | Pass | Pass | Pass |
| E2E-06 | Sinh viên đăng ký học phần | Đăng ký thành công, học phần xuất hiện | Pass | Pass | Pass | Pass |
| E2E-07 | Hủy đăng ký bằng modal xác nhận | Nút "Giữ lại" không hủy; "Hủy đăng ký" hủy thành công | Pass | Pass | Pass | Pass |
| E2E-08 | Xem thời khóa biểu và điểm | Trang hiển thị đúng, có biểu đồ GPA | Pass | Pass | Pass | Pass |
| E2E-09 | Giảng viên xem tổng quan/lớp/nhập điểm | Dashboard, danh sách lớp và bảng điểm hiển thị | Pass | Pass | Pass | Pass |
| E2E-10 | Quản trị xem dashboard | Có thống kê, biểu đồ đăng ký và tỷ lệ lấp đầy | Pass | Pass | Pass | Pass |
| E2E-11 | Quản trị thêm/xóa môn học | Tạo môn thành công, xóa qua modal xác nhận | Pass | Pass | Pass | Pass |
| E2E-12 | Quản trị khóa/mở khóa tài khoản | Trạng thái đổi và khôi phục đúng | Pass | Pass | Pass | Pass |

Ghi chú: bảng trên gồm 12 kịch bản, trong đó 11 test độc lập (E2E-06 và E2E-07 nằm trong cùng một test đăng ký/hủy).

Chạy Firefox và WebKit: `cd frontend && npx playwright install firefox webkit && npx playwright test -c playwright.multi.config.js`.

Kết quả chạy ngày 28/10/2026, backend profile H2 khởi động mới:

| Trình duyệt | Cách chạy | Pass / Tổng | Thời gian | Lỗi phát hiện |
|---|---|---|---|---|
| Chrome | Playwright (channel chrome) | 11/11 | 1,2 phút | Không |
| Edge | Playwright (channel msedge) | 11/11 | 1,3 phút | Không |
| Firefox | Playwright (Desktop Firefox) | 11/11 | 3,9 phút (chạy chung với WebKit) | Không |
| Safari (WebKit) | Playwright (Desktop Safari) | 11/11 | 3,9 phút (chạy chung với Firefox) | Không |

Cả bốn trình duyệt đạt 100%, vượt mục tiêu T42 (từ 80% trở lên). Safari được kiểm thử bằng engine WebKit của Playwright, chưa chạy trên thiết bị Apple thật.
## 5. Kiểm thử tải (NFR-01)

Công cụ: `tools/load-test.mjs` (Node.js, không phụ thuộc thư viện ngoài).

```bash
CONCURRENCY=50 REQUESTS=200 WARMUP=40 node tools/load-test.mjs
```

Kết quả trên máy dev (H2, Windows, Node 22):

| Chỉ số | Lần 1 | Lần 2 | Lần 3 | Ngưỡng |
|---|---|---|---|---|
| Số request / đồng thời | 200 / 50 | 200 / 50 | 200 / 50 | 50 đồng thời |
| Thành công / lỗi | 200 / 0 | 200 / 0 | 200 / 0 | 0 lỗi |
| Tổng thời gian | 995,27 ms | 578,06 ms | 675,56 ms | - |
| Throughput | 200,95 req/s | 345,99 req/s | 296,05 req/s | - |
| Min | 51,31 ms | 36,05 ms | 39,23 ms | - |
| P50 | 165,74 ms | 105,49 ms | 112,87 ms | - |
| P95 | 444,29 ms | 320,04 ms | 330,43 ms | <= 500 ms |
| P99 | 565,20 ms | 331,76 ms | 369,85 ms | - |
| Max | 573,91 ms | 360,56 ms | 373,58 ms | - |
| Kết quả | [PASS] | [PASS] | [PASS] | |

Kết luận: đạt mục tiêu P95 ≤ 500 ms và không có lỗi trong môi trường H2. Cần đo lại khi chạy MySQL/Docker và trên hạ tầng thật.
- Lần 1 chậm hơn hai lần sau (P95 444 ms, gần ngưỡng) vì chạy ngay sau khi vừa khởi động backend, Java và bộ nhớ đệm chưa “nóng”. Hai lần sau ổn định quanh 320-330 ms.
- Máy chạy cả backend, client test và IntelliJ cùng lúc nên số liệu có thể thấp hơn môi trường thật.
- Công cụ chỉ đo một endpoint đọc (/api/lop-hoc-phan), chưa đo đăng ký đồng thời. Kết quả chỉ chứng minh hiệu năng đọc với 50 người dùng cùng lúc.
- Môi trường là H2 trong bộ nhớ, cần đo lại trên MySQL/Docker nếu có điều kiện.
## 6. Truy vết NFR

| NFR | Cách kiểm chứng | Trạng thái |
|---|-----------------|---|
| NFR-01 Hiệu năng | Load test 50 đồng thời, 3 lần chạy,  P95 từ 320 đến 444 ms, 0 lỗi | Đạt trên môi trường dev |
| NFR-02 Toàn vẹn dữ liệu | Transaction `READ_COMMITTED`, khóa bi quan sinh viên + lớp, unique `(sinh_vien_id, lop_hoc_phan_id)`, integration test đối chiếu sĩ số và bản ghi | Đạt |
| NFR-03 Bảo mật | JWT, BCrypt, phân quyền theo vai trò, E2E kiểm tra chuyển hướng | Đạt |
| NFR-04 Kiểm tra đầu vào | Bean Validation ở DTO, xử lý lỗi tập trung | Đạt |
| NFR-05 Ghi log | SLF4J cho đăng ký/hủy/nhập điểm/lỗi hệ thống | Đạt |
| NFR-06 Bảo trì | Tách Controller–Service–Repository–DTO, 19 unit test + 4 integration test đăng ký đồng thời | Đạt |
| NFR-07 Triển khai | Docker Compose, `docker compose config` hợp lệ | Đạt |
| NFR-08 Giao diện | Responsive, E2E chạy tự động trên Chrome, Edge, FireFox, Safari | Một phần |

## 7. Tư liệu giao diện

Bộ ảnh giao diện phục vụ báo cáo/Figma nằm trong `docs/screenshots/`, tạo lại bằng:

```bash
cd frontend
npm run screenshots
```

Gồm: đăng nhập, dashboard sinh viên, đăng ký học phần, điểm & GPA, dashboard giảng viên, lớp phụ trách, dashboard quản trị, lớp học phần, modal thêm môn học.

## 8. Hạn chế và việc cần làm
- Safari mới được kiểm thử bằng WebKit của Playwright; chưa kiểm thử trên thiết bị Apple thật (macOS/iOS).
- Load test mới đo endpoint danh sách lớp; nên bổ sung kịch bản đăng ký đồng thời để kiểm chứng khóa bi quan dưới tải.
- E2E dùng dữ liệu seed và có thao tác tạo/xóa/lock/unlock; các test đã được viết để chạy lại an toàn trên database mới.
