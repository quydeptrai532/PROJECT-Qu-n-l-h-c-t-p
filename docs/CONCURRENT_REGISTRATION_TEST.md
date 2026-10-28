# Kiểm chứng đăng ký học phần đồng thời

Bài kiểm thử tích hợp tạo học kỳ, môn học, lớp học phần và tài khoản sinh viên riêng cho mỗi lần chạy. Nó gửi các request HTTP thật vào API, tranh một lớp có sức chứa cấu hình được, rồi đối chiếu số request thành công với số đăng ký `DA_DANG_KY` và `siSoHienTai` trong database. Bài kiểm thử cũng gửi hai yêu cầu đồng thời của cùng sinh viên đến hai lớp cùng môn và kiểm tra hủy đăng ký trả lại chỗ.

## Chạy với H2 cô lập

Cần JDK 21 và Node không cần cài thêm thư viện. Từ thư mục `backend`:

```powershell
./mvnw.cmd '-Dregistration.test.capacity=3' `
  '-Dregistration.test.student-count=12' `
  '-Dregistration.test.concurrency=12' `
  '-Dregistration.report=target/concurrent-registration-report.json' test
```

Mỗi giá trị có thể đổi để chạy các mức khác nhau. `student-count` phải lớn hơn `capacity` để kiểm tra tranh suất cuối. Report JSON lưu môi trường database, cấu hình đầu vào, status từng request, kết quả nhất quán và p50/p95/p99/max latency. Mặc định file nằm ở `backend/target/concurrent-registration-report.json`.

## Chạy với MySQL/Docker

Tạo một database dành riêng cho bài kiểm thử trước; không trỏ vào database đang chứa dữ liệu cần giữ vì bài kiểm thử tạo schema sạch (`ddl-auto=create-drop`):

```powershell
docker exec qldt-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS qldt_concurrency_test"
$env:REGISTRATION_TEST_DB_URL = "jdbc:mysql://localhost:3307/qldt_concurrency_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:REGISTRATION_TEST_DB_USERNAME = "root"
$env:REGISTRATION_TEST_DB_PASSWORD = "root"
./mvnw.cmd '-Dregistration.test.capacity=3' '-Dregistration.test.student-count=12' `
  '-Dregistration.test.concurrency=12' '-Dregistration.report=target/concurrent-registration-mysql.json' test
```

Nếu MySQL chưa chạy, khởi động dịch vụ bằng `docker compose up -d mysql` trước. Có thể dùng `REGISTRATION_TEST_DB_URL`, `REGISTRATION_TEST_DB_USERNAME` và `REGISTRATION_TEST_DB_PASSWORD` để trỏ đến một MySQL test riêng khác.

Kết quả cần có số thành công bằng `min(capacity, student-count)`, các request thua chỗ nhận HTTP 409, không có lớp nào vượt sức chứa, số bản ghi đăng ký đang hoạt động bằng sĩ số lưu, và chỉ một trong hai đăng ký cùng môn của một sinh viên thành công. Giữ file JSON cùng cấu hình lệnh khi đưa bằng chứng vào báo cáo.
