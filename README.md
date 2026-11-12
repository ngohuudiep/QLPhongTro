# QLPhongTro

## Yêu cầu môi trường

- JDK 17 để chạy Maven, biên dịch và kiểm thử.
- Apache Maven 3.6.3 trở lên.
- SQL Server để chạy ứng dụng với cơ sở dữ liệu được cấu hình trong `src/main/resources/application.properties`.

Maven Enforcer kiểm tra phiên bản JDK trong phase `validate` và từ chối các phiên bản ngoài `[17,18)`. Trên Windows, dùng wrapper để tìm JDK 17 và chỉ đặt `JAVA_HOME`/`PATH` trong phạm vi tiến trình Maven:

```cmd
scripts\mvn17.cmd -v
scripts\mvn17.cmd clean test
```

Trong PowerShell có thể dùng `.\scripts\mvn17.ps1 -v` hoặc `.\scripts\mvn17.ps1 clean test`. Nếu JDK 17 nằm ngoài các thư mục dò tự động, đặt biến `JDK17_HOME` cho phiên terminal hiện tại trước khi gọi wrapper. Nếu không tìm thấy JDK 17, wrapper in hướng dẫn cài Temurin 17 và thoát với mã lỗi.

JDK 24 bị chặn có chủ đích: dự án yêu cầu JDK 17, đây là bản LTS đã khai báo làm baseline; JDK 24 không phải bản LTS và Spring Boot 3.2.5 không công bố hỗ trợ chính thức cho JDK 24. File `.java-version` cũng khai báo phiên bản yêu cầu cho các công cụ quản lý Java hỗ trợ file này.
