# QLPhongTro

## Yêu cầu môi trường

- JDK 17 để chạy Maven, biên dịch và kiểm thử.
- Apache Maven 3.6.3 trở lên.
- SQL Server để chạy ứng dụng với cơ sở dữ liệu được cấu hình trong `src/main/resources/application.properties`.

Maven Enforcer kiểm tra phiên bản JDK trong phase `validate` và từ chối các phiên bản ngoài `[17,18)`. Trên Windows PowerShell, đặt JDK 17 cho phiên terminal hiện tại trước khi build và kiểm tra:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -v
mvn clean verify
```

Kiểm tra đầu ra của `java -version` và `mvn -v` để xác nhận cả hai sử dụng Java 17. Nếu đã cài JDK 17 ở vị trí khác, thay đường dẫn `JAVA_HOME` tương ứng. File `.java-version` cũng khai báo phiên bản yêu cầu cho các công cụ quản lý Java hỗ trợ file này.
