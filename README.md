# Badminton Booking System

RESTful API quản lý và đặt sân cầu lông, hỗ trợ JWT, phân quyền, quản lý sân/time slot/booking, upload ảnh và audit log.

## Tính năng

- Đăng ký, đăng nhập, refresh token, logout và đổi mật khẩu.
- Xác thực JWT stateless; phân quyền `ADMIN`, `MANAGER`, `CUSTOMER`.
- Quản lý user, profile, sân, quản lý được gán cho sân và time slot theo từng sân.
- Customer đặt nhiều time slot trong một booking và xem booking của mình.
- Manager xử lý booking thuộc sân được gán; Admin có quyền trên toàn hệ thống.
- Chống đặt trùng sân/ngày/time slot bằng transaction và `PESSIMISTIC_WRITE`.
- Upload nhiều ảnh sân, soft delete, audit log, validation, exception tập trung, phân trang và lọc dữ liệu.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ/Framework | Java 21, Spring Boot 3.3.5 |
| API/Bảo mật | Spring Web, Spring Security, JWT (JJWT 0.12.6) |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8; H2 cho test |
| Khác | Jakarta Validation, Spring AOP, Lombok, Gradle, Spotless |

## Kiến trúc

Dự án chia theo module nghiệp vụ trong `src/main/java/com/project/modules`:

```text
auth/       booking/       court/       storage/       timeslot/       user/
```

Mỗi module sử dụng các lớp `controller`, `service`, `repository`, `entity`, `dto` và `mapper` tùy nhu cầu. Luồng xử lý chính:

```text
Client -> Spring Security/JWT Filter -> Controller -> Service -> Repository -> MySQL
```

Các thư mục quan trọng:

```text
src/main/java/com/project/
├── aspect/       # Audit booking
├── common/       # Enum, exception, response và utility dùng chung
├── config/       # Security, CORS và static resource
├── data/         # Khởi tạo dữ liệu demo
├── modules/      # Các module nghiệp vụ
└── security/     # JWT, user details và security handler

src/main/resources/
├── application.properties
└── init.sql
```

## Database và quy tắc nghiệp vụ

Các bảng chính gồm `users`, `courts`, `court_managers`, `court_images`, `time_slots`, `bookings`, `booking_time_slots`, `refresh_tokens` và `audit_logs`.

- `users.username` và `users.email` là duy nhất; mỗi user có một role.
- User dùng `is_active`; sân và time slot được soft delete lần lượt bằng `INACTIVE` và `active=false`.
- Admin có thể disable user; xóa user là hard delete và xóa dữ liệu liên quan.
- Time slot thuộc một sân; giá booking lấy từ `time_slots.price`.
- Booking mới luôn là `PENDING`, phải có ít nhất một time slot và không chứa ID trùng.
- Booking `PENDING` hoặc `CONFIRMED` chặn booking khác trùng sân, ngày và bất kỳ time slot nào.
- Trạng thái hợp lệ: `PENDING -> CONFIRMED/REJECTED`, `CONFIRMED -> COMPLETED/CANCELLED`.
- Audit log ghi nhận thao tác tạo booking thành công hoặc thất bại.

Các enum:

| Enum | Giá trị |
| --- | --- |
| `RoleName` | `ADMIN`, `MANAGER`, `CUSTOMER` |
| `CourtStatus` | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |
| `BookingStatus` | `PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED` |

Schema đầy đủ nằm tại `src/main/resources/init.sql` và có thể chạy lại mà không xóa dữ liệu hiện có. Ứng dụng hiện dùng:

Nếu database hiện tại vẫn có `users.status`, chạy migration một lần:

```bash
mysql -u root -p < src/main/resources/migrate_user_status_to_is_active.sql
```

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Cài đặt và chạy

Yêu cầu: **JDK 21** và **MySQL 8+**. Không cần cài Gradle riêng.

### 1. Khởi tạo database

```bash
mysql -u root -p < src/main/resources/init.sql
```

