# Postman

## Import

Chỉ cần import một file sau vào Postman:

- `Badminton_Booking_API.postman_collection.json`

Không cần tạo hoặc chọn environment. Các biến cấu hình, tài khoản, token và ID
được lưu trực tiếp trong collection variables.

Chạy ứng dụng tại `http://localhost:8080`.

## Chạy tự động

Chọn collection và dùng **Run collection**. Luồng chính từ folder `01` đến `06`
sẽ tự đăng nhập, tạo dữ liệu, lưu access token, refresh token, `courtId`,
`timeSlotId`, `bookingId`, `userId` và dùng lại ở các request tiếp theo. Mỗi
request kiểm tra HTTP status mong đợi, `Content-Type` và response envelope chung.

Các nhóm có tác động phụ nên được chạy thủ công khi cần:

- Folder `07`: chọn file ảnh trong request upload trước khi chạy.
- Folder `08`: kiểm thử đổi mật khẩu và logout trên tài khoản vừa được đăng ký.
  Nên chạy sau cùng.
- Folder `09`: xóa mềm court/time slot. Nên chạy sau các folder cần dữ liệu này.

## Biến thường dùng

Mở collection, chọn tab **Variables** để đổi nhanh:

| Biến | Mục đích |
| --- | --- |
| `baseUrl` | URL ứng dụng, mặc định `http://localhost:8080` |
| `page`, `size`, `sortAsc`, `sortDesc` | Tham số phân trang dùng chung |
| `defaultPrice`, `updatedPrice`, `bookingStatus` | Dữ liệu kiểm thử dùng lại |

Tài khoản seed dùng chung mật khẩu `123456`:

| Role | Username |
| --- | --- |
| Admin | `admin` |
| Manager | `manager` |
| Customer | `customer` |
