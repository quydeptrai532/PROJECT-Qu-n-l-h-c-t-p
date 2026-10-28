# Demo Course Planner (QLSV-49)

## Chuẩn bị

Chạy backend có dữ liệu mẫu và frontend, sau đó đăng nhập bằng `sv02 / 123456` và mở **Kế hoạch học tập** (`/ke-hoach-hoc-tap`). Giỏ được lưu trong trình duyệt theo tài khoản và học kỳ đang mở; thêm hoặc bỏ lớp không tạo đăng ký chính thức.

## Kịch bản giao diện

1. Tìm lớp bằng mã và thêm vào giỏ. Course Planner báo các cặp trùng lịch (cả hai lớp, thứ và khoảng tiết) và trùng môn; xác nhận bị vô hiệu hóa cho đến khi xử lý điều kiện. Fixture seed hiện tại không có cặp lớp cùng ngày/tiết bị trùng, vì vậy E2E tạo xung đột bằng cách điều chỉnh lịch trả về của `INT301.01` thành Thứ 2 để kiểm tra giao diện mà không sửa dữ liệu seed.
2. Bỏ lớp khỏi giỏ, tải lại trang để xem trạng thái giỏ được khôi phục. Lịch dự kiến và tổng tín chỉ thay đổi ngay khi thêm/bỏ.
3. Thêm `INT301.01` và `INT302.01`, là hai lớp không giao lịch. Chọn **Xác nhận đăng ký** rồi xác nhận trong hộp thoại. Backend kiểm tra lại điều kiện tiên quyết; với tài khoản `sv02`, đăng ký bị từ chối do thiếu `INT201`, không lớp nào được ghi danh và giỏ vẫn còn để chỉnh sửa.

> Dữ liệu seed hiện có thứ, tiết bắt đầu, số tiết và phòng. Giao diện trình bày đúng các trường đó dưới dạng lịch tuần; repo chưa có giờ đồng hồ hoặc tuần luân phiên, nên không suy diễn các thông tin này.

## Kiểm chứng ghi danh thành công và rollback

`ConcurrentRegistrationIntegrationTest` tạo fixture cô lập với lớp hợp lệ và gọi endpoint xác nhận giỏ qua HTTP. Các test kiểm tra đăng ký thành công, cùng môn, thiếu tiên quyết và hết chỗ; khi một lớp trong giỏ không hợp lệ, transaction rollback toàn bộ đăng ký của giỏ. Chạy cùng backend suite bằng `cd backend && ./mvnw test`.
