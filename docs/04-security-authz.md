# 4) Bảo mật, xác thực và phân quyền

## 4.1 Cơ chế xác thực

Nguồn chính:

- `backend/.../AuthController.java`
- `backend/.../SecurityConfig.java`
- `backend/.../security/JwtFilter.java`
- `backend/.../security/JwtUtil.java`

### Đăng nhập

- User gửi email/password.
- Mật khẩu so sánh bằng BCrypt.
- Nếu hợp lệ, server trả JWT chứa userId/email/role.

### Đăng ký

- Validate email/password bắt buộc.
- Chặn trùng email.
- Tạo user role mặc định `USER`.

## 4.2 Phân quyền endpoint

Theo `SecurityConfig`:

- Public:
  - `/api/auth/**`
  - `GET /api/products/**`
  - `/api/watermark/**`
  - `/h2-console/**` (dev)
- Còn lại cần authenticated JWT.

Lưu ý: endpoint watermark đang mở public để thuận tiện verify. Trong production có thể cân nhắc giới hạn role/admin.

## 4.3 CORS và session

- CORS cho phép localhost/127.0.0.1 ở mọi port (phù hợp dev frontend/mobile).
- Session stateless (`SessionCreationPolicy.STATELESS`).
- JWT filter chạy trước UsernamePasswordAuthenticationFilter.

## 4.4 Điểm cần chú ý khi production

1. Chuyển secret JWT sang biến môi trường hoặc vault.
2. Thu hẹp CORS origin theo domain thực tế.
3. Bật HTTPS bắt buộc.
4. Giới hạn tốc độ (rate limit) cho `/api/watermark/extract`.
5. Audit log cho hành vi verify và download.
6. Cân nhắc đóng public endpoint watermark hoặc yêu cầu API key/JWT đặc thù.
