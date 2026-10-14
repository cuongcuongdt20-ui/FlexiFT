package vn.flexift.core;

import java.net.InetAddress;
import java.nio.file.Path;

/** Cấu hình của một máy. Đặt các trường trước khi tạo FlexiNode. */
public final class NodeConfig {
    /** Tên hiển thị cho máy bên kia. */
    public String name = defaultName();
    /** Thư mục lưu file nhận về. */
    public Path receiveDir = Path.of(System.getProperty("user.home"), "Downloads", "FlexiFT");
    /** Số kênh dữ liệu tối đa cho một phiên. */
    public int maxChannels = 8;
    /** Khác 0 thì dùng kích thước khối này thay cho bảng mặc định (dùng trong kiểm thử). */
    public int chunkSizeOverride = 0;

    /** Chế độ tự động: độ dài cửa sổ đo thông lượng. */
    public long adaptWindowMs = 2_000;
    /** Chế độ tự động: thời gian giữ nguyên sau một lần thử không có lợi. */
    public long adaptHoldMs = 10_000;

    /** Tự nhận mọi file mà không hỏi (công cụ dòng lệnh và kiểm thử). */
    public boolean autoAccept = false;
    /** Máy nối tới tự nối lại khi mất kết nối giữa chừng. */
    public boolean autoReconnect = true;
    public long reconnectDelayMs = 1_000;
    public int reconnectAttempts = 15;

    public int heartbeatMs = 5_000;
    public int idleTimeoutMs = 15_000;

    // --- điều tốc và giả lập đường truyền (0 hoặc -1 = tắt) ---
    /** Giới hạn tốc độ gửi, byte mỗi giây. */
    public long rateLimitBytesPerSec = 0;
    /** Xác suất làm hỏng một khối trước khi gửi. */
    public double corruptProbability = 0;
    /** Ngắt kết nối một lần sau khi đã gửi chừng này byte. */
    public long dropAfterBytes = -1;
    /** Hạt giống cho phần ngẫu nhiên của bộ giả lập, để thực nghiệm lặp lại được. */
    public long seed = 42;

    /** Kích thước khối theo dung lượng file. */
    public int chunkSizeFor(long fileSize) {
        if (chunkSizeOverride > 0) {
            return chunkSizeOverride;
        }
        if (fileSize < 64L * 1024 * 1024) {
            return 256 * 1024;
        }
        if (fileSize <= 1024L * 1024 * 1024) {
            return 1024 * 1024;
        }
        return 4 * 1024 * 1024;
    }

    private static String defaultName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "may-" + (System.nanoTime() % 10_000);
        }
    }
}
