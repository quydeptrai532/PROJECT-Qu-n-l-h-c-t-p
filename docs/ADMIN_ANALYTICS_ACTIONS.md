# Admin Analytics thành danh sách hành động

API `GET /api/admin/thong-ke/hanh-dong?hocKyId=&khoaId=` và dashboard quản trị dùng các lớp học phần, môn/khoa, học kỳ và bản ghi đăng ký hiện có. Bộ lọc học kỳ và khoa được áp dụng ở truy vấn backend. Số đăng ký được đếm từ `DangKyHocPhan` với trạng thái `DA_DANG_KY`; bản ghi `DA_HUY` không được tính. API không tải danh sách sinh viên.

## Định nghĩa mục cần chú ý

| Loại | Quy tắc | Dữ liệu hiển thị |
|---|---|---|
| Lớp gần đầy | Đăng ký hiệu lực / sĩ số tối đa >= 90%. Mốc 90% đã được dùng trong dashboard hiện tại. | Mã lớp, môn, đăng ký hiệu lực, chỉ tiêu, tỷ lệ, học kỳ |
| Lớp ít đăng ký | Đăng ký hiệu lực / sĩ số tối đa <= 25%. | Mã lớp, môn, đăng ký hiệu lực, chỉ tiêu, tỷ lệ, học kỳ |
| Nhu cầu môn đạt chỉ tiêu | Tổng đăng ký hiệu lực của các lớp môn / tổng chỉ tiêu các lớp môn >= 100%. Đây là tín hiệu cần xem dữ liệu nguồn; không tự kết luận cần mở thêm lớp. | Môn, số lớp, đăng ký hiệu lực, tổng chỉ tiêu, tỷ lệ, học kỳ |
| Đợt đăng ký sắp đóng | Học kỳ đang trong khoảng đăng ký mở và ngày hết hạn còn từ 0 đến 7 ngày tính từ ngày hiện tại. | Hạn đăng ký, số ngày còn lại, số lớp trong phạm vi, học kỳ |

Không có mức ưu tiên tùy ý; nhãn phân loại phản ánh trực tiếp quy tắc trên. Mỗi mục có `mucTinh`, lý do bằng số liệu thật và đường dẫn đến màn hình quản trị phù hợp. Học kỳ được giữ trên đường dẫn khi mở danh sách lớp; mã lớp/môn được đưa vào ô tìm kiếm sẵn có.

Nếu chưa có học kỳ, API trả danh sách rỗng cùng `hocKyId: null`. Danh sách UI có trạng thái tải, lỗi truy vấn, không có mục cần chú ý và thời điểm truy vấn gần nhất. Controller yêu cầu `QUAN_TRI`; trang đích vẫn áp dụng quyền riêng hiện có. Không có schema/migration mới.
