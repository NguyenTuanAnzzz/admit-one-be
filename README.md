# Admit One — Backend

Backend cho **Admit One**, nền tảng bán vé sự kiện chịu tải cao (concert, show...). Xây dựng để luyện các bài toán thật của hệ thống bán vé: chống bán vượt số lượng khi nhiều người tranh mua cùng lúc, giữ vé tạm có hạn, cập nhật số vé còn lại theo thời gian thực, thanh toán qua cổng payOS với webhook idempotent, và chống bot mua vé hàng loạt.

Frontend: [admit-one-fe](https://github.com/NguyenTuanAnzzz/admit-one-fe)

## Công nghệ

- **Java 17**, **Spring Boot**
- **PostgreSQL** — dữ liệu chính (user, sự kiện, vé, đơn hàng, thanh toán)
- **Redis** — lớp tăng tốc chống bán vượt (Lua script), cache số vé còn lại
- **Spring Security + JWT** — access token (RAM phía FE) + refresh token (httpOnly cookie)
- **WebSocket (STOMP)** — đẩy số vé còn lại theo thời gian thực tới mọi client đang xem sự kiện
- **payOS** — cổng thanh toán, xác minh chữ ký webhook
- **Cloudinary** — lưu ảnh poster sự kiện
- **Spring Mail** — gửi OTP xác minh email, gửi vé điện tử

## Tính năng chính

| Mã | Tính năng | Trạng thái |
|---|---|---|
| 5.1 | Chống bán vượt số lượng (Redis Lua + Postgres conditional update làm lớp chốt cuối) | ✅ |
| 5.2 | Giữ vé tạm 10 phút, tự hoàn trả đúng hạn (sai số ±2 giây) | ✅ |
| 5.3 | Số vé còn lại cập nhật realtime qua WebSocket | ✅ |
| 5.4 | Phòng chờ ảo | ❌ Không làm (ngoài phạm vi) |
| 5.5 | Thanh toán payOS + webhook idempotent (chống xử lý trùng giao dịch) | ✅ |
| 5.6 | Chống bot (giới hạn 4 vé/người/sự kiện) | ✅ một phần |

Cùng với các trang quản trị: quản lý người dùng (Admin/Staff), tra cứu đơn hàng + log thanh toán, check-in vé bằng QR tại cửa, tổng quan doanh thu kèm biểu đồ.

## Chạy ở máy

### Yêu cầu
- JDK 17
- Maven (dùng `./mvnw` đi kèm, không cần cài riêng)
- PostgreSQL và Redis chạy sẵn (local hoặc Docker)

### Biến môi trường

Tạo biến môi trường (hoặc file `.env` tuỳ cách bạn chạy) với các khoá sau — **không ghi giá trị thật vào `application.properties`**:

```
DB_PASSWORD=
MAIL_USERNAME=
MAIL_PASSWORD=
JWT_SECRET=
CLOUDINARY_URL=
PAYOS_CLIENT_ID=
PAYOS_API_KEY=
PAYOS_CHECKSUM_KEY=
```

Mặc định kết nối Postgres tại `localhost:5432/admit_one` (user `postgres`) và Redis tại `localhost:6379` — sửa trực tiếp trong `src/main/resources/application.properties` nếu khác.

### Khởi động

```bash
./mvnw spring-boot:run
```

Server chạy ở `http://localhost:8080`, API gốc `http://localhost:8080/api`.

## Cấu trúc

```
src/main/java/com/an/admit_one/
├── controller/      # REST endpoint
├── service/         # Nghiệp vụ
├── repository/       # Spring Data JPA
├── entity/          # Bảng trong Postgres
├── dto/             # request/response
├── security/        # JWT, STOMP auth interceptor
├── config/          # WebSocket, Redis config
└── scheduler/        # Job nền (hết hạn giữ vé...)
```
