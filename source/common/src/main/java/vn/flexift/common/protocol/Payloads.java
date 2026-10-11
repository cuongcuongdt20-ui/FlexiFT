package vn.flexift.common.protocol;

/** Các record cho phần dữ liệu "d". Tên thành phần chính là tên trường trong JSON. */
public final class Payloads {
    private Payloads() {
    }

    public record Hello(int protocol, String name) {
    }

    public record Welcome(int protocol, String name) {
    }

    public record Ping(long t0) {
    }

    public record Pong(long t0, long t1) {
    }

    /** Đề nghị gửi một file. mode là SEQUENTIAL, PARALLEL hoặc AUTO (chỉ để hiển thị). */
    public record Offer(String transferId, String name, long size, int chunkSize, int chunkCount,
                        String fileHash, String mode) {
    }

    /** missing là các khoảng khối còn thiếu, mỗi phần tử là [đầu, cuối], tính cả hai đầu. */
    public record Accept(String transferId, int[][] missing, String dataToken) {
    }

    public record Decline(String transferId, String reason) {
    }

    /** Bên gửi báo số kênh dữ liệu mong muốn; máy nối tới mở thêm cho đủ. */
    public record Channels(String transferId, int count) {
    }

    public record ChunkAck(String transferId, int[] indexes) {
    }

    public record ChunkNack(String transferId, int index, String reason) {
    }

    /** Dùng cho PAUSE, RESUME, CANCEL. */
    public record TransferRef(String transferId) {
    }

    public record Complete(String transferId, boolean ok) {
    }

    /** Khung đầu tiên của một kênh dữ liệu. */
    public record DataOpen(String transferId, int channelNo, String dataToken) {
    }

    public record ErrorInfo(String code, String message) {
    }
}
