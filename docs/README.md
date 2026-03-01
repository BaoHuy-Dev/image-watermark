# Bộ tài liệu kỹ thuật dự án Image Watermark

Bộ tài liệu này mô tả chi tiết các kỹ thuật đã áp dụng trong dự án, theo đúng trạng thái code hiện tại.

## Mục lục

1. [Tổng quan kiến trúc](01-kien-truc-tong-quan.md)
2. [Kỹ thuật watermark cốt lõi (LSB, QIM, PDF)](02-watermark-core-techniques.md)
3. [Kỹ thuật fingerprint & dò ảnh chụp màn hình](03-fingerprint-screen-matching.md)
4. [Bảo mật, xác thực và phân quyền](04-security-authz.md)
5. [Luồng nghiệp vụ end-to-end](05-business-flow-end-to-end.md)
6. [Vận hành, cấu hình và kiểm thử](06-operations-testing.md)
7. [Giới hạn hiện tại và hướng cải tiến](07-limitations-roadmap.md)

## Đối tượng đọc

- Dev backend/frontend/mobile
- QA kiểm thử watermark
- Tech lead cần nắm kiến trúc và thuật toán
- DevOps cần triển khai môi trường dev/prod

## Gợi ý cách đọc

- Nếu mới vào dự án: đọc 1 → 2 → 5.
- Nếu tập trung screenshot detection: đọc 2 → 3 → 6.
- Nếu triển khai production: đọc 1 → 4 → 6 → 7.
