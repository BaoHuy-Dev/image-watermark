# 7) Giới hạn hiện tại và hướng cải tiến

## 7.1 Giới hạn kỹ thuật hiện tại

1. Fingerprint hiện dựa chủ yếu vào dHash 64-bit.
2. DB fingerprint có thể tăng nhanh theo số trang và số lượt download.
3. H2 in-memory ở dev khiến dữ liệu verify không bền qua restart.
4. Một số endpoint watermark đang public để thuận tiện demo.

## 7.2 Rủi ro vận hành

- Nhiều ảnh có layout giống nhau có thể làm tăng nhiễu matching.
- Nếu content bị cắt cực nhỏ hoặc blur mạnh, tỷ lệ detect giảm.
- Nếu dữ liệu fingerprint quá lớn, truy vấn toàn bảng sẽ tốn chi phí.

## 7.3 Roadmap cải tiến đề xuất

## Giai đoạn 1: Tăng độ tin cậy

- Kết hợp nhiều loại perceptual hash (dHash + pHash/aHash).
- Thêm endpoint debug top-k để QA tuning threshold.
- Lưu thêm metadata cho fingerprint (kiểu crop/tile) để phân tích lỗi.

## Giai đoạn 2: Tối ưu hiệu năng

- Index hóa fingerprint (LSH/ANN) để tìm gần nhất nhanh hơn.
- Batch insert fingerprint và cleanup chiến lược.
- Dùng PostgreSQL extension hoặc vector store cho bài toán near-neighbor.

## Giai đoạn 3: Tăng bảo mật

- Bổ sung role-based access cho verify API.
- Thêm rate limit + chống abuse upload.
- Ký số payload watermark để chống giả mạo dữ liệu truy vết.

## 7.4 Chỉ số nên theo dõi

- Precision/Recall trên tập ảnh thực tế.
- Tỷ lệ false positive theo từng loại giao diện.
- P95 latency của endpoint `/api/watermark/extract`.
- Kích thước bảng `watermark_fingerprints` theo thời gian.
