# Kế hoạch kiểm thử

Tài liệu này tổng hợp test case theo FR/NFR, cách chạy và kết quả thực tế của dự án.

## 1. Môi trường kiểm thử

| Thành phần | Cấu hình |
|---|---|
| Backend | Spring Boot 3.4, Java 21, profile `h2` (in-memory) |
| Frontend | Vite dev server `http://127.0.0.1:5173` |
| Trình duyệt | Google Chrome (tự động), Microsoft Edge (tự động), Firefox/Safari (kiểm thử thủ công) |
| Dữ liệu | Seed tự động: `admin/admin123`, `gv01..gv03/123456`, `sv01..sv20/123456` |
| Công cụ | JUnit 5 + Mockito, Playwright, Node.js load test |

## 2. Unit test backend

Chạy: `cd backend && ./mvnw test` — **14/14 pass**.

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

## 3. E2E (Playwright)

Chạy: `cd frontend && npx playwright test`. Có thể chọn trình duyệt bằng biến `BROWSER_CHANNEL` (`chrome` mặc định, `msedge` cho Edge).

| Mã | Kịch bản | Kỳ vọng | Kết quả Chrome | Kết quả Edge |
|---|---|---|---|---|
| E2E-01 | Mở trang đăng nhập | Hiển thị form và tài khoản dùng thử | Pass | Pass |
| E2E-02 | Đăng nhập sai | Hiển thị lỗi, vẫn ở trang login | Pass | Pass |
| E2E-03 | Sinh viên đăng nhập | Chuyển đến `/tong-quan`, thấy dashboard | Pass | Pass |
| E2E-04 | Chưa đăng nhập vào trang bảo vệ | Chuyển về `/login` | Pass | Pass |
| E2E-05 | Cập nhật họ tên hồ sơ | Lưu thành công, tên mới hiển thị | Pass | Pass |
| E2E-06 | Sinh viên đăng ký học phần | Đăng ký thành công, học phần xuất hiện | Pass | Pass |
| E2E-07 | Hủy đăng ký bằng modal xác nhận | Nút "Giữ lại" không hủy; "Hủy đăng ký" hủy thành công | Pass | Pass |
| E2E-08 | Xem thời khóa biểu và điểm | Trang hiển thị đúng, có biểu đồ GPA | Pass | Pass |
| E2E-09 | Giảng viên xem tổng quan/lớp/nhập điểm | Dashboard, danh sách lớp và bảng điểm hiển thị | Pass | Pass |
| E2E-10 | Quản trị xem dashboard | Có thống kê, biểu đồ đăng ký và tỷ lệ lấp đầy | Pass | Pass |
| E2E-11 | Quản trị thêm/xóa môn học | Tạo môn thành công, xóa qua modal xác nhận | Pass | Pass |
| E2E-12 | Quản trị khóa/mở khóa tài khoản | Trạng thái đổi và khôi phục đúng | Pass | Pass |

Ghi chú: bảng trên gồm 12 kịch bản, trong đó 11 test độc lập (E2E-06 và E2E-07 nằm trong cùng một test đăng ký/hủy).

## 4. Kiểm thử tải (NFR-01)

Công cụ: `tools/load-test.mjs` (Node.js, không phụ thuộc thư viện ngoài).

```bash
CONCURRENCY=50 REQUESTS=200 WARMUP=40 node tools/load-test.mjs
```

Kết quả trên máy dev (H2, Windows, Node 22):

| Chỉ số | Giá trị | Ngưỡng |
|---|---|---|
| Số request | 200 | — |
| Đồng thời | 50 | 50 |
| Thành công / lỗi | 200 / 0 | 0 lỗi |
| Throughput | ~261 req/s | — |
| P50 | ~141 ms | — |
| P95 | ~332 ms | ≤ 500 ms |
| P99 | ~346 ms | — |

Kết luận: đạt mục tiêu P95 ≤ 500 ms và không có lỗi trong môi trường H2. Cần đo lại khi chạy MySQL/Docker và trên hạ tầng thật.

## 5. Truy vết NFR

| NFR | Cách kiểm chứng | Trạng thái |
|---|---|---|
| NFR-01 Hiệu năng | Load test 50 đồng thời, P95 346 ms | Đạt trên môi trường dev |
| NFR-02 Toàn vẹn dữ liệu | Transaction + khóa bi quan, unique `(sinh_vien_id, lop_hoc_phan_id)`, unit test sĩ số/trùng lịch | Đạt |
| NFR-03 Bảo mật | JWT, BCrypt, phân quyền theo vai trò, E2E kiểm tra chuyển hướng | Đạt |
| NFR-04 Kiểm tra đầu vào | Bean Validation ở DTO, xử lý lỗi tập trung | Đạt |
| NFR-05 Ghi log | SLF4J cho đăng ký/hủy/nhập điểm/lỗi hệ thống | Đạt |
| NFR-06 Bảo trì | Tách Controller–Service–Repository–DTO, 14 unit test | Đạt |
| NFR-07 Triển khai | Docker Compose, `docker compose config` hợp lệ | Đạt |
| NFR-08 Giao diện | Responsive, Chrome và Edge chạy E2E; Firefox/Safari cần kiểm thử thủ công | Một phần |

## 7. Tư liệu giao diện

Bộ ảnh giao diện phục vụ báo cáo/Figma nằm trong `docs/screenshots/`, tạo lại bằng:

```bash
cd frontend
npm run screenshots
```

Gồm: đăng nhập, dashboard sinh viên, đăng ký học phần, điểm & GPA, dashboard giảng viên, lớp phụ trách, dashboard quản trị, lớp học phần, modal thêm môn học.

## 8. Hạn chế và việc cần làm

- Firefox và Safari chưa chạy tự động; cần cài browser Playwright hoặc kiểm thử thủ công trước khi nộp.
- Load test mới đo endpoint danh sách lớp; nên bổ sung kịch bản đăng ký đồng thời để kiểm chứng khóa bi quan dưới tải.
- E2E dùng dữ liệu seed và có thao tác tạo/xóa/lock/unlock; các test đã được viết để chạy lại an toàn trên database mới.
