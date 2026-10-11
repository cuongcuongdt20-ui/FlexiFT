## Cấu trúc (source/app, package vn.flexift.app)
- `Launcher` (main của flexift.jar), `MainApp`
- `model/` PeerInfo, IncomingOffer, TransferSnapshot, TransferMode, TransferState, Direction
- `service/` **TransferService** (interface giao diện↔lõi), TransferListener, FakeTransferService, ServiceFactory
- `ui/` MainWindow, UiBridge
- `util/` Formatters, HostPort
- `resources/flexift.css`

# CÁC FILE CHỈ ĐỂ DEMO GIAO DIỆN
- service/FakeTransferService : mn ai làm phần service thì sửa lại file này
- service/ServiceFactory : sửa cả hàm mà return trỏ đến (
    
    public static TransferService create() {
        return new FakeTransferService();
    }
)

## Cách chạy
```bash
cd FlexiFT\source
mvn clean verify
java -jar app/target/flexift.jar
```
Trên cửa sổ: bấm "Chờ kết nối" (sau 3 giây có máy giả nối tới, sau 2 giây nữa có lời đề nghị gửi file),
 hoặc nhập địa chỉ bất kỳ rồi "Nối tới" (gõ `loi` để thử trạng thái lỗi). Menu Công cụ có "Giả lập: có file gửi đến".


