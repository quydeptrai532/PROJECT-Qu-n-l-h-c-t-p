# Hệ thống Quản lý đào tạo và Đăng ký học phần

Đồ án xây dựng theo tài liệu SRS `SRS_QuanLyDaoTao_DangKyHocPhan`: quản lý chương trình, môn học, lớp học phần; sinh viên đăng ký/hủy học phần trực tuyến; giảng viên nhập điểm; phòng đào tạo thống kê và xuất báo cáo.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Backend | Spring Boot 3.4, Java 21, Spring Security + JWT, Spring Data JPA |
| Cơ sở dữ liệu | MySQL 8.4 (mặc định), H2 (profile `h2` để chạy nhanh) |
| Frontend | React 18, Vite 6, Tailwind CSS 3, React Router, Axios, Recharts |
| Kiểm thử | JUnit 5 + Mockito, Playwright (Chrome), load test bằng Node.js |
| Triển khai & CI/CD | Docker, Docker Compose, Nginx, GitHub Actions, GHCR |

## Chức năng theo vai trò

- **SINH_VIEN**: dashboard tổng quan (tín chỉ, GPA, lịch hôm nay, biểu đồ GPA), cập nhật họ tên/email/SĐT, đổi mật khẩu, lập kế hoạch học tập bằng giỏ lớp và thời khóa biểu dự kiến, xác nhận đăng ký/hủy học phần, xem thời khóa biểu, điểm và GPA.
- **GIANG_VIEN**: dashboard tổng quan (lớp phụ trách, sĩ số, biểu đồ lấp đầy, lịch dạy hôm nay), thời khóa biểu giảng dạy, danh sách sinh viên, nhập/cập nhật điểm quá trình và cuối kỳ.
- **QUAN_TRI**: quản lý khoa/ngành/lớp hành chính/môn học/học kỳ, cấu hình đợt đăng ký, mở lớp học phần, quản lý sinh viên & giảng viên, khóa/mở tài khoản, nhập CSV, dashboard thống kê có biểu đồ và xuất CSV.

Sinh viên và giảng viên đăng nhập vào `/tong-quan`; quản trị vào `/quan-tri`.

Góp ý bổ sung cho báo cáo đề cương: [docs/PROPOSAL_CHECKLIST.md](docs/PROPOSAL_CHECKLIST.md).

## Cấu trúc thư mục

```
.github/workflows/ # CI (build/test/E2E) và build/push Docker image
backend/
  src/main/java/com/edu/registration/
    config/       # Security, CORS, seed dữ liệu mẫu
    controller/   # REST API
    dto/          # request/response records
    entity/       # JPA entities
    exception/    # xử lý lỗi tập trung
    repository/   # Spring Data repositories
    security/     # JWT filter, JwtService, CurrentUser, UserAuthCache
    service/      # nghiệp vụ
  src/test/java/  # unit test nghiệp vụ đăng ký, hủy và nhập điểm
frontend/
  e2e/            # Playwright E2E
  src/
    api/          # axios client, interceptor JWT
    components/   # Layout, Modal, ConfirmDialog, CrudPage, TimetableGrid, UI
    context/      # AuthContext
    lib/          # format, hook useApi
    pages/        # student / lecturer / admin / RoleDashboard
docs/
  TEST_PLAN.md    # test case theo FR/NFR và kết quả
  COURSE_PLANNER_DEMO.md # hướng dẫn demo Course Planner
  AUDIT_LOG_DEMO.md # hướng dẫn tra cứu nhật ký kiểm toán
  PROPOSAL_CHECKLIST.md # góp ý bổ sung báo cáo đề cương
  screenshots/    # ảnh giao diện cho báo cáo/Figma
tools/
  load-test.mjs   # load test 50 request đồng thời
docker-compose.yml
```

## Chạy nhanh

### 1. Docker Compose (MySQL + backend + frontend)

```bash
cp .env.example .env
docker compose up --build
```

- Giao diện: http://localhost:3000
- API: http://localhost:8080/api
- MySQL: `localhost:3307`, database `quan_ly_dao_tao` (port host đổi để tránh đụng MySQL cài sẵn trên máy; trong mạng Docker backend vẫn dùng `mysql:3306`)

Dữ liệu mẫu được tạo tự động khi `SEED_ENABLED=true` và database còn trống.

### 2. Chạy local với H2 (không cần MySQL)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Windows: `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2`

- API: http://localhost:8080/api
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:quan_ly_dao_tao`)

### 3. Chạy local với MySQL

Tạo database `quan_ly_dao_tao`, sau đó đặt biến môi trường (hoặc sửa `application.yml`):

```bash
DB_URL=jdbc:mysql://localhost:3306/quan_ly_dao_tao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
```

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite chạy ở `http://localhost:5173` và proxy `/api` sang `http://localhost:8080`.

## Tài khoản mẫu

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị | `admin` | `admin123` |
| Giảng viên | `gv01`, `gv02`, `gv03` | `123456` |
| Sinh viên | `sv01` … `sv20` | `123456` |

