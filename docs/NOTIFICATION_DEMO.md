# Demo trung tâm thông báo (QLSV-51)

## Sự kiện và người nhận

| Sự kiện từ luồng hiện có | Người nhận | Nội dung/đường dẫn |
|---|---|---|
| Đăng ký hoặc hủy lớp thành công | Sinh viên thực hiện và giảng viên phụ trách lớp | Xác nhận lớp/môn/học kỳ; giảng viên chỉ nhận mô tả chung, không có tên hay mã sinh viên |
| Đổi phòng, thứ/tiết hoặc giảng viên phụ trách lớp | Sinh viên đang đăng ký lớp và giảng viên đang phụ trách sau cập nhật | Thông tin lịch/phân công mới; mở thời khóa biểu |
| Đợt đăng ký còn hôm nay hoặc ngày mai | Tất cả tài khoản sinh viên đang hoạt động | Tên học kỳ, ngày kết thúc, ưu tiên cao; mở trang đăng ký |

Sinh viên và giảng viên có đường dẫn **Thông báo** trên menu. Trang hiển thị tất cả/chưa đọc, trạng thái đọc, thời gian, ưu tiên, mở nội dung, đánh dấu một hoặc tất cả đã đọc và phân trang. Quản trị viên không nhận các sự kiện trên và không được truy cập API trung tâm thông báo.

## Tính riêng tư và idempotency

Người nhận được xác định từ hồ sơ server và lớp/học kỳ trong transaction; không có API tạo thông báo hoặc cho client gửi người nhận/nội dung. Mỗi request đọc/trạng thái được giới hạn theo `recipient_user_id`; ID không thuộc người đăng nhập trả `404`. Nội dung chỉ chứa lớp, môn, học kỳ và lịch cần thiết. Không có tên/mã/email/số điện thoại sinh viên, mật khẩu hay token.

Thông báo đăng ký được tạo trong cùng transaction với đăng ký/hủy; khóa lớp hiện có tuần tự hóa các thao tác cạnh tranh và khóa duy nhất `(recipient_user_id, idempotency_key)` ngăn cùng khóa sự kiện lưu lặp. Cập nhật lớp chỉ phát thông báo nếu lịch hoặc giảng viên thực sự thay đổi. Nhắc hạn dùng khóa học kỳ khi phát sinh và khóa duy nhất theo người nhận + học kỳ + ngày kết thúc, nên chạy lặp hoặc nhiều scheduler không tạo nhắc trùng.

## Nhắc hạn đăng ký

Job chạy mỗi giờ theo `Asia/Ho_Chi_Minh`, chỉ nhắc học kỳ đã mở đăng ký và kết thúc trong hôm nay hoặc ngày mai. Mỗi học kỳ/ngày kết thúc tạo tối đa một thông báo cho mỗi sinh viên đang hoạt động. Nếu chỉnh ngày kết thúc, khóa sự kiện mới dùng ngày kết thúc mới.

## Schema

Entity `ThongBao` tạo bảng `thong_bao`, index theo người nhận/thời gian và trạng thái đọc, cùng unique constraint cho idempotency. Repo sử dụng Hibernate `ddl-auto:update`; nếu môi trường production quản lý schema bằng migration riêng, cần tạo bảng/cột/index/unique constraint tương ứng trước khi triển khai.
