# QLSV-68 / TASK-LC-03: Gửi thông báo cho lớp học phần của Giảng viên

## 1. Mục tiêu và bối cảnh nghiệp vụ

Tính năng cho phép Giảng viên (`GIANG_VIEN`) trực tiếp tạo và phát thông báo học vụ tới toàn thể sinh viên đang theo học trong các lớp học phần mà giảng viên đó được phân công phụ trách.

### Nguyên tắc nghiệp vụ cốt lõi
1. **Kiểm soát phân công trên máy chủ**:
   - Backend xác thực danh tính giảng viên từ Security Context (`CurrentUser`), truy vấn quan hệ giảng viên – lớp học phần.
   - Tuyệt đối không tin cậy `teacherId`, `classId` hoặc danh sách người nhận gửi từ phía client.
   - Giảng viên chỉ được gửi thông báo cho các lớp do mình trực tiếp phụ trách. Truy cập vào lớp của giảng viên khác trả về `403 Forbidden`. Lớp không tồn tại trả về `404 Not Found`.
2. **Xác định người nhận hợp lệ**:
   - Người nhận được xác định tự động bởi backend thông qua truy vấn `DangKyHocPhan` với điều kiện `trangThai == DA_DANG_KY` (đang đăng ký hiệu lực) và tài khoản `User` của sinh viên đang hoạt động (`trangThai == true`).
   - Sinh viên đã hủy đăng ký (`DA_HUY`) hoặc tài khoản bị khóa không nhận được thông báo.
3. **Bảo mật và phân quyền Notification Center**:
   - Thông báo được lưu vào bảng `thong_bao` liên kết với `recipient_user_id` của từng sinh viên đủ điều kiện.
   - Endpoint `GET /api/notifications` chỉ truy xuất thông báo thuộc về chính `currentUser` từ token JWT, đảm bảo người ngoài hoặc sinh viên lớp khác không thể đọc được.
4. **Chống trùng lặp (Idempotency) & Retry an toàn**:
   - Bảng `thong_bao_lop_hoc_phan` có ràng buộc duy nhất `uk_thong_bao_lop_idempotency` trên cặp `(lop_hoc_phan_id, idempotency_key)`.
   - Bảng `thong_bao` có ràng buộc duy nhất `uk_thong_bao_recipient_event` trên cặp `(recipient_user_id, idempotency_key)`.
   - Khi client retry (hoặc gửi lại cùng `idempotencyKey`), backend phát hiện bản ghi đã tồn tại và trả về kết quả thành công mà không phát sinh thêm thông báo trùng lặp.
5. **Cách ly giao dịch & khôi phục bền vững**:
   - Phát thông báo nội bộ được thực hiện ở pha `afterCommit` trong transaction mới (`PROPAGATION_REQUIRES_NEW`). Nếu nghiệp vụ tạo thông báo rollback thì không gửi thông báo; nếu việc lưu thông báo con gặp lỗi kết nối cơ sở dữ liệu tạm thời, giao dịch nghiệp vụ đã commit không bị rollback.
6. **Giới hạn chức năng theo năng lực hiện có**:
   - **Chỉ triển khai gửi ngay (Immediate Send)**.
   - Do dự án hiện chưa có cơ chế hạ tầng scheduler nền cho thông báo lớp hay chính sách nghiệp vụ rõ ràng về việc thu hồi/chỉnh sửa/xóa thông báo đã gửi cho sinh viên, hệ thống **không tự ý xây scheduler hoặc chính sách chỉnh sửa/xóa** ngoài kiến trúc sẵn có.
7. **Lưu người tạo, thời điểm và ghi Audit Log**:
   - Bản ghi `ThongBaoLopHocPhan` lưu `giangVien` (người tạo) và `thoiGianTao`.
   - Ghi Audit Log với hành động `CLASS_ANNOUNCEMENT_CREATED`, đối tượng `COURSE_SECTION`, kèm chi tiết `maLop`, `soNguoiNhan`, `thongBaoLopId`, `tieuDe`.

---

## 2. Danh sách Endpoint API

| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `POST` | `/api/giang-vien/lop-hoc-phan/{id}/thong-bao` | `GIANG_VIEN` | Tạo và gửi thông báo ngay tới sinh viên đang đăng ký hiệu lực của lớp phụ trách. Hỗ trợ header `Idempotency-Key` / body `idempotencyKey`. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{id}/thong-bao` | `GIANG_VIEN` | Xem lịch sử thông báo đã gửi của lớp học phần phụ trách. |
| `GET` | `/api/giang-vien/me/thong-bao-lop` | `GIANG_VIEN` | Danh sách thông báo lớp đã gửi bởi giảng viên hiện tại (hỗ trợ query param `lopHocPhanId`). |
| `GET` | `/api/sinh-vien/lop-hoc-phan/{id}/thong-bao` | `SINH_VIEN` | Xem danh sách thông báo của lớp học phần mà sinh viên đang đăng ký học (`DA_DANG_KY`). Trả về 403 nếu sinh viên không học lớp này. |
| `GET` | `/api/notifications` | `SINH_VIEN`, `GIANG_VIEN` | Trung tâm thông báo nội bộ nhận diện sự kiện `CLASS_ANNOUNCEMENT`. |

---

## 3. Giao diện người dùng

- **`ClassAnnouncementModal`**:
  - Modal chuyên dụng tích hợp tại trang Danh sách lớp (`/lop-cua-toi`) và trang Nhập điểm lớp (`/lop-cua-toi/:id/nhap-diem`).
  - **Tab Soạn thông báo**:
    - Nhập tiêu đề (giới hạn 120 ký tự, có bộ đếm ký tự).
    - Nhập nội dung (giới hạn 1000 ký tự, có bộ đếm ký tự).
    - Hiển thị chính sách gửi ngay và cảnh báo phạm vi người nhận.
    - Validate dữ liệu phía client trước khi gửi.
    - Xử lý trạng thái loading ("Đang gửi thông báo..."), vô hiệu hóa nút bấm tránh double-click.
    - Thông báo phản hồi kết quả: hiển thị rõ số lượng sinh viên đã nhận thông báo.
  - **Tab Lịch sử đã gửi**:
    - Danh sách các thông báo đã gửi cho lớp với đầy đủ thông tin: tiêu đề, nội dung, thời điểm gửi, số lượng người nhận và người gửi.
    - Trạng thái loading (`Spinner`), lỗi (`Alert tone="error"`), và trạng thái rỗng (`EmptyState`).
- **Trung tâm thông báo (`NotificationsPage`)**:
  - Hỗ trợ hiển thị nhãn sự kiện `CLASS_ANNOUNCEMENT` ("Thông báo lớp học phần").
  - Click vào thông báo đánh dấu đã đọc và điều hướng tới `/hoc-phan-cua-toi`.