Dữ liệu mẫu gồm 2 khoa, 3 ngành, 3 lớp hành chính, 8 môn học (có môn tiên quyết), 3 học kỳ (một học kỳ đang mở đăng ký), 11 lớp học phần và 20 sinh viên với đăng ký/điểm ở các học kỳ trước.

## Quy tắc nghiệp vụ đã triển khai

1. Chỉ được đăng ký/hủy khi học kỳ đang trong thời gian cho phép (`dang_ky_tu` ≤ hôm nay ≤ `dang_ky_den`).
2. Phải đạt **tất cả** môn tiên quyết (điểm tổng kết ≥ 5.0) trước khi đăng ký.
3. Không đăng ký hai lớp của cùng một môn trong cùng học kỳ.
4. Không trùng thứ và khoảng tiết học với học phần đã đăng ký.
5. Không vượt sĩ số tối đa; kiểm tra và tăng sĩ số nằm trong cùng transaction với khóa bi quan (`SELECT ... FOR UPDATE`) trên lớp học phần.
6. Tổng tín chỉ không vượt quá giới hạn tối đa của học kỳ. Khi hủy, hệ thống chặn nếu tổng tín chỉ còn lại rơi vào khoảng (0, tối thiểu); được phép hủy toàn bộ để rút hết học phần. Khi đăng ký thêm, giao diện cảnh báo nếu chưa đạt mức tối thiểu.
7. Hủy đăng ký hoàn lại một chỗ, giữ lại bản ghi với trạng thái `DA_HUY`; đăng ký lại sẽ kích hoạt lại bản ghi cũ (đảm bảo ràng buộc duy nhất `sinh_vien_id + lop_hoc_phan_id`).
8. Giảng viên chỉ được xem/nhập điểm cho lớp mình phụ trách.
9. Điểm tổng kết = `0.4 × quá trình + 0.6 × cuối kỳ`, làm tròn 1 chữ số; điểm hợp lệ 0–10.
10. GPA hệ 4 quy đổi: ≥8.5→4.0, ≥8.0→3.5, ≥7.0→3.0, ≥6.5→2.5, ≥5.5→2.0, ≥5.0→1.5, ≥4.0→1.0, còn lại 0. Tín chỉ tích lũy chỉ tính môn đạt (≥5.0).
11. Không xóa dữ liệu danh mục đang được tham chiếu; API trả `409 Conflict` kèm thông báo.
12. Mở lớp học phần có cảnh báo trùng phòng/trùng lịch giảng viên (vẫn cho phép lưu, trả về danh sách `canhBao`).

## API chính

