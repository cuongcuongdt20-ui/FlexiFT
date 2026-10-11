package vn.flexift.app.model;

/**
 * Ảnh chụp trạng thái một phiên truyền; bất biến nên luồng mạng có thể đưa thẳng cho giao diện.
 *
 * @param bytesDone  số byte đã được xác nhận
 * @param speedBps   thông lượng hiện tại, byte/giây
 * @param channels   số kênh dữ liệu đang dùng
 * @param message    ghi chú ngắn (lý do lỗi, từ chối...), có thể null
 */
public record TransferSnapshot(String transferId, String fileName, long size, Direction direction,
                               String peerName, TransferMode mode, TransferState state,
                               long bytesDone, double speedBps, int channels, String message) {

    public double fraction() {
        if (size <= 0) {
            return state == TransferState.COMPLETED ? 1.0 : 0.0;
        }
        return Math.min(1.0, (double) bytesDone / size);
    }

    public TransferSnapshot with(TransferState newState, long newBytes, double newSpeed, int newChannels, String newMessage) {
        return new TransferSnapshot(transferId, fileName, size, direction, peerName, mode,
                newState, newBytes, newSpeed, newChannels, newMessage);
    }
}
