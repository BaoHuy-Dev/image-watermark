# 5) Luồng nghiệp vụ end-to-end

## 5.1 Marketplace flow

1. User vào catalog (`GET /api/products`, `GET /api/products/featured`).
2. User thêm hàng vào cart (frontend state).
3. Checkout gọi `POST /api/orders`.
4. Library gọi `GET /api/orders/my` để hiển thị nội dung đã mua.
5. Download gọi `GET /api/downloads/{productId}`.

## 5.2 Download & watermark flow

Khi user tải file:

1. Backend xác minh quyền sở hữu qua `OrderService.ownsProduct`.
2. Đọc file gốc từ storage.
3. Nhúng watermark cá nhân (`embedAuto`).
4. Sinh fingerprint đa vùng và lưu DB.
5. Trả file đã watermark cho client.

## 5.3 Verify flow

Người kiểm duyệt upload ảnh/file vào `POST /api/watermark/extract`.

Pipeline xử lý:

1. Direct extraction (metadata, robust QIM, LSB fallback).
2. Nếu chưa tìm thấy: fingerprint matching theo nhóm.
3. Trả kết quả JSON gồm `found`, `watermark`, `method`, `confidence`.

## 5.4 Trải nghiệm theo vai trò

### USER

- Mua và tải nội dung.
- Quản lý thư viện cá nhân.

### SELLER/ADMIN

- Dashboard quản lý sản phẩm.
- Upload sản phẩm mới.
- Verify nguồn gốc nội dung bị chia sẻ.

## 5.5 Trường hợp ảnh chụp màn hình phức tạp

Hệ thống vẫn có thể truy vết nếu:

- Ảnh còn chứa một phần nội dung trang/asset đã fingerprint.
- Mức biến đổi chưa vượt quá khả năng nhận diện của dHash + voting.

Ví dụ đã test trong dự án:

- Ảnh desktop rộng có thanh UI.
- Ảnh mobile có status bar/navigation bar.
- Ảnh chỉ chứa vùng nhỏ của nội dung gốc.