| Phương thức | Endpoint | Chức năng |
|---|---|---|
| GET | `/api/health` | Health check công khai (dùng cho CI/Docker) |
| POST | `/api/auth/login` | Đăng nhập, nhận JWT |
| GET/PUT | `/api/auth/me` | Xem/cập nhật hồ sơ (họ tên, email, SĐT) |
| PUT | `/api/auth/doi-mat-khau` | Đổi mật khẩu |
| GET | `/api/hoc-ky` | Danh sách học kỳ |
| GET | `/api/lop-hoc-phan?hocKyId=&keyword=` | Tìm lớp học phần |
| POST | `/api/dang-ky` | Sinh viên đăng ký |
| POST | `/api/dang-ky/xac-nhan-ke-hoach` | Xác nhận nhiều lớp trong một transaction, dùng lại toàn bộ kiểm tra đăng ký |
| DELETE | `/api/dang-ky/{id}` | Sinh viên hủy đăng ký |
| GET | `/api/dang-ky/me?hocKyId=` | Học phần đã đăng ký |
| GET | `/api/sinh-vien/me/thoi-khoa-bieu` | Thời khóa biểu |
| GET | `/api/sinh-vien/me/diem` | Điểm, GPA học kỳ và tích lũy |
| GET | `/api/giang-vien/me/lop-hoc-phan` | Lớp giảng viên phụ trách |
| GET | `/api/giang-vien/me/thoi-khoa-bieu` | Thời khóa biểu giảng dạy |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien` | Danh sách sinh viên của lớp |
| PUT | `/api/diem/{dangKyId}` | Nhập/cập nhật điểm |
| CRUD | `/api/admin/khoa`, `/nganh`, `/lop-hanh-chinh`, `/mon-hoc`, `/hoc-ky` | Quản trị danh mục |
| GET/POST/DELETE | `/api/admin/mon-hoc/{id}/tien-quyet` | Cấu hình môn tiên quyết |
| CRUD | `/api/admin/lop-hoc-phan` | Mở lớp học phần |
| CRUD | `/api/admin/sinh-vien`, `/giang-vien` | Quản lý người dùng |
| POST | `/api/admin/sinh-vien/import` | Nhập sinh viên từ CSV |
| GET | `/api/admin/thong-ke/tong-quan`, `/theo-mon` | Thống kê |
| GET | `/api/admin/lop-hoc-phan/{id}/csv` | Xuất danh sách lớp CSV |
| GET | `/api/admin/audit-logs` | Tra cứu nhật ký (lọc thời gian, người dùng, hành động, đối tượng; phân trang; chỉ quản trị viên) |

Swagger/OpenAPI chưa tích hợp; có thể bổ sung `springdoc-openapi` nếu cần.

## Định dạng CSV nhập sinh viên

Có thể tải file mẫu trên giao diện quản trị sinh viên. Cột (dòng đầu là header, có thể bỏ qua):

```csv
ma,hoTen,email,khoaId,nganhId,lopId
SV010,Nguyễn Văn A,a@student.edu.vn,1,1,1
```

- `khoaId`, `nganhId`, `lopId` là id số, có thể để trống.
- Tên đăng nhập mặc định bằng `ma`, mật khẩu mặc định `123456`.
- Mỗi dòng được xử lý trong transaction riêng; dòng lỗi được báo lại, các dòng hợp lệ vẫn được tạo.

## Kiểm thử và build

### Backend tests

```bash
cd backend
./mvnw test        # 35 test: 22 unit + 13 integration trên HTTP
```

### E2E với Playwright

Yêu cầu backend đang chạy và frontend dùng Chrome đã cài trên máy:

```bash
cd frontend
npm ci
npx playwright test        # 16 kịch bản trên Chrome
BROWSER_CHANNEL=msedge npx playwright test   # chạy lại trên Edge
npx playwright show-report
```

Windows PowerShell: `$env:BROWSER_CHANNEL="msedge"; npx playwright test`.

Cấu hình dùng `channel: "chrome"` nên không cần tải browser của Playwright. Nếu backend chưa chạy, `globalSetup` sẽ báo lỗi rõ ràng. Chi tiết test case và kết quả xem tại [docs/TEST_PLAN.md](docs/TEST_PLAN.md).

### Load test

```bash
# backend đang chạy
node tools/load-test.mjs
# hoặc: cd frontend && npm run test:load
```

Tham số môi trường: `CONCURRENCY` (mặc định 50), `REQUESTS` (200), `WARMUP` (30), `TARGET_P95_MS` (500).

Kết quả chạy trên máy dev (H2 in-memory, Windows, Node 22, có warmup):

| Chỉ số | Giá trị |
|---|---|
| Request / đồng thời | 200 / 50 |
| Thành công / lỗi | 200 / 0 |
| Throughput | ~261 req/s |
| P50 / P95 / P99 | 141 / 332 / 346 ms |

Kết quả thay đổi theo cấu hình máy và database; khi chạy MySQL/Docker nên đo lại trong môi trường đích.

Kiểm chứng đăng ký tranh chỗ cuối bằng request đồng thời, bao gồm cấu hình sức chứa/số sinh viên/độ đồng thời và report kết quả + latency: [docs/CONCURRENT_REGISTRATION_TEST.md](docs/CONCURRENT_REGISTRATION_TEST.md).

### Chụp ảnh giao diện

```bash
cd frontend
npm run screenshots     # cần backend và frontend đang chạy
```

Ảnh được lưu vào `docs/screenshots/` (đăng nhập, dashboard sinh viên/giảng viên/quản trị, đăng ký, điểm, lớp học phần, modal).

### Build frontend

```bash
cd frontend
npm run build      # code-split React, Recharts, icons; xuất dist/
```

## CI/CD

- `.github/workflows/ci.yml`: chạy unit test backend, build jar, build frontend và E2E Playwright trên Chrome khi push/PR vào `main` hoặc `dev`.
- `.github/workflows/docker.yml`: build và push image backend/frontend lên GHCR khi push `main`, tag `v*` hoặc chạy thủ công. Dùng `GITHUB_TOKEN` với quyền `packages: write`.
- Để chạy E2E trong CI, workflow khởi động backend profile `h2` và chờ `/api/health` trước khi chạy Playwright.

## Biến môi trường backend

| Biến | Mặc định | Mô tả |
|---|---|---|
| `DB_URL` | MySQL localhost | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | Tài khoản DB |
| `DB_POOL_SIZE` | `20` | Số connection tối đa của HikariCP |
| `JWT_SECRET` | khóa mẫu 64 ký tự | Khóa ký JWT, nên đổi khi triển khai |
| `JWT_EXPIRATION_MS` | `86400000` | Thời hạn token (24 giờ) |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000` | Origin được phép |
| `SEED_ENABLED` | `true` | Tạo dữ liệu mẫu khi DB trống |

## Ghi chú

- Backend dùng `ddl-auto=update` để tiện phát triển; khi triển khai thật nên dùng migration (Flyway/Liquibase).
- JWT filter dùng cache ngắn hạn (30 giây) cho trạng thái tài khoản để giảm truy vấn DB; khóa/mở khóa tài khoản sẽ xóa cache ngay.
- Đã thêm index cho khóa ngoại của lớp học phần và đăng ký học phần để cải thiện truy vấn danh sách lớp.
- Yêu cầu hiệu năng NFR-01 đã được đo sơ bộ bằng load test ở trên; cần benchmark lại trên MySQL và môi trường triển khai thật.
- CSDL dùng tên bảng/cột tiếng Việt không dấu theo mô hình dữ liệu trong SRS.
