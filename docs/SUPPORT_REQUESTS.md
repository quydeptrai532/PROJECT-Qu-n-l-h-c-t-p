# Yêu cầu hỗ trợ / khiếu nại sinh viên (QLSV-64 — TASK-ST-08)

## 1. Tổng quan chức năng

- **Sinh viên (`SINH_VIEN`)**:
  - Tạo phiếu hỗ trợ/khiếu nại với loại yêu cầu thuộc danh mục hợp lệ, tiêu đề (5–200 ký tự), nội dung gốc (10–4000 ký tự).
  - Tùy chọn liên kết đối tượng học vụ: môn học (`monHocId`), học kỳ (`hocKyId`) hoặc bản ghi điểm của chính mình (`diemId`).
  - Tùy chọn đính kèm tệp minh chứng theo giới hạn cấu hình.
  - Xem danh sách phiếu của chính mình (có lọc theo trạng thái, loại yêu cầu, từ khóa và phân trang), xem chi tiết phiếu, lịch sử chuyển trạng thái và trao đổi phản hồi.
  - Nội dung gốc (`tieuDe`, `noiDungGoc`) được khóa sau khi gửi; các phản hồi tiếp theo được lưu tách biệt tại bảng `yeu_cau_ho_tro_trao_doi`.
- **Quản trị viên (`QUAN_TRI`)**:
  - Tra cứu, lọc và phân trang danh sách toàn bộ phiếu hỗ trợ.
  - Xem chi tiết phiếu, tải xuống tệp minh chứng, gửi phản hồi trao đổi và cập nhật trạng thái xử lý theo luồng hợp lệ (ghi nhận người thực hiện, vai trò, thời điểm và ghi chú vào `yeu_cau_ho_tro_lich_su_trang_thai`).

## 2. Kiểm soát truy cập & Bảo mật tệp đính kèm

1. **Xác thực người tạo từ Principal**:
   - Không nhận `studentId`, `sinhVienId`, `nguoiTaoId`, `nguoiXuLyId` từ phía client; luôn truy xuất tài khoản từ `CurrentUser` (`SecurityContextHolder`).
2. **Chống IDOR**:
   - Sinh viên chỉ được xem chi tiết, thêm trao đổi, tải tệp đính kèm trên phiếu của chính mình và chỉ được gắn `diemId` thuộc kết quả học tập của chính mình. Mọi truy cập sang ID của sinh viên khác bị từ chối với `403 Forbidden`.
3. **Bảo mật tệp minh chứng (`SupportAttachmentStorageService`)**:
   - Kiểm tra phần mở rộng hợp lệ, chặn tên tệp chứa ký tự đường dẫn (`..`, `/`, `\`, `:`, ký tự điều khiển) và chặn phần mở rộng kép chứa định dạng thực thi/kịch bản (`.exe`, `.php`, `.jsp`, `.sh`, `.html`, `.svg`, v.v.).
   - Kiểm tra chữ ký nhị phân (magic bytes) thực tế của tệp (`%PDF-` cho PDF, `89 50 4E 47...` cho PNG, `FF D8 FF` cho JPEG) và đối chiếu khớp với phần mở rộng tệp; không tin `Content-Type` do client gửi.
   - Tên lưu trữ trên đĩa do server sinh bằng `UUID`, lưu tại thư mục riêng (`app.support-request.storage-dir`) ngoài thư mục public, chỉ phục vụ qua API có kiểm tra quyền với header `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'; sandbox` và `Cache-Control: no-store`.
   - Tự động xóa tệp mồ côi trên đĩa nếu giao dịch cơ sở dữ liệu thất bại hoặc rollback.

## 3. Cấu hình tập trung cần chủ dự án xác nhận

Do mã nguồn hiện có trước đó chưa quy định danh mục loại yêu cầu, trạng thái xử lý, giới hạn tệp và SLA chính thức, hệ thống gom cấu hình tập trung tại `app.support-request` (`SupportRequestProperties` / `application.yml`) với các giá trị mặc định an toàn tạm thời:

| Cấu hình | Biến môi trường | Giá trị mặc định tạm thời | Trạng thái xác nhận |
|---|---|---|---|
| `app.support-request.allowed-types` | `SUPPORT_ALLOWED_TYPES` | `HOC_PHAN,DIEM_SO,HOC_KY,HO_SO_HOC_VU,KHAC` | Cần chủ dự án xác nhận danh mục chính thức |
| `app.support-request.allowed-statuses` | `SUPPORT_ALLOWED_STATUSES` | `MOI_TAO,DANG_XU_LY,CHO_PHAN_HOI_SINH_VIEN,DA_GIAI_QUYET,TU_CHOI,DA_DONG` | Cần chủ dự án xác nhận luồng trạng thái chính thức |
| `app.support-request.comment-allowed-statuses` | `SUPPORT_COMMENT_ALLOWED_STATUSES` | `MOI_TAO,DANG_XU_LY,CHO_PHAN_HOI_SINH_VIEN,DA_GIAI_QUYET` | Cần chủ dự án xác nhận chính sách đóng phản hồi |
| `app.support-request.max-file-size-bytes` | `SUPPORT_MAX_FILE_SIZE_BYTES` | `5242880` (5 MB / tệp) | Cần chủ dự án xác nhận giới hạn dung lượng |
| `app.support-request.max-files-per-request` | `SUPPORT_MAX_FILES_PER_REQUEST` | `3` tệp / phiếu | Cần chủ dự án xác nhận số lượng tệp tối đa |
| `app.support-request.allowed-extensions` | `SUPPORT_ALLOWED_EXTENSIONS` | `pdf,png,jpg,jpeg` | Cần chủ dự án xác nhận loại tệp minh chứng |
| `app.support-request.sla-note` | `SUPPORT_SLA_NOTE` | *(Để trống — không tuyên bố SLA chính thức)* | Cần chủ dự án quyết định SLA xử lý nếu áp dụng |
