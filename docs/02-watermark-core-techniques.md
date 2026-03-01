# 2) Kỹ thuật watermark cốt lõi (LSB, QIM, PDF)

## 2.1 LSB Steganography cho ảnh lossless

Nguồn: `sdk/src/main/java/com/watermark/sdk/LsbSteganography.java`

### Ý tưởng

- Ghi dữ liệu vào 2 bit thấp nhất của kênh Blue mỗi pixel.
- Dùng định dạng payload có header:
  - `MAGIC` (4 byte)
  - `LENGTH` (4 byte)
  - data đã nén Deflate.
- Kết quả xuất PNG để tránh mất dữ liệu bit.

### Ưu điểm

- Dung lượng chứa watermark cao.
- Độ biến đổi thị giác rất thấp.
- Extract nhanh khi file chưa bị tái nén lossy.

### Hạn chế

- JPEG/ảnh nén lossy có thể phá vỡ bit watermark.
- Không tối ưu cho trường hợp screenshot hoặc chỉnh sửa mạnh.

## 2.2 Robust Watermark bằng QIM cho ảnh và PDF render

Nguồn: `sdk/src/main/java/com/watermark/sdk/RobustWatermark.java`

### Ý tưởng

- Chia ảnh thành block theo lưới tương đối (`GRID_COLS`).
- Điều chỉnh trung bình kênh Blue của block theo lượng tử hóa (`QUANT_STEP=4`).
- Mỗi bit được nhúng lặp lại theo vòng để tăng dự phòng.
- Khi extract dùng majority voting để phục hồi bit.

### Vì sao bền vững hơn LSB

- Dữ liệu nằm ở mức thống kê block (average), không phụ thuộc pixel riêng lẻ.
- Chịu được screenshot, scale, JPEG compression tốt hơn.

### Trade-off

- Payload nhỏ hơn LSB thuần.
- Cần tuning block/step theo chất lượng ảnh mục tiêu.

## 2.3 Watermark PDF đa lớp

Nguồn: `sdk/src/main/java/com/watermark/sdk/PdfWatermark.java`

### Layer 1: Metadata

- Ghi chữ ký vào custom metadata (`X-Aura-Sig`).
- Giúp truy vết nhanh khi có file PDF gốc.

### Layer 2: Robust watermark trên trang PDF

- Render từng trang PDF thành ảnh (150 DPI).
- Nhúng watermark robust vào ảnh trang.
- Build lại PDF từ ảnh đã watermark.

### Luồng extract PDF

1. Thử đọc metadata trước (nhanh nhất).
2. Nếu fail, render trang và extract robust watermark.

## 2.4 WatermarkEngine: facade chiến lược

Nguồn: `sdk/src/main/java/com/watermark/sdk/WatermarkEngine.java`

- `embedAuto`: tự chọn đường nhúng theo phần mở rộng file.
- `extractAuto`: với ảnh thử robust trước, rồi fallback LSB.
- Với PDF: ưu tiên extract metadata và robust page extraction.

Kỹ thuật này tạo ra cơ chế multi-strategy giúp tăng tỷ lệ detect trong thực tế.
