# FlexiFT

Ứng dụng truyền file tin cậy tự thích ứng trên TCP (Java 21). Bài tập lớn môn Lập trình mạng, Chủ đề 3: Ứng dụng mạng.

> Đây là khung dự án, chưa có mã nguồn. README đầy đủ sẽ được viết khi sản phẩm hoàn thành.

## Cấu trúc

```text
FlexiFT/
├── report/                  báo cáo cuối kỳ
├── statics/results/         CSV và biểu đồ của thực nghiệm
└── source/                  Maven multi-module
    ├── common/              khung tin, message, mã lỗi                       (TV1)
    │   └── vn.flexift.common
    │       ├── protocol/
    │       └── codec/
    ├── core/                toàn bộ phần mạng và truyền file, không có giao diện
    │   └── vn.flexift.core
    │       ├── net/         kênh điều khiển, kênh dữ liệu, chờ và nối tới    (TV1)
    │       ├── link/        giới hạn tốc độ, giả lập đường truyền            (TV1)
    │       ├── store/       kho khối, bảng khối đã nhận, SHA-256             (TV2)
    │       ├── receive/     phiên nhận, tiếp tục khi đứt                     (TV2)
    │       ├── send/        chia khối, gửi tuần tự và song song              (TV3)
    │       └── adaptive/    tự điều chỉnh số kênh                            (TV3)
    ├── app/                 giao diện JavaFX                                 (TV4)
    │   └── vn.flexift.app
    │       └── ui/
    ├── cli/                 công cụ dòng lệnh chạy thực nghiệm, ghi CSV      (TV4)
    ├── package-app.bat      đóng gói thành FlexiFT.exe kèm Java (Windows)
    └── run-experiments.bat  chạy các thực nghiệm
```

## Môi trường

- Java 21 trở lên
- Maven 3.9 trở lên

```bash
cd source
mvn clean verify
```

Lệnh trên chạy được ngay trên khung rỗng và tải sẵn các thư viện về máy.

## Thư viện ngoài

- Gson 2.11.0: đọc ghi JSON
- JavaFX 21.0.5: giao diện
- JUnit 5.11.0: kiểm thử

## Lớp chính cần đặt đúng tên

- `vn.flexift.app.Launcher`: lớp `main` của `app/target/flexift.jar`
- `vn.flexift.cli.Main`: lớp `main` của `cli/target/flexift-cli.jar`
