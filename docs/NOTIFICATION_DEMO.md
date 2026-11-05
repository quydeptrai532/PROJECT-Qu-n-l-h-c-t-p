# Trung tâm Thông báo (QLSV-51, QLSV-63 & QLSV-65)

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
   - Khi giảng viên tạo thông báo nghỉ buổi học (`POST /api/giang-vien/me/thong-bao-nghi`), hệ thống gửi thông báo cho các sinh viên đang đăng ký hiệu lực (`DA_DANG_KY`) của lớp học phần đó (`/thoi-khoa-bieu`).
5. **Thay đổi lịch học hoặc giảng viên phụ trách (`COURSE_SECTION_SCHEDULE_CHANGED`, `COURSE_SECTION_ASSIGNMENT_CHANGED`)**:
   - Chỉ phát khi lịch học (`phong`, `thu`, `tietBatDau`, `soTiet`) hoặc `giangVienId` thực sự thay đổi, gửi tới các sinh viên đang đăng ký (`DA_DANG_KY`) và giảng viên phụ trách.
6. **Nhắc hạn đóng đợt đăng ký (`REGISTRATION_WINDOW_CLOSING`)**:
   - Job định kỳ nhắc một lần cho mỗi sinh viên đang hoạt động khi đợt đăng ký còn mở và sắp kết thúc.
7. **Công bố điểm lớp học phần (`GRADES_PUBLISHED` — QLSV-65)**:
   - Quy trình bảng điểm trên `LopHocPhan`: `NHAP` (nháp) → `CHO_DUYET` (chờ duyệt) → `DA_DUYET` (đã phê duyệt) → `DA_CONG_BO` (đã công bố).
   - **Không** gửi thông báo khi giảng viên nhập/sửa điểm nháp (`NHAP`), gửi duyệt (`CHO_DUYET`) hoặc khi bảng điểm mới ở bước phê duyệt (`DA_DUYET`).
   - Chỉ phát thông báo nội bộ (`GRADES_PUBLISHED`, ưu tiên `HIGH`, `targetUrl = "/diem"`) khi bảng điểm chuyển hợp lệ sang `DA_CONG_BO` sau bước phê duyệt.
   - **Xác định người nhận trên server**: truy vấn các bản ghi `DangKyHocPhan` của lớp có `trangThai = DA_DANG_KY` (loại trừ sinh viên đã hủy đăng ký `DA_HUY`), có bản ghi `Diem` hợp lệ (`diemTongKet != null`) và tài khoản `User` đang hoạt động (`trangThai = true`). Không nhận danh sách người nhận từ client.
   - **Bảo mật dữ liệu điểm**: nội dung thông báo chỉ nêu mã lớp, mã/tên môn học, học kỳ/năm học và thời điểm công bố kèm liên kết nội bộ `/diem` (trang yêu cầu đăng nhập JWT và kiểm tra quyền `SINH_VIEN`). Tuyệt đối không đưa điểm cá nhân (`diemQuaTrinh`, `diemCuoiKy`, `diemTongKet`, GPA hay kết quả đạt/trượt) vào nội dung thông báo, URL, log ứng dụng hoặc chi tiết audit log.
   - **Thu hồi / điều chỉnh sau công bố**: khi thu hồi công bố (`POST /api/giang-vien/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem` hoặc `/api/admin/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem`), hệ thống ghi audit `GRADE_SHEET_PUBLICATION_REVOKED`, chuyển bảng điểm về `NHAP` và giữ nguyên `phienBanCongBo` để không tự phát lại thông báo công bố thông thường (`GRADES_PUBLISHED`) gây trùng lặp khi chưa có loại sự kiện nghiệp vụ riêng cho "công bố lại / điều chỉnh điểm sau công bố".

## Cơ chế chống trùng (Idempotency) & Xử lý lỗi giao dịch

- **Ràng buộc duy nhất**: bảng `thong_bao` có unique constraint `(recipient_user_id, idempotency_key)` kết hợp truy vấn batch `findExistingRecipientIdsByIdempotencyKey` (tránh N+1 query khi gửi theo lớp) và xử lý `DataIntegrityViolationException` an toàn khi có retry đồng thời.
- **Khóa idempotency tất định theo sự kiện**:
  - Đăng ký / hủy đăng ký / lớp bị hủy: `registration:{dangKyId}:{eventType}:{thoiGianDangKy}`.
  - Buổi học tạm nghỉ: `session-cancelled:{lopHocPhanId}:{ngayNghi}`.
  - Thay đổi lịch/giảng viên lớp học phần: `section-update:{lopHocPhanId}:{eventType}:{phong}:{thu}:{tietBatDau}:{soTiet}:{giangVienId}`.
  - Nhắc hạn đóng đăng ký: `registration-window-closing:{hocKyId}:{dangKyDen}`.
  - Công bố điểm lớp học phần: `grade-published:{lopHocPhanId}:v{phienBanCongBo}` (kết hợp `recipient_user_id` trong ràng buộc duy nhất `uk_thong_bao_recipient_event`).
- **Cách ly lỗi & khôi phục bền vững**:
  - Thông báo được phát ở pha `afterCommit` trong giao dịch mới (`PROPAGATION_REQUIRES_NEW`). Nếu giao dịch nghiệp vụ rollback thì không phát thông báo; nếu việc lưu thông báo gặp lỗi cơ sở dữ liệu tạm thời sau khi nghiệp vụ đã commit, lỗi được ghi log chẩn đoán và **không** làm rollback hay sai lệch trạng thái bảng điểm `DA_CONG_BO` đã commit.
  - Khi sinh viên truy vấn `/api/notifications` (hoặc đánh dấu tất cả đã đọc), `ThongBaoService.reconcileForUser` tự động đối soát cả `lich_su_dang_ky` và các lớp học phần đã công bố (`trangThaiBangDiem = DA_CONG_BO`, `congBoBangDiemLuc != null`) của sinh viên đó để tạo bù mọi thông báo còn thiếu theo đúng `idempotencyKey`.

## API liên quan

- `GET /api/notifications?unreadOnly=&page=&size=`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `GET /api/giang-vien/lop-hoc-phan/{id}/trang-thai-bang-diem`
- `POST /api/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem`
- `POST /api/giang-vien/lop-hoc-phan/{id}/phe-duyet-diem`
- `POST /api/giang-vien/lop-hoc-phan/{id}/cong-bo-diem`
- `POST /api/giang-vien/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem`
- `POST /api/admin/lop-hoc-phan/{id}/phe-duyet-diem`
- `POST /api/admin/lop-hoc-phan/{id}/cong-bo-diem`
- `POST /api/admin/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem`
