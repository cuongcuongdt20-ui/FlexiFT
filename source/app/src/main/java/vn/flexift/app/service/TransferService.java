package vn.flexift.app.service;

import vn.flexift.app.model.TransferMode;

import java.nio.file.Path;

/**
 * Điểm nối duy nhất giữa giao diện (TV4) và lõi (TV1, TV2, TV3).
 *
 * Giao diện chỉ biết interface này. Hiện dùng {@link FakeTransferService} (dữ liệu giả, mục 08–10/10);
 * khi lõi chạy được (14/10) viết một lớp CoreTransferService implements TransferService gọi vào
 * vn.flexift.core rồi đổi MỘT dòng trong {@link ServiceFactory}.
 *
 * Mọi hàm trả về ngay, không chặn luồng giao diện; kết quả báo qua {@link TransferListener}.
 */
public interface TransferService extends AutoCloseable {

    void setListener(TransferListener listener);

    /** FR-01: mở cổng chờ và báo địa chỉ của máy mình qua onListening. */
    void startListening(int port);

    void stopListening();

    /** FR-01: nối tới máy đang chờ. Thành công thì onPeerConnected, thất bại thì onError. */
    void connectTo(String host, int port);

    void disconnect(String peerId);

    /**
     * FR-04, FR-06..08: gửi file cho một máy. {@code channels} chỉ có nghĩa khi mode = PARALLEL (2..8).
     */
    void sendFile(String peerId, Path file, TransferMode mode, int channels);

    /** FR-05: người dùng bấm Nhận; file lưu vào {@code saveDir}. */
    void accept(String transferId, Path saveDir);

    /** FR-05: người dùng bấm Từ chối. */
    void decline(String transferId);

    /** FR-09. */
    void pause(String transferId);

    void resume(String transferId);

    void cancel(String transferId);

    /** FR-14: giới hạn tốc độ gửi theo byte/giây; 0 = không giới hạn. */
    void setSpeedLimit(long bytesPerSecond);

    /** Đặt tên máy hiển thị cho bên kia (trao trong HELLO/WELCOME). */
    void setDeviceName(String name);

    @Override
    void close();
}
