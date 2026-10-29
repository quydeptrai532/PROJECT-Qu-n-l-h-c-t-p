# Demo nhật ký kiểm toán

## Truy cập

Đăng nhập bằng tài khoản có vai trò quản trị, mở **Quản trị → Nhật ký hoạt động** (`/quan-tri/nhat-ky-hoat-dong`). Trang chỉ hiển thị dữ liệu đọc từ API quản trị đã phân quyền. Bộ lọc hỗ trợ khoảng ngày, tên đăng nhập, hành động, loại đối tượng và ID; kết quả mới nhất đứng trước và được phân trang.

## Kịch bản demo

1. Tạo, sửa hoặc xóa môn học trong danh mục môn. Nhật ký ghi `COURSE_CREATED`, `COURSE_UPDATED` hoặc `COURSE_DELETED`; thay đổi môn tiên quyết ghi `PREREQUISITE_ADDED`/`PREREQUISITE_REMOVED` cùng ID các môn liên quan. Tạo hoặc sửa lớp học phần trong phần quản lý lớp ghi `COURSE_SECTION_CREATED` hoặc `COURSE_SECTION_UPDATED` cùng ID lớp và các trường lịch/phòng/sĩ số liên quan.
2. Thay đổi thời gian mở/đóng đăng ký hoặc giới hạn tín chỉ của học kỳ. Lọc `REGISTRATION_WINDOW_UPDATED` để xem giá trị trước và sau.
3. Khóa hoặc mở khóa tài khoản sinh viên/giảng viên. Lọc theo người thao tác và loại đối tượng; chi tiết chỉ có trạng thái hoạt động trước/sau.
4. Đăng ký hoặc hủy đăng ký lớp học phần. Sự kiện có kết quả, ID đăng ký/lớp và sĩ số trước/sau. Thử đăng ký lớp không hợp lệ để xem mã kết quả thất bại.
5. Lọc theo ngày và ID đối tượng, chuyển trang để kiểm tra truy vấn.

Các sự kiện ghi nhận gồm tạo/cập nhật/xóa môn học và lớp học phần; thêm/xóa môn tiên quyết; tạo/cập nhật/xóa cấu hình học kỳ có lịch đăng ký; tạo/cập nhật/khóa/mở khóa tài khoản sinh viên/giảng viên; đăng ký và hủy đăng ký học phần. Khoa, ngành, lớp hành chính chưa được ghi audit; ứng dụng hiện không có luồng quản trị đổi quyền/vai trò riêng. Thông tin hồ sơ cá nhân, mật khẩu, email và số điện thoại không được đưa vào chi tiết audit. Thao tác chỉ đọc, đăng nhập, xem Course Planner và thao tác chưa có trong danh sách trên hiện chưa phát sinh audit event.

## Tính toàn vẹn và riêng tư

Máy chủ tự xác định actor từ phiên JWT đã xác thực và ghi sự kiện trong cùng luồng nghiệp vụ. Sự kiện thành công dùng chung transaction với thay đổi dữ liệu; lỗi ghi audit làm transaction nghiệp vụ thất bại. Lỗi nghiệp vụ được ghi bằng transaction độc lập để giữ dấu vết thất bại. Chi tiết chỉ lấy từ allowlist trường nghiệp vụ; không lưu mật khẩu, token, email hoặc nội dung lỗi thô. Chỉ vai trò quản trị được đọc; không có API sửa/xóa log.

Schema tạo bảng `audit_log` và các index theo cấu hình Hibernate `ddl-auto:update` của ứng dụng. Nếu môi trường triển khai quản lý schema bằng migration riêng, cần tạo bảng và các index tương ứng trước khi bật phiên bản này.
