# 6) Vận hành, cấu hình và kiểm thử

## 6.1 Cấu hình môi trường

Nguồn: `backend/src/main/resources/application.yml`

- Port backend: `8080`.
- Upload limit: `500MB`.
- `dev` profile:
  - H2 in-memory,
  - `ddl-auto: create-drop`.
- `prod` profile:
  - PostgreSQL,
  - `ddl-auto: update`.

## 6.2 Chạy dự án local

### Build SDK

- `mvn -pl sdk -am install -DskipTests`

### Chạy backend

- `mvn -f backend/pom.xml spring-boot:run`

### Chạy frontend

- `npm --prefix frontend install`
- `npm --prefix frontend run dev -- --host 0.0.0.0 --port 5173`

## 6.3 Checklist test chức năng chính

1. Login bằng demo account.
2. Mua sản phẩm bất kỳ.
3. Download file từ library.
4. Upload file hoặc screenshot vào trang verify.
5. Xác nhận kết quả `found=true` và payload user/product.

## 6.4 Lưu ý quan trọng khi test watermark

- Với profile dev H2 in-memory, dữ liệu fingerprint mất sau restart backend.
- Sau mỗi lần restart, cần download lại file để sinh fingerprint mới.
- Nếu dùng ảnh chụp quá mờ hoặc crop quá nhỏ, có thể giảm confidence hoặc không match.

## 6.5 Gợi ý test nâng cao

- Test cùng một nội dung ở nhiều giao diện (desktop, mobile, PDF viewer).
- Test ảnh bị nén JPEG mức mạnh.
- Test ảnh có overlay text/UI dày.
- Theo dõi log backend để xem `distance`, `hits`, `source` khi matching.