### 2. Cấu hình

Cấu hình mặc định nằm tại `src/main/resources/application.properties`:

| Property | Mặc định |
| --- | --- |
| `server.port` | `8080` |
| `spring.datasource.url` | MySQL local, database `badminton_booking_db` |
| `spring.datasource.username/password` | `root` / `123456` |
| `app.jwt.access-token-expiration-ms` | `1800000` (30 phút) |
| `app.jwt.refresh-token-expiration-ms` | `604800000` (7 ngày) |
| `app.file.upload-dir` | `uploads` |
| Upload limit | `10MB/file`, `50MB/request` |

Có thể override bằng biến môi trường:

```bash
export SPRING_DATASOURCE_PASSWORD='your-password'
export APP_JWT_SECRET='your-secret-key-at-least-256-bits'
```

### 3. Chạy ứng dụng

```bash
./gradlew bootRun
```

Windows dùng `gradlew.bat bootRun`. Base URL: `http://localhost:8080/api/v1`.

Lần chạy đầu, `DataInitializer` tự tạo tài khoản demo:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `123456` |
| Manager | `manager` | `123456` |
| Customer | `customer` | `123456` |

## Quy ước API

Endpoint được bảo vệ yêu cầu header:

```http
Authorization: Bearer <access-token>
```

Response thành công có dạng `{"success":true,"message":"...","data":{}}`. Response lỗi gồm `timestamp`, `status`, `error`, `message` và `path`.

Các endpoint phân trang hỗ trợ:

```text
?page=0&size=10&sort=createdAt,desc
```

HTTP status thường dùng: `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `500`.

## Endpoint

Quyền truy cập: `Public`, `Authenticated`, `Customer`, `Manager/Admin`, `Admin`.

### Auth và user

| Method | Endpoint | Quyền | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Public | Đăng ký customer |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập, nhận access/refresh token |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Xóa refresh token hiện tại |
| `POST` | `/api/v1/auth/change-password` | Authenticated | Đổi mật khẩu |
| `POST` | `/api/v1/auth/forgot-password` | Public | Kiểm tra email tồn tại |
| `GET/PUT` | `/api/v1/profile` | Authenticated | Xem/cập nhật profile |
| `GET/POST` | `/api/v1/admin/users` | Admin | Tìm kiếm/tạo user |
| `GET/PUT/DELETE` | `/api/v1/admin/users/{id}` | Admin | Xem/cập nhật/xóa hẳn user và dữ liệu liên quan |
| `PATCH` | `/api/v1/admin/users/{id}/disable` | Admin | Disable user |

### Sân và quản lý sân

| Method | Endpoint | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/courts` | Public | Tìm sân theo `name`, `status`, phân trang |
| `GET` | `/api/v1/courts/{id}` | Public | Chi tiết sân |
| `POST` | `/api/v1/manager/courts` | Manager/Admin | Tạo sân |
| `PUT/DELETE` | `/api/v1/manager/courts/{id}` | Manager/Admin | Cập nhật/soft delete sân |
| `POST` | `/api/v1/manager/courts/{courtId}/images` | Manager/Admin | Upload nhiều ảnh |
| `DELETE` | `/api/v1/manager/courts/images/{imageId}` | Manager/Admin | Xóa ảnh |
| `GET` | `/api/v1/admin/courts/{courtId}/managers` | Admin | Xem quản lý của sân |
| `POST/DELETE` | `/api/v1/admin/courts/{courtId}/managers/{managerId}` | Admin | Thêm/xóa quản lý |

Manager tạo sân sẽ tự được gán quản lý; Admin tạo sân phải truyền ít nhất một `managerId`. Manager chỉ thao tác trên sân được gán, Admin thao tác trên mọi sân. Dùng `status=ACTIVE` nếu chỉ muốn lấy sân đang hoạt động.

### Time slot và booking

