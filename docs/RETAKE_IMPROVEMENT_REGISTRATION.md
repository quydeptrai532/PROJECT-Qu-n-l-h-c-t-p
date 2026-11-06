# Đăng ký học lại và học cải thiện (QLSV-60)

## Điều kiện và dữ liệu

- `GET /api/sinh-vien/me/dang-ky-hoc-lai-cai-thien?hocKyId=...` lấy hồ sơ từ người dùng đã xác thực; danh sách lớp được nối với kết quả của chính sinh viên đó.
- Lịch sử có điểm tổng kết được xếp theo học kỳ mới nhất. Theo ngưỡng hiện hữu của dự án (đạt từ 5.0), kết quả gần nhất dưới 5.0 xếp vào học lại; từ 5.0 trở lên xếp vào học cải thiện.
- `POST /api/dang-ky` nhận `loaiDangKy` cho thao tác học lại/cải thiện. Backend đọc lại lịch sử, xác thực loại được yêu cầu, thời gian mở đăng ký, trùng môn/lịch, tiên quyết, sĩ số và giới hạn tín chỉ. Thiếu loại khi môn đã có kết quả trước hoặc tự gửi loại cho môn chưa có kết quả đều bị từ chối với lý do cụ thể.
- Mỗi lần học tiếp tục là một đăng ký học phần riêng và điểm liên kết theo đăng ký đó. Không sửa hoặc thay thế điểm của lần học trước.

## Quy chế cần chủ dự án chốt

Repo không có cấu hình quy chế thay điểm/GPA cho học lại hoặc học cải thiện. Ngưỡng 5.0 hiện là hằng số trong logic hiện hữu, không phải cấu hình thay điểm. Vì vậy không suy diễn lần cao nhất/thấp nhất/mới nhất để thay thế điểm cũ.

Màn hình kết quả vẫn phản ánh cách tính GPA đang có (tính các đăng ký hiện hành có điểm), nhưng đánh dấu là **chưa chính thức** với môn có nhiều lần học. Trước khi công bố kết quả chính thức, chủ dự án cần xác định tối thiểu:

1. Với học cải thiện/học lại, điểm được dùng là cao nhất, mới nhất, lần đầu hay quy tắc khác.
2. Lần học nào đóng góp tín chỉ tích lũy và GPA; cách xử lý điểm chưa đạt, rút/hủy và các lần học song song.
3. Phạm vi áp dụng theo chương trình/khóa và ngày hiệu lực.

Cho đến khi được quyết định và cấu hình, ứng dụng giữ nguyên mọi lần học/điểm, không thực hiện thay điểm ngầm và không coi GPA lặp lần học là kết quả chính thức.
