# Lịch sử đăng ký học phần (QLSV-59)

Sinh viên tra cứu lịch sử theo học kỳ tại `/lich-su-dang-ky`. Trang chỉ gọi API của tài khoản đang đăng nhập và hiển thị từng lần chuyển trạng thái, kể cả lần đăng ký sau khi đã hủy.

## API

`GET /api/dang-ky/me/lich-su?hocKyId={id}&page=0&size=20`

- Chỉ cho vai trò `SINH_VIEN`; `sinhVienId` được lấy từ hồ sơ gắn với người dùng hiện tại, không nhận từ query string.
- `hocKyId` bắt buộc và phải tồn tại. `page` bắt đầu từ 0; `size` trong khoảng 1–100.
- Trả về cấu trúc phân trang Spring (`content`, `totalElements`, `totalPages`, `number`, `size`). Sự kiện sắp theo `thoiDiem` giảm dần, rồi `id` giảm dần để thứ tự không đổi khi nhiều sự kiện trùng thời gian.
- Mỗi mục gồm môn, mã lớp, thời điểm đăng ký, trạng thái của sự kiện, thời điểm sự kiện, thời điểm hủy và lý do nếu có. Một lần hủy là một sự kiện riêng; đăng ký lại không ghi đè sự kiện trước.

`DELETE /api/dang-ky/{id}?lyDo={text}` chấp nhận lý do tùy chọn tối đa 255 ký tự. Lý do để trống được lưu là null.

## Lưu trữ và audit

Bảng `lich_su_dang_ky` lưu các lần chuyển trạng thái để truy vấn theo sinh viên và học kỳ, có chỉ mục phục vụ sắp xếp/phân trang. Khi sinh viên tra cứu lần đầu, các đăng ký hiện hữu chưa có event được nạp snapshot một lần; vì dữ liệu cũ không lưu giờ hủy/lý do hủy riêng, hai trường đó để trống cho snapshot cũ. Bản ghi audit hiện hữu tiếp tục ghi thao tác đăng ký/hủy cho mục đích kiểm toán; API lịch sử dùng bảng sự kiện có cấu trúc thay vì phân tích JSON audit ở từng request. Hai bản ghi phục vụ hai nhu cầu khác nhau.

Với môi trường chạy `JPA_DDL_AUTO=update`, Hibernate tạo bảng và chỉ mục mới khi khởi động. Khi dùng `validate` hoặc schema quản lý ngoài ứng dụng, cần triển khai schema tương ứng trước khi phát hành.
