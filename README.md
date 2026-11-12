# QLPhongTro

## Yêu cầu môi trường

- JDK 24 để chạy Maven và bộ kiểm thử. Mã nguồn vẫn biên dịch với `--release 17`.
- Apache Maven 3.6.3 trở lên.
- SQL Server để chạy ứng dụng với cơ sở dữ liệu được cấu hình trong `src/main/resources/application.properties`.

Maven phải chạy bằng JDK 24. Byte Buddy được nâng lên phiên bản hỗ trợ instrument trên JDK 24; Java target của ứng dụng vẫn là 17. Trên Windows PowerShell, đặt JDK 24 cho phiên terminal hiện tại trước khi build và kiểm tra:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -v
mvn clean test
```

Kiểm tra đầu ra của `java -version` và `mvn -v` để xác nhận cả hai sử dụng Java 24. Nếu đã cài JDK 24 ở vị trí khác, thay đường dẫn `JAVA_HOME` tương ứng.
