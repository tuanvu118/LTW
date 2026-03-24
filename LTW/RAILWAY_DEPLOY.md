# Hướng dẫn triển khai LTW Backend (Spring Boot + MySQL) trên Railway

Tham khảo: [Railway Spring Boot Guide](https://docs.railway.app/guides/spring-boot) | [Railway MySQL Guide](https://docs.railway.app/guides/mysql)

---

## Bước 1: Tạo Project trên Railway

1. Đăng nhập [railway.app](https://railway.app)
2. Click **New Project**
3. Chọn **Deploy from GitHub repo** → chọn repository chứa code LTW

**Lưu ý:** Nếu repo của bạn có cấu trúc thư mục lồng nhau (ví dụ: `LTW/LTW/` chứa `pom.xml`), vào **Settings** của service → **Root Directory** → đặt là `LTW` (đường dẫn tới thư mục chứa `pom.xml` và `Dockerfile`).

---

## Bước 2: Thêm MySQL Database

1. Trong project, click **+ New** hoặc `Ctrl/Cmd + K`
2. Chọn **Database** → **MySQL** (hoặc dùng [MySQL template](https://railway.app/template/mysql))
3. Đợi MySQL khởi động
4. Sau khi tạo xong, MySQL service sẽ có các biến: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`

---

## Bước 3: Liên kết MySQL với Backend Service

1. Click vào **service Spring Boot** (service deploy từ GitHub)
2. Vào **Variables**
3. Click **Add a variable reference** (hoặc **Reference**)
4. Chọn **MySQL service** → chọn các biến:
   - `MYSQLHOST` 
   - `MYSQLPORT`
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

Hoặc có thể dùng **Variables** → **Add all variables from service** để thêm tất cả biến từ MySQL service.

---

## Bước 4: Cấu hình Biến môi trường bắt buộc

Vào **Variables** của Spring Boot service và thêm:

| Biến | Bắt buộc | Ví dụ |
|------|----------|-------|
| `JWT_SECRET` | ✅ | Chuỗi bí mật dài tối thiểu 32 ký tự |
| `CORS_ALLOWED_ORIGINS` | ✅ | URL Frontend của bạn, ví dụ: `https://your-fe.railway.app` |
| `CLOUDINARY_CLOUD_NAME` | Tùy chọn | Nếu dùng Cloudinary |
| `CLOUDINARY_API_KEY` | Tùy chọn | |
| `CLOUDINARY_API_SECRET` | Tùy chọn | |
| `SPRING_PROFILES_ACTIVE` | (mặc định trong Dockerfile) | `production` |

### CORS – Điền URL Frontend

Thêm biến `CORS_ALLOWED_ORIGINS` với URL Frontend:

```
https://your-fe.railway.app
```

Nếu có nhiều domain (FE dev, FE prod):

```
https://your-fe.railway.app,https://your-fe.vercel.app,http://localhost:3000
```

**Lưu ý:** Không thêm dấu cách sau dấu phẩy, hoặc dùng dấu phẩy rồi space đều được (code đã trim khoảng trắng).

---

## Bước 5: Tạo Public Domain

1. Vào **Settings** của Spring Boot service
2. Mục **Networking** → **Generate Domain**
3. Copy URL dạng: `https://ltw-production-xxxx.up.railway.app`

URL này là địa chỉ API backend để FE gọi.

---

## Bước 6: Deploy

- **Tự động:** Nếu đã deploy từ GitHub, mỗi lần push code, Railway sẽ tự build và deploy.
- **Thủ công (CLI):**
  ```bash
  npm i -g @railway/cli
  railway login
  railway link   # link với project
  railway up    # deploy
  ```

---

## Cấu trúc file cấu hình đã thêm

| File | Mô tả |
|------|-------|
| `CorsConfig.java` | Cấu hình CORS, đọc `app.cors.allowed-origins` từ properties/env |
| `application-production.properties` | Profile cho Railway, dùng biến môi trường |
| `application.example.properties` | Thêm `app.cors.allowed-origins` mẫu cho local |
| `Dockerfile` | Multi-stage build Java 21, dùng Maven wrapper |
| `railway.json` | Dùng Dockerfile để build |

---

## Xử lý lỗi thường gặp

### Build lỗi
- Kiểm tra Java 21 trong `pom.xml` khớp với Dockerfile
- Kiểm tra đường dẫn `pom.xml`, `mvnw`, `src/` nằm đúng thư mục root của repo

### Không kết nối được database
- Kiểm tra đã reference đủ biến MySQL từ Variables
- Kiểm tra MySQL service đang chạy
- Nếu dùng TCP Proxy để kết nối ngoài project: bật TCP Proxy cho MySQL (Settings → TCP Proxy)

### CORS lỗi khi gọi API từ FE
- Kiểm tra `CORS_ALLOWED_ORIGINS` đã chứa đúng URL FE (bao gồm `https://`, không có `/` cuối)
- Ví dụ đúng: `https://my-app.vercel.app`
- Ví dụ sai: `https://my-app.vercel.app/` hoặc `my-app.vercel.app`

---

## Lưu ý bảo mật

- **Không** commit `application.properties` (chứa mật khẩu). Chỉ dùng `application.example.properties` làm mẫu.
- `JWT_SECRET` phải đủ mạnh, tối thiểu 32 ký tự random.
- Cloudinary keys nên để trong Variables, không hardcode trong code.