| Method | Endpoint | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/courts/{courtId}/time-slots` | Public | Lấy time slot của sân |
| `POST` | `/api/v1/manager/courts/{courtId}/time-slots` | Manager/Admin | Tạo time slot |
| `PUT/DELETE` | `/api/v1/manager/courts/{courtId}/time-slots/{id}` | Manager/Admin | Cập nhật/soft delete time slot |
| `POST` | `/api/v1/customer/bookings` | Customer | Tạo booking với `courtId`, `timeSlotIds`, `bookingDate`, `note?` |
| `GET` | `/api/v1/customer/bookings` | Customer | Xem booking của chính mình |
| `GET` | `/api/v1/manager/bookings` | Manager/Admin | Xem booking thuộc sân quản lý |
| `GET` | `/api/v1/manager/bookings/{id}` | Manager/Admin | Chi tiết booking |
| `PUT` | `/api/v1/manager/bookings/{id}/status` | Manager/Admin | Cập nhật trạng thái booking |

`startTime` phải nhỏ hơn `endTime`, dùng định dạng 24 giờ `"HH:mm"`. `bookingDate` dùng `yyyy-MM-dd` và không được ở quá khứ.

## Bảo mật

- Mật khẩu mã hóa bằng BCrypt; server dùng `SessionCreationPolicy.STATELESS`.
- JWT ký bằng HMAC secret; role được tải từ database khi xác thực request.
- Refresh token được lưu trong database và rotate khi refresh.
- Logout xóa refresh token; access token đã cấp vẫn dùng được đến khi hết hạn.
- User `LOCKED` hoặc `DISABLED` không thể đăng nhập/xác thực.
- `401` dành cho request chưa xác thực/token không hợp lệ; `403` dành cho request thiếu quyền.
- Public: auth register/login/refresh/forgot-password, `GET /api/v1/courts/**`, `GET /uploads/**`.
- Customer chỉ truy cập `/api/v1/customer/**`; Manager/Admin truy cập `/api/v1/manager/**`; chỉ Admin truy cập `/api/v1/admin/**`.
- CORS hiện cho phép mọi origin, các method `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` và mọi header.

## Upload ảnh

Hỗ trợ PNG, JPG/JPEG và WEBP, tối đa `10MB/file`. Ảnh lưu tại:

```text
uploads/courts/<uuid>.<extension>
```

Upload ảnh:

```bash
curl -X POST http://localhost:8080/api/v1/manager/courts/1/images \
  -H 'Authorization: Bearer <manager-access-token>' \
  -F 'files=@/path/to/court.jpg'
```

Ảnh có thể truy cập công khai qua `http://localhost:8080/uploads/courts/<file-name>`.

## Postman, kiểm thử và format

Import `postman/Badminton_Booking_API.postman_collection.json` để kiểm thử API. Collection tự lưu token và các ID cần thiết; xem hướng dẫn tại `postman/README.md`.

```bash
./gradlew test            # Chạy test bằng H2
./gradlew spotlessCheck   # Kiểm tra format
./gradlew spotlessApply   # Tự động format
./gradlew clean build     # Build toàn bộ dự án
```

Integration test kiểm tra đăng nhập/tạo booking, chống đặt trùng, phân quyền Customer/Manager, audit log và refresh token sau logout.

## Lưu ý production

1. Thay JWT secret, database password và tài khoản demo mặc định; lưu secret qua biến môi trường/secret manager.
2. Giới hạn CORS theo frontend thực tế và bật HTTPS.
3. Dùng Flyway/Liquibase thay cho `ddl-auto=update`.
4. Hoàn thiện `forgot-password`; hiện endpoint chưa gửi email/reset token.
5. Bổ sung unique constraint hoặc chiến lược chống trùng booking ở tầng database.
6. Bổ sung rate limiting, logging, monitoring và cơ chế dọn token/file hết hạn.
7. Bổ sung kiểm tra trùng email khi cập nhật user/profile.
8. Dùng persistent volume hoặc object storage cho file upload.
