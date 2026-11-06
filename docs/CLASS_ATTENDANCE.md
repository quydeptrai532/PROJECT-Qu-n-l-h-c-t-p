# QLSV-70 / TASK-LC-05: Điểm danh buổi học và báo cáo chuyên cần

## 1. Mục tiêu và bối cảnh nghiệp vụ

Tính năng cho phép Giảng viên (`GIANG_VIEN`) phụ trách lớp học phần tạo các buổi học gắn với ngày và ca học, thực hiện điểm danh từng sinh viên với các trạng thái chuẩn, chỉnh sửa trước khi khóa buổi và theo dõi báo cáo chuyên cần của cả lớp. Đồng thời, sinh viên có thể theo dõi tỷ lệ chuyên cần cá nhân và Quản trị viên (`QUAN_TRI`) giữ quyền đặc biệt xử lý các buổi học đã khóa.

---

## 2. Nguyên tắc nghiệp vụ cốt lõi

1. **Kiểm soát phân công giảng viên trên máy chủ**:
   - Backend xác thực giảng viên phụ trách từ Security Context (`CurrentUser`) và quan hệ `LopHocPhan.giangVien`.
   - Giảng viên chỉ được tạo buổi học, điểm danh, sửa buổi và xem báo cáo cho lớp mình phụ trách. Truy cập lớp của giảng viên khác trả về `403 Forbidden`.
   - Quản trị viên (`QUAN_TRI`) có quyền truy cập toàn diện và là vai trò duy nhất có quyền đặc biệt sửa hoặc mở khóa các buổi học đã bị khóa.

2. **Cấu trúc buổi học và ngăn chặn trùng lặp**:
   - Mỗi buổi học (`BuoiHoc`) gắn liền với lớp học phần (`lopHocPhan`), ngày học (`ngayHoc`), ca học (`caHoc`), tiết học và phòng học.
   - Ngày học được kiểm tra nghiêm ngặt phải nằm trong khung thời gian của học kỳ (`ngayBatDau` đến `ngayKetThuc`).
   - Ràng buộc duy nhất `uk_buoi_hoc_lop_ngay_ca` trên bộ ba `(lop_hoc_phan_id, ngay_hoc, ca_hoc)` tại cơ sở dữ liệu ngăn chặn hoàn toàn việc tạo trùng buổi học trong cùng ngày và ca khi có request đồng thời. Khi phát hiện trùng lặp trả về `409 Conflict`.

3. **Điểm danh sinh viên**:
   - Mỗi sinh viên trong lớp có tối đa một trạng thái điểm danh trong mỗi buổi học, được bảo vệ bằng ràng buộc duy nhất `uk_diem_danh_buoi_sinh_vien` trên cặp `(buoi_hoc_id, sinh_vien_id)`.
   - Bộ 4 trạng thái ban đầu:
     - `CO_MAT`: Có mặt.
     - `VANG`: Vắng không phép.
     - `MUON`: Đi muộn.
     - `CO_PHEP`: Vắng có phép.
   - Chỉ các sinh viên có đăng ký hiệu lực (`trangThai == DA_DANG_KY`) và tài khoản đang hoạt động mới được phép điểm danh. Cố tình điểm danh sinh viên ngoài lớp sẽ trả về `400 Bad Request`.

4. **Chính sách khóa buổi học (Locking Policy)**:
   - Cho phép giảng viên chỉnh sửa thông tin buổi học và kết quả điểm danh trước khi buổi học bị khóa.
   - Lưu vết kiểm toán: người tạo (`nguoiTao`), thời điểm tạo (`thoiGianTao`), người sửa cuối (`nguoiCapNhat`), thời điểm sửa cuối (`thoiGianCapNhat`), và người khóa (`nguoiKhoa`, `thoiGianKhoa`).
   - Sau khi buổi học được khóa (`daKhoa == true`): giảng viên không thể chỉnh sửa điểm danh hay mở khóa (trả về lỗi `400 Bad Request` hoặc `403 Forbidden`).
   - Buổi học đã khóa chỉ có thể chỉnh sửa hoặc mở khóa bằng quyền đặc biệt đã có của Quản trị viên (`Role.QUAN_TRI`), không tạo thêm quyền vượt chính sách.

5. **Cấu hình hóa quy tắc và các điểm chờ Chủ dự án chốt**:
   - Do quy chế học vụ về cách tính vắng, xử lý vắng có phép, quy đổi muộn sang vắng và mẫu số tính tỷ lệ chuyên cần chưa được Jira chốt chính thức, hệ thống triển khai thông qua cấu hình động tập trung `app.attendance` (`AttendanceProperties`):
     - `app.attendance.trang-thai-tinh-vang`: Trạng thái tính là vắng (mặc định: `VANG`).
     - `app.attendance.co-phep-tinh-vang`: Vắng có phép có tính vào số buổi vắng không (mặc định: `false`).
     - `app.attendance.so-lan-muon-thanh-mot-vang`: Số lần đi muộn quy đổi thành 1 lần vắng (mặc định: `0` - chưa tự ý quy đổi).
     - `app.attendance.mau-so-chuyen-can`: Mẫu số tính tỷ lệ chuyên cần (`SO_BUOI_DA_DIEM_DANH` mặc định theo buổi thực tế, hoặc `SO_BUOI_QUY_DINH` theo tín chỉ).
     - `app.attendance.ghi-chu-quy-che`: Thông điệp quy chế hiển thị minh bạch cho người dùng trên giao diện.
   - API và giao diện trả về đầy đủ các số liệu thô (`soBuoiDaDiemDanh`, `soCoMat`, `soVang`, `soMuon`, `soCoPhep`) kèm tỷ lệ tính theo cấu hình, không âm thầm áp đặt quy chế cứng.

