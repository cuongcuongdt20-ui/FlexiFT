package vn.flexift.app.model;

/** Lời đề nghị gửi file từ máy khác (FR-05). Người dùng phải bấm Nhận thì mới có dữ liệu (NFR-05). */
public record IncomingOffer(String transferId, String peerName, String fileName, long size,
                            int chunkCount, TransferMode mode, boolean resumable) {
}
