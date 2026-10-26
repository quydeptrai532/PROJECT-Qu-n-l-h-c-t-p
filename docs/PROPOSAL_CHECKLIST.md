# Góp ý bổ sung báo cáo đề cương

Báo cáo hiện tại đủ để chia việc và chốt phạm vi, nhưng nếu dùng để nộp/ bảo vệ thì nên bổ sung các mục dưới đây.

## 1. Phần nên bổ sung

1. **Thiết kế giao diện & UX**
   - Sơ đồ luồng màn hình theo 3 vai trò (đăng nhập → dashboard → chức năng).
   - Design system: màu chủ đạo, typography, button, input, badge, modal, bảng.
   - Danh sách màn hình chính kèm ảnh chụp: đăng nhập, dashboard sinh viên, đăng ký, thời khóa biểu, điểm, dashboard giảng viên, nhập điểm, dashboard quản trị, danh mục, lớp học phần, người dùng.
   - Quy ước responsive (desktop/tablet/mobile) và trạng thái loading/empty/error.
2. **Mô hình dữ liệu & API**
   - ERD đầy đủ quan hệ bảng, khóa ngoại, unique constraint.
   - API contract: method, path, request/response mẫu, mã lỗi 400/401/403/404/409.
   - Sơ đồ tuần tự cho: đăng nhập JWT, đăng ký học phần (transaction + khóa bi quan), nhập điểm, khóa tài khoản.
3. **Tiêu chí nghiệm thu & test**
   - Mỗi FR có acceptance criteria rõ ràng (input, điều kiện, output, ngoại lệ).
   - Bảng test case tham chiếu `docs/TEST_PLAN.md` (unit, E2E, load test).
   - Kết quả thực tế: 14 unit test, 11 E2E trên Chrome và Edge, load test 50 đồng thời P95 ~332ms.
4. **CI/CD & triển khai**
   - Sơ đồ pipeline GitHub Actions: build/test → E2E → build & push image GHCR.
   - Sơ đồ triển khai Docker Compose (frontend Nginx, backend, MySQL, volume).
   - Danh sách biến môi trường, quy trình rollback, health check `/api/health`.
5. **Phi chức năng**
   - Chốt lại mục tiêu hiệu năng (đề cương ghi 50 request, SRS ghi 200 người dùng/500ms).
   - Bảo mật: JWT, BCrypt, CORS, phân quyền, cache trạng thái tài khoản.
   - Logging/monitoring, sao lưu dữ liệu, thời gian giữ log.
   - Tương thích trình duyệt: Chrome/Edge đã tự động; Firefox/Safari cần kiểm thử thủ công.
6. **Quản trị dự án**
   - Risk register (rủi ro tiến độ, phụ thuộc T22/T27–T29/T10–T13, thiếu thiết bị/trình duyệt).
   - Assumptions & out-of-scope.
   - Definition of Done cho từng task.
   - Bảng đóng góp kèm commit/PR và minh chứng Trello.
   - Cập nhật trạng thái task theo ngày, không chỉ kế hoạch.
7. **Phụ lục**
   - Ảnh chụp giao diện, test report Playwright, log load test, CI badge, link repo/board thật.

## 2. Điểm chưa nhất quán cần sửa

- Link GitHub/Trello/Figma vẫn là placeholder; cần thay bằng URL thật và phân quyền cho giảng viên.
- Hạn nộp ghi 23/10/2026 nhưng file lưu ngày 26/10/2026; kiểm tra lại mốc thời gian.
- Đề cương không nhắc nhập điểm/xem điểm, thời khóa biểu, nhập sinh viên CSV — trong khi SRS và sản phẩm có các phần này.
- Đề cương yêu cầu test tải 50 request, SRS ghi 200 người dùng đồng thời; cần chốt một con số.
- Quy tắc tín chỉ tối thiểu cần nêu rõ: chặn khi hủy nếu tổng còn lại nằm giữa 0 và mức tối thiểu; cho phép hủy toàn bộ.
- Thông tin lớp `HN-KS24-CNTT1 (Ví dụ)`, chỉ có MSV của nhóm trưởng; nên bổ sung đầy đủ MSV và phân công.
- Chưa có tiêu chí "hoàn thành" cho từng task (code + test + review + demo).

## 3. Dùng giao diện hiện tại làm Figma

1. Chạy app và chụp ảnh các màn hình chính (full page, 1440px). Bộ ảnh mẫu đã có trong `docs/screenshots/` (tạo lại bằng `cd frontend && npm run screenshots`).
2. Tạo frame Figma theo từng nhóm: Auth, Student, Lecturer, Admin, Components.
3. Ghi lại token: màu indigo/violet, font Be Vietnam Pro, bo góc 12–16px, shadow, badge.
4. Tách component: Sidebar, Header, PageHeader, StatCard, DataTable, Modal, ConfirmDialog, ChartCard.
5. Dùng ảnh chụp làm reference, vẽ lại component để có file thiết kế có thể chỉnh sửa.

## 4. Cấu trúc báo cáo cuối gợi ý

1. Giới thiệu & mục tiêu
2. Phân tích yêu cầu (FR/NFR)
3. Thiết kế hệ thống (kiến trúc, ERD, API, sequence)
4. Thiết kế giao diện & UX
5. Triển khai & CI/CD
6. Kiểm thử (unit, E2E, load) & kết quả
7. Đánh giá, hạn chế, hướng phát triển
8. Phụ lục (ảnh, link, bảng phân công)
