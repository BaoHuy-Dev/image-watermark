# 3) Kỹ thuật fingerprint & dò ảnh chụp màn hình

## 3.1 Mục tiêu

Bài toán thực tế: ảnh bị chụp trong nhiều giao diện khác nhau (browser, app mobile, thanh status/navigation, vùng crop nhỏ) vẫn cần truy ra chủ sở hữu.

Giải pháp áp dụng: fingerprint thị giác + so khớp vùng con đa tỉ lệ + voting theo nhóm.

## 3.2 dHash (PerceptualHash)

Nguồn: `sdk/src/main/java/com/watermark/sdk/PerceptualHash.java`

### Quy trình

1. Resize ảnh về 9x8 grayscale.
2. So sánh từng pixel với pixel bên phải.
3. Sinh hash 64 bit (hex 16 ký tự).
4. So sánh bằng Hamming distance.

### Ý nghĩa

- Hash gần nhau khi nội dung thị giác tương tự.
- Bền với scale, nén JPEG, thay đổi định dạng.

## 3.3 Sinh fingerprint tại thời điểm download

Nguồn: `backend/.../DownloadController.java`

Khi user download file đã mua:

1. Backend nhúng watermark cá nhân hóa.
2. Sinh nhiều hash cho mỗi trang/ảnh bằng hàm `collectHashes`:
   - full frame,
   - crop theo nhiều tỉ lệ diện tích,
   - crop theo nhiều aspect ratio,
   - nhiều anchor vị trí,
   - sliding tiles ở nhiều kích thước.
3. Lưu vào bảng `watermark_fingerprints` với thông tin user/product/page.
4. Trước khi lưu mới, xóa fingerprint cũ theo cặp user-product để tránh nhiễu.

## 3.4 Matching tại endpoint verify/extract

Nguồn: `backend/.../WatermarkController.java`

Khi nhận ảnh upload để verify:

1. Tạo `uploadedCandidates` (nhiều hash vùng con tương tự logic ở download).
2. Với mỗi candidate:
   - tìm fingerprint gần nhất và gần nhì trong DB,
   - áp điều kiện ambiguity để giảm false positive.
3. Cộng điểm theo nhóm khóa:
   - `(userId, userEmail, productId, productTitle, pageNumber)`.
4. Chọn nhóm có score cao nhất, so với nhóm thứ hai (`GROUP_SCORE_GAP`).
5. Chấp nhận nếu đạt điều kiện hits hoặc có best distance rất thấp.
6. Trả về:
   - `method=fingerprint` hoặc `fingerprint-subregion`,
   - `confidence`, `distance`, `hits`, `source`.

## 3.5 Vì sao xử lý được ảnh điện thoại và ảnh rộng

- Multi-crop + multi-tile giúp bắt được phần nội dung dù chỉ xuất hiện một phần nhỏ trong ảnh lớn.
- Group voting ngăn trường hợp một hash đơn lẻ match nhầm.
- Ambiguity gap giảm rủi ro khi nhiều nội dung giống nhau.

## 3.6 Rủi ro và kiểm soát

### Rủi ro

- False positive khi nội dung quá phổ biến/ít đặc trưng.
- False negative nếu vùng chứa nội dung quá nhỏ, quá mờ, hoặc bị chỉnh sửa nặng.

### Biện pháp hiện có

- Ngưỡng khoảng cách + ngưỡng tách top1/top2.
- Điều kiện số hit tối thiểu theo nhóm.
- Hard accept với khoảng cách rất thấp.

### Hướng tăng độ chắc chắn

- Lưu thêm fingerprint theo pyramid scale nhiều mức DPI.
- Dùng ensemble hash (dHash + pHash/aHash).
- Kết hợp feature local descriptor (ORB/SIFT) cho bước re-check cuối.
