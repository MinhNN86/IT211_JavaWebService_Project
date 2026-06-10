# Postman

## Import

Chỉ cần import một file sau vào Postman:

- `Badminton_Booking_API_All_In_One.postman_collection.json`

Không cần tạo hoặc chọn environment. Các biến cấu hình, tài khoản, token và ID
được lưu trực tiếp trong collection variables.

Chạy ứng dụng tại `http://localhost:8080`.

## Chạy tự động

Chọn collection và dùng **Run collection**. Luồng chính từ folder `01` đến `06`
sẽ tự đăng nhập, tạo dữ liệu, lưu access token, refresh token, `courtId`,
`timeSlotId`, `bookingId`, `userId` và dùng lại ở các request tiếp theo.

Các nhóm có tác động phụ được bỏ qua mặc định khi chạy collection:

- Folder `07`: chọn file ảnh và đặt `runFileUploads` thành `true`.
- Folder `08`: đổi mật khẩu/logout, nên chạy thủ công sau cùng.
- Folder `09`: đặt `runCleanup` thành `true` để xóa mềm court/time slot.

Tài khoản seed dùng chung mật khẩu `123456`:

| Role | Username |
| --- | --- |
| Admin | `admin` |
| Manager | `manager` |
| Customer | `customer` |
