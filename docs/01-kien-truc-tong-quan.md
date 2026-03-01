# 1) Tổng quan kiến trúc

## 1.1 Cấu trúc module

Dự án là mono-repo gồm 4 phần chính:

- `sdk`: thư viện Java xử lý watermark/robust extraction.
- `backend`: Spring Boot API cho auth, sản phẩm, đơn hàng, tải file, verify watermark.
- `frontend`: React + Vite web app cho marketplace và seller dashboard.
- `mobile`: React Native app cho trải nghiệm mobile catalog/library.

## 1.2 Kiến trúc logic

### Tầng SDK (thuật toán)

- `WatermarkEngine`: facade điều phối embed/extract theo loại file.
- `LsbSteganography`: watermark vô hình cấp pixel cho ảnh lossless.
- `RobustWatermark`: watermark bền vững trên block (QIM) cho ảnh/screenshot.
- `PdfWatermark`: watermark PDF đa lớp (metadata + robust watermark trên trang render).
- `PerceptualHash`: dHash để tạo fingerprint thị giác phục vụ truy vết screenshot.

### Tầng Backend API

- `AuthController`: đăng ký/đăng nhập/me bằng JWT.
- `ProductController`: catalog, featured, detail, upload file, tạo sản phẩm.
- `OrderController`: checkout, lịch sử đơn, kiểm tra quyền sở hữu.
- `DownloadController`: chỉ cho người đã mua tải file; khi tải sẽ nhúng watermark + tạo fingerprint.
- `WatermarkController`: endpoint verify/extract, kết hợp direct extraction và fingerprint matching.

### Tầng Frontend/Mobile

- Frontend web gọi API qua `src/api.js`.
- Seller có trang verify (`/verify`) và upload (`/upload`).
- User có cart, checkout, library và download.

## 1.3 Luồng dữ liệu cốt lõi

1. User đăng nhập lấy JWT.
2. User mua sản phẩm.
3. Khi user download:
   - backend đọc file gốc trong storage,
   - nhúng watermark cá nhân hóa,
   - sinh và lưu nhiều fingerprint để phục vụ truy vết.
4. Khi admin/seller verify ảnh hoặc file:
   - thử extract trực tiếp trước,
   - nếu không có thì fingerprint matching theo vùng con đa tỉ lệ/đa vị trí.

## 1.4 Điểm mạnh kiến trúc hiện tại

- Tách biệt rõ thuật toán (sdk) và nghiệp vụ (backend).
- Một API hỗ trợ cả watermark trực tiếp và truy vết screenshot.
- Thiết kế multi-strategy extraction giúp tăng tỷ lệ tìm thấy watermark.
- Có thể mở rộng kỹ thuật mà không phá vỡ contract API hiện tại.