6. **Nhật ký hoạt động (Audit Log)**:
   - Mọi hành vi tạo buổi học (`ATTENDANCE_SESSION_CREATED`), cập nhật buổi học (`ATTENDANCE_SESSION_UPDATED`), khóa/mở khóa (`ATTENDANCE_SESSION_LOCKED` / `ATTENDANCE_SESSION_UNLOCKED`), xóa buổi (`ATTENDANCE_SESSION_DELETED`), và ghi nhận điểm danh (`ATTENDANCE_RECORDED`) đều được lưu vào bảng `audit_log` phục vụ thanh tra đào tạo.

---

## 3. Danh sách Endpoint API

| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `POST` | `/api/giang-vien/lop-hoc-phan/{lopId}/buoi-hoc` | `GIANG_VIEN`, `QUAN_TRI` | Tạo buổi học mới cho lớp học phần (gắn ngày, ca học, phòng, tiết). Chặn trùng lớp/ngày/ca (409). |
| `GET` | `/api/giang-vien/lop-hoc-phan/{lopId}/buoi-hoc` | `GIANG_VIEN`, `QUAN_TRI` | Lấy danh sách các buổi học của lớp học phần. |
| `PUT` | `/api/giang-vien/buoi-hoc/{id}` | `GIANG_VIEN`, `QUAN_TRI` | Cập nhật thông tin buổi học (trước khi khóa; buổi khóa cần quyền QUAN_TRI). |
| `DELETE` | `/api/giang-vien/buoi-hoc/{id}` | `GIANG_VIEN`, `QUAN_TRI` | Xóa buổi học và điểm danh liên quan. |
| `POST` | `/api/giang-vien/buoi-hoc/{id}/khoa` | `GIANG_VIEN`, `QUAN_TRI` | Khóa hoặc mở khóa buổi học. Mở khóa buổi đã khóa yêu cầu quyền QUAN_TRI. |
| `GET` | `/api/giang-vien/buoi-hoc/{id}/diem-danh` | `GIANG_VIEN`, `QUAN_TRI` | Lấy chi tiết buổi học và danh sách sinh viên lớp để điểm danh. |
| `PUT` | `/api/giang-vien/buoi-hoc/{id}/diem-danh` | `GIANG_VIEN`, `QUAN_TRI` | Lưu kết quả điểm danh cho sinh viên trong buổi. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{lopId}/chuyen-can` | `GIANG_VIEN`, `QUAN_TRI` | Báo cáo chuyên cần tổng hợp của lớp học phần. |
| `GET` | `/api/giang-vien/sinh-vien/{studentId}/chuyen-can` | `GIANG_VIEN`, `QUAN_TRI` | Báo cáo chuyên cần theo sinh viên và học kỳ. |
| `GET` | `/api/sinh-vien/lop-hoc-phan/{lopId}/buoi-hoc` | `SINH_VIEN` | Sinh viên xem danh sách các buổi học của lớp mình đang học. |
| `GET` | `/api/sinh-vien/me/chuyen-can` | `SINH_VIEN` | Sinh viên xem báo cáo chuyên cần các môn của mình theo học kỳ. |

---

## 4. Giao diện người dùng

1. **Trang Quản lý Điểm danh của Giảng viên (`/lop-cua-toi/:id/diem-danh`)**:
   - Nút điều hướng nhanh "Điểm danh & chuyên cần" tích hợp trên từng thẻ lớp tại `/lop-cua-toi`.
   - **Tab 1: Điểm danh buổi học**:
     - Danh sách các buổi học theo dạng thanh chọn trực quan, hiển thị ngày, ca, trạng thái khóa, số lượng sinh viên đã điểm danh.
     - Modal tạo buổi học: Ngày học, Ca học, Tiết học, Phòng học, Ghi chú.
     - Chi tiết buổi học được chọn:
       - Thông tin kiểm toán: người tạo, thời gian tạo, người sửa cuối, thời gian sửa cuối.
       - Cảnh báo rõ ràng khi buổi học bị khóa.
       - Nút tiện ích: "Tất cả có mặt", "Khóa buổi", "Sửa buổi", "Xóa buổi", "Lưu điểm danh".
       - Bảng điểm danh sinh viên: chọn nhanh 4 trạng thái (Có mặt, Vắng, Muộn, Có phép) bằng nút màu trực quan, ô nhập ghi chú riêng cho từng sinh viên.
   - **Tab 2: Báo cáo chuyên cần cả lớp**:
     - Thẻ chỉ số tổng quan: Tổng buổi học, Sĩ số lớp, Tỷ lệ chuyên cần trung bình, Số sinh viên nguy cơ cấm thi (< 80%).
     - Khung thông báo quy chế: công thức đang áp dụng và ghi chú quy chế chờ phê duyệt.
     - Bảng chi tiết từng sinh viên: Mã SV, Họ tên, Lớp, Số buổi có mặt/vắng/muộn/phép, Tỷ lệ %, Huy hiệu đánh giá Đạt / Cảnh báo / Nguy cơ cấm thi.
     - Bộ lọc tìm kiếm nhanh sinh viên theo tên hoặc mã số.
2. **Trang Chuyên cần của Sinh viên (`/chuyen-can-cua-toi`)**:
   - Menu "Chuyên cần" trên thanh điều hướng bên trái của sinh viên.
   - Lọc theo học kỳ.
   - Thẻ thống kê tổng số buổi đã diễn ra, số buổi có mặt, vắng không phép, tỷ lệ chuyên cần chung.
   - Bảng chi tiết từng môn học với số buổi có mặt/vắng/muộn/phép, tỷ lệ chuyên cần và trạng thái đánh giá.
