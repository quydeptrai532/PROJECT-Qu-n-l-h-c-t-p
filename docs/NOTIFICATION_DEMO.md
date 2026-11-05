# Trung tâm Thông báo (QLSV-51 & QLSV-63)

## Chức năng

- Thông báo trong ứng dụng cho tài khoản `SINH_VIEN` và `GIANG_VIEN` qua `/thong-bao` và biểu tượng chuông ở thanh bên.
- Mọi thao tác đọc/đánh dấu đã đọc lấy người nhận từ JWT trên máy chủ; không nhận `userId` từ client và không cho phép xem/cập nhật thông báo của tài khoản khác.
- Mỗi thông báo chứa môn học/lớp học phần liên quan, thời điểm xảy ra, kết quả/trạng thái và bước tiếp theo kèm `targetUrl` điều hướng nội bộ.

## Các sự kiện hỗ trợ

1. **Đăng ký học phần thành công (`COURSE_REGISTRATION_CONFIRMED`)**:
   - Người nhận: sinh viên vừa đăng ký thành công (`/hoc-phan-cua-toi`) và giảng viên phụ trách lớp (`/lop-cua-toi`).
   - Trường hợp đăng ký thất bại (lớp đầy, trùng lịch, trùng môn, thiếu tiên quyết, ngoài thời gian đăng ký): hệ thống hiện xử lý như lỗi request (`ApiException`) và rollback giao dịch, **không** mô hình hóa thất bại thành bản ghi sự kiện nghiệp vụ bền vững nên **không** sinh thông báo "đăng ký thất bại" từ exception tùy tiện.
2. **Hủy học phần thành công (`COURSE_REGISTRATION_CANCELLED`)**:
   - Người nhận: sinh viên vừa hủy đăng ký (`/thoi-khoa-bieu`, kèm lý do hủy nếu có) và giảng viên phụ trách lớp (`/lop-cua-toi`).
3. **Lớp học phần bị hủy & thay đổi trạng thái đăng ký (`COURSE_SECTION_CANCELLED`)**:
   - Khi quản trị viên hủy lớp học phần (`POST /api/admin/lop-hoc-phan/{id}/huy`), tất cả đăng ký đang hiệu lực (`DA_DANG_KY`) tại thời điểm sự kiện được chuyển sang `DA_HUY`, ghi nhận vào `lich_su_dang_ky` và gửi thông báo ưu tiên cao (`HIGH`, điều hướng `/dang-ky`) **chỉ** cho các sinh viên đang `DA_DANG_KY` tại thời điểm hủy (cùng giảng viên phụ trách). Sinh viên đã tự hủy trước đó không nhận thông báo này.
4. **Buổi học tạm nghỉ (`COURSE_SESSION_CANCELLED`)**:
   - Khi giảng viên tạo thông báo nghỉ buổi học (`POST /api/giang-vien/thong-bao-nghi`), hệ thống gửi thông báo cho các sinh viên đang đăng ký hiệu lực (`DA_DANG_KY`) của lớp học phần đó (`/thoi-khoa-bieu`).
5. **Thay đổi lịch học hoặc giảng viên phụ trách (`COURSE_SECTION_SCHEDULE_CHANGED`, `COURSE_SECTION_ASSIGNMENT_CHANGED`)**:
   - Chỉ phát khi lịch học (`phong`, `thu`, `tietBatDau`, `soTiet`) hoặc `giangVienId` thực sự thay đổi, gửi tới các sinh viên đang đăng ký (`DA_DANG_KY`) và giảng viên phụ trách.
6. **Nhắc hạn đóng đợt đăng ký (`REGISTRATION_WINDOW_CLOSING`)**:
   - Job định kỳ nhắc một lần cho mỗi sinh viên đang hoạt động khi đợt đăng ký còn mở và sắp kết thúc.

## Cơ chế chống trùng (Idempotency) & Xử lý lỗi giao dịch

- **Ràng buộc duy nhất**: bảng `thong_bao` có unique constraint `(recipient_user_id, idempotency_key)` kết hợp kiểm tra `existsByRecipientIdAndIdempotencyKey` và xử lý `DataIntegrityViolationException` an toàn khi có retry đồng thời.
- **Khóa idempotency tất định theo sự kiện**:
  - Đăng ký / hủy đăng ký / lớp bị hủy: `registration:{dangKyId}:{eventType}:{thoiGianDangKy}` (mỗi chu kỳ đăng ký có `thoiGianDangKy` riêng nên đăng ký lại sau khi hủy vẫn nhận thông báo mới, nhưng retry cùng một thao tác không tạo bản ghi trùng).
  - Buổi học tạm nghỉ: `session-cancelled:{lopHocPhanId}:{ngayNghi}`.
  - Thay đổi lịch/giảng viên lớp học phần: `section-update:{lopHocPhanId}:{eventType}:{phong}:{thu}:{tietBatDau}:{soTiet}:{giangVienId}`.
  - Nhắc hạn đóng đăng ký: `registration-window-closing:{hocKyId}:{dangKyDen}`.
- **Cách ly lỗi & khôi phục bền vững**:
  - Thông báo đăng ký/hủy được phát ở pha `afterCommit` trong giao dịch mới (`PROPAGATION_REQUIRES_NEW`). Nếu giao dịch nghiệp vụ rollback thì không phát thông báo; nếu việc lưu thông báo gặp lỗi cơ sở dữ liệu tạm thời sau khi nghiệp vụ đã commit, lỗi được ghi log chẩn đoán (không chứa token/mật khẩu/dữ liệu nhạy cảm) và **không** làm rollback hay sai lệch trạng thái đăng ký đã commit.
  - Bảng `lich_su_dang_ky` đóng vai trò bản ghi sự kiện bền vững trong cùng giao dịch đăng ký/hủy. Khi sinh viên truy vấn `/api/notifications` (hoặc đánh dấu đã đọc), `ThongBaoService.reconcileForUser` tự động đối soát `lich_su_dang_ky` và tạo bù mọi thông báo còn thiếu theo đúng `idempotencyKey`, đảm bảo thông báo không bị mất vĩnh viễn.

## API

- `GET /api/notifications?unreadOnly=&page=&size=`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `POST /api/admin/lop-hoc-phan/{id}/huy`

## Cách kiểm thử nhanh

1. Chạy backend profile `h2` và frontend Vite.
2. Đăng nhập `sv01` / `123456`, vào `/dang-ky`, đăng ký một lớp đang mở.
3. Mở `/thong-bao`, kiểm tra thông báo chưa đọc, lọc `Chưa đọc`, bấm `Mở nội dung` hoặc `Đánh dấu đã đọc`.
