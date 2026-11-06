# QLSV-67 / TASK-LC-02: Phạm vi xem thông tin sinh viên cho Giảng viên

## 1. Mục tiêu và bối cảnh nghiệp vụ
Theo quy định bảo vệ dữ liệu cá nhân và phân quyền đào tạo, giảng viên (`GIANG_VIEN`) chỉ được phép xem thông tin học tập tối thiểu của các sinh viên đang đăng ký học phần (`trangThai == DA_DANG_KY`) trong các lớp học phần mà giảng viên đó trực tiếp phụ trách.

Giảng viên không được phép:
- Xem thông tin sinh viên thuộc các lớp do giảng viên khác phụ trách.
- Xem thông tin sinh viên ngoài phạm vi các lớp được phân công.
- Tiếp cận dữ liệu nhạy cảm hoặc thông tin liên hệ riêng tư của sinh viên (như mật khẩu, hash, token, số điện thoại cá nhân).
- Vượt quyền thông qua việc thay đổi `id` lớp học phần hoặc `studentId` trên URL (URL tampering).
- Dò quét danh tính hay suy luận dữ liệu thông qua autocomplete, số lượng kết quả tìm kiếm hoặc thông báo phụ.

---

## 2. Nguyên tắc kiểm soát truy cập và bảo vệ dữ liệu

### 2.1. Xác thực và kiểm tra phân công ở Backend
- Mọi endpoint phục vụ giảng viên đều lấy thông tin giảng viên từ Security Context (`CurrentUser.requireGiangVien()`), liên kết với tài khoản người dùng (`User.username`).
- Không bao giờ tin tưởng hay cấp quyền dựa vào tham số client gửi lên.
- Với endpoint theo lớp học phần: kiểm tra `lopHocPhan.giangVien.id == currentGiangVien.id`.
- Với endpoint tra cứu sinh viên: kiểm tra quan hệ đăng ký hoạt động `trangThai == DA_DANG_KY` của sinh viên trong lớp học phần thuộc quyền phụ trách của giảng viên thông qua `DangKyHocPhanRepository` và `SinhVienRepository`.

### 2.2. Xử lý lỗi theo quy ước
- Lớp học phần hoặc sinh viên không tồn tại trong hệ thống: trả về `404 Not Found`.
- Lớp học phần tồn tại nhưng do giảng viên khác phụ trách: trả về `403 Forbidden` (`Bạn không có quyền truy cập lớp học phần này`).
- Sinh viên tồn tại nhưng không đăng ký lớp học phần của giảng viên hoặc nằm ngoài phân công: trả về `403 Forbidden` (`Sinh viên không thuộc phạm vi lớp học phần bạn phụ trách`).
- Người dùng có vai trò khác (như `SINH_VIEN`) truy cập endpoint giảng viên: trả về `403 Forbidden`.

### 2.3. Chính sách thu nhỏ dữ liệu (Data Minimization)
Không trả trực tiếp JPA Entity. Mọi endpoint trả về DTO chuyên biệt:
- **`StudentMinimalProfileDto`**:
  - `id`: Mã định danh số.
  - `ma`: Mã sinh viên.
  - `hoTen`: Họ và tên sinh viên.
  - `email`: Email nhà trường cấp.
  - `khoaMa`, `khoaTen`: Khoa quản lý.
  - `nganhMa`, `nganhTen`: Ngành đào tạo.
  - `khoaNhapHoc`: Khóa tuyển sinh.
  - `lopMa`, `lopTen`: Lớp hành chính.
  - `chuongTrinhKhoaNhapHoc`, `chuongTrinhPhienBan`: Thông tin chương trình đào tạo.
  - `trangThaiHocTap`: Trạng thái học tập của sinh viên.
- **Dữ liệu tuyệt đối loại bỏ / không trả về**:
  - `password`, `matKhau`, `token`.
  - `soDienThoai` (số điện thoại cá nhân).
  - Địa chỉ cá nhân, CCCD/CMND.

