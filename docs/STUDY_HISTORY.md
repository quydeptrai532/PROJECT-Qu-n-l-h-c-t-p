# Lịch sử học tập và môn chưa đạt (QLSV-61 / TASK-ST-05)

Sinh viên tra cứu lịch sử học tập và danh sách môn chưa đạt theo học kỳ tại `/lich-su-hoc-tap`.

## API

`GET /api/sinh-vien/me/lich-su-hoc-tap?hocKyId={id}&trangThai={DAT|CHUA_DAT|CHUA_CO_DIEM}`

- Chỉ cho vai trò `SINH_VIEN`; hồ sơ sinh viên luôn được xác định từ tài khoản đang đăng nhập (`CurrentUser`), không nhận `studentId` từ client.
- `hocKyId` (tùy chọn): phải là ID học kỳ dương và tồn tại trong hệ thống (`400 Bad Request` nếu `<= 0`, `404 Not Found` nếu không tồn tại).
- `trangThai` (tùy chọn): chỉ chấp nhận `DAT`, `CHUA_DAT`, `CHUA_CO_DIEM` (hoặc để trống/`TAT_CA`); giá trị khác trả `400 Bad Request`.
- Thứ tự kết quả cố định: học kỳ theo `ngayBatDau` tăng dần (`id` tăng dần nếu trùng ngày), sau đó theo mã môn học (`maMon`, `monHocId`) và thứ tự lần học (`thoiGianDangKy`, `maLop`, `dangKyId`).
- Số thứ tự lần học (`lanHoc`) và tổng số lần học của môn (`tongSoLanHoc`) được tính trên toàn bộ lịch sử đăng ký hợp lệ của sinh viên trước khi áp bộ lọc để các lần học lại/học cải thiện luôn giữ đúng thứ tự lần học và hiển thị thành các bản ghi riêng.

## Quy tắc công bố điểm, ngưỡng đạt và giới hạn hiện tại

- Hệ thống hiện chưa có bảng/cờ phê duyệt công bố điểm riêng (thuộc `TASK-LC-09`); theo quy ước hiện hành trong `DiemService`, `SinhVienDashboardService` và `HocLaiCaiThienService`, bản ghi điểm chỉ được xem là đã công bố khi có `diemTongKet != null`.
- Nếu chưa có bản ghi điểm hoặc `diemTongKet == null`, API trả `diemQuaTrinh = null`, `diemCuoiKy = null`, `diemTongKet = null` và trạng thái `CHUA_CO_DIEM` (`Chưa có điểm`), không rò rỉ điểm thành phần nháp và không quy thành `0` hay trượt.
- Ngưỡng đạt dùng chung `AcademicRules.DIEM_DAT` (`5.0`); công thức GPA hệ 10, GPA hệ 4 và tín chỉ tích lũy tái sử dụng trực tiếp quy tắc của `DiemService`.
- Quy chế thay thế điểm khi học lại/học cải thiện để tính GPA chính thức chưa được cấu hình trong dự án; vì vậy mọi lần học được bảo toàn riêng và giao diện hiển thị cảnh báo rõ ràng khi tra cứu.