### 2.4. Hiệu năng và truy vấn
- Sử dụng `@EntityGraph` (`SinhVienRepository.findWithAcademicProfileById`) nạp sẵn các quan hệ `khoa`, `nganh`, `lopHanhChinh`, `chuongTrinh`.
- Sử dụng JOIN FETCH trong `DangKyHocPhanRepository.findAssignedStudentsOfLecturer` tránh triệt để lỗi N+1 queries khi nạp danh sách và tìm kiếm sinh viên.

---

## 3. Danh sách Endpoint được bảo vệ

| Method | Endpoint | Quyền hạn | Mô tả & Dữ liệu trả về |
|---|---|---|---|
| `GET` | `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien` | `GIANG_VIEN` | Danh sách sinh viên thuộc lớp phụ trách. Hỗ trợ query param `tuKhoa` / `keyword` tìm kiếm theo mã, họ tên, email. Chỉ trả sinh viên đăng ký lớp đó. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}` | `GIANG_VIEN` | Hồ sơ học tập tối thiểu (`StudentMinimalProfileDto`) của sinh viên thuộc lớp phụ trách. Chặn 403 nếu đổi ID sinh viên sang lớp khác. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so` | `GIANG_VIEN` | Hồ sơ đào tạo học vụ (`StudentAcademicProfileDto`) của sinh viên thuộc lớp phụ trách. |
| `GET` | `/api/giang-vien/sinh-vien` | `GIANG_VIEN` | Tìm kiếm/danh sách sinh viên nằm trong tất cả các lớp giảng viên phụ trách. Params: `lopHocPhanId` (tùy chọn), `tuKhoa` (tùy chọn). |
| `GET` | `/api/giang-vien/sinh-vien/{studentId}` | `GIANG_VIEN` | Hồ sơ học tập tối thiểu (`StudentMinimalProfileDto`) của sinh viên nếu sinh viên có học ít nhất 1 lớp do giảng viên phụ trách. Trả 403 nếu không được phân công. |
| `GET` | `/api/giang-vien/sinh-vien/{studentId}/ho-so` | `GIANG_VIEN` | Hồ sơ học vụ (`StudentAcademicProfileDto`) của sinh viên nếu sinh viên thuộc phạm vi phân công của giảng viên. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv` | `GIANG_VIEN` | Xuất CSV danh sách sinh viên lớp phụ trách. Kiểm tra chặt quyền giảng viên phụ trách trước khi xuất dữ liệu. |

---

## 4. Kiểm thử và xác minh

Bộ kiểm thử tích hợp đầy đủ tại `LecturerStudentScopeIntegrationTest`:
1. `testLecturerCanOnlyViewAssignedClassStudentList`: Xác minh giảng viên xem đúng danh sách sinh viên lớp mình phụ trách và lọc từ khóa chính xác.
2. `testLecturerCannotAccessOtherLecturerClass`: Xác minh trả về 403 Forbidden khi giảng viên cố tình gọi API danh sách sinh viên hoặc xuất CSV lớp của giảng viên khác.
3. `testNonExistentClassOrStudentReturns404`: Xác minh trả về 404 Not Found theo đúng chuẩn khi ID lớp hoặc ID sinh viên không tồn tại.
4. `testViewStudentMinimalProfileAndTamperingProtection`: Xác minh hồ sơ tối thiểu trả đúng thông tin cần thiết, không chứa thông tin nhạy cảm; thay đổi ID sinh viên hoặc ID lớp sang đối tượng không được phân công nhận ngay 403 Forbidden.
5. `testSearchAssignedStudentsDoesNotLeakExternalData`: Xác minh tìm kiếm sinh viên chỉ trả về sinh viên trong phạm vi phân công, không rò rỉ bất kỳ sinh viên nào của lớp/giảng viên khác.
6. `testRoleIsolation`: Xác minh sinh viên hoặc người dùng khác không thể truy cập các endpoint này (nhận 403 Forbidden).
