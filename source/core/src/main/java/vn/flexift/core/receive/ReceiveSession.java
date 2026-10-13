package vn.flexift.core.receive;

import vn.flexift.common.codec.Hashes;
import vn.flexift.common.protocol.Payloads;
import vn.flexift.core.store.ChunkStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Điều phối một phiên nhận ở tầng ứng dụng, chưa phụ thuộc vào socket/UI.
 * Lớp xử lý mạng sẽ ánh xạ OFFER/ACCEPT/CHUNK_ACK/CHUNK_NACK/COMPLETE vào các hàm này.
 */
public final class ReceiveSession implements AutoCloseable {
    public enum ChunkResult {
        ACK, NACK, INVALID, FILE_HASH_MISMATCH
    }

    public record OfferResult(boolean accepted, String reason, String transferId,
                              int[][] missing, String dataToken) {
    }

    public record ChunkResultInfo(ChunkResult result, int index, String reason,
                                  Path completedFile) {
    }

    private final ChunkStore store;
    private final String dataToken;

    private ReceiveSession(ChunkStore store, String dataToken) {
        this.store = store;
        this.dataToken = dataToken;
    }

    /** Mở hoặc khôi phục phiên nhận sau khi người dùng đã đồng ý nhận file. */
    public static ReceiveSession accept(Path receiveDir, Payloads.Offer offer, String dataToken)
            throws IOException {
        if (dataToken == null || dataToken.isBlank()) {
            throw new IOException("dataToken không được để trống");
        }
        return new ReceiveSession(ChunkStore.open(receiveDir, offer), dataToken);
    }

    /**
     * Xử lý một khối đã được ChunkFrame đọc từ kênh dữ liệu.
     * Mã băm sai sẽ trả NACK; khối hợp lệ trả ACK.
     */
    public synchronized ChunkResultInfo receiveChunk(int index, byte[] payload, byte[] sha256)
            throws IOException {
        Objects.requireNonNull(payload, "payload");
        boolean ok;
        try {
            ok = store.writeChunk(index, payload, sha256);
        } catch (IOException e) {
            return new ChunkResultInfo(ChunkResult.INVALID, index, e.getMessage(), null);
        }

        if (!ok) {
            return new ChunkResultInfo(ChunkResult.NACK, index, "SHA-256 khối không khớp", null);
        }

        if (store.hasAllChunks()) {
            Path finalFile = store.complete();
            if (finalFile != null) {
                return new ChunkResultInfo(ChunkResult.ACK, index, "Đã nhận đủ và hash file đúng", finalFile);
            }
            // Không NACK riêng khối cuối: hash toàn file sai không cho biết khối nào hỏng.
            // Tầng giao thức nên gửi COMPLETE {ok:false}, không yêu cầu gửi lại vô hạn một khối.
            return new ChunkResultInfo(ChunkResult.FILE_HASH_MISMATCH, index,
                    "Đủ khối nhưng SHA-256 toàn file không khớp; cần COMPLETE ok=false", null);
        }
        return new ChunkResultInfo(ChunkResult.ACK, index, "Khối hợp lệ", null);
    }

    /** Các khoảng khối còn thiếu để đưa vào Payloads.Accept. */
    public int[][] missingRanges() {
        return store.missingRanges();
    }

    public String dataToken() {
        return dataToken;
    }

    public Payloads.Offer offer() {
        return store.offer();
    }

    public int receivedCount() {
        return store.receivedCount();
    }

    public void checkpoint() throws IOException {
        store.checkpoint();
    }

    @Override
    public void close() throws IOException {
        store.close();
    }

    /** Hàm tiện ích để so sánh hash hex trong OFFER với hash thực tế của file. */
    public static boolean fileHashMatches(Path file, String expectedHex) throws IOException {
        return Hashes.sha256File(file).equalsIgnoreCase(expectedHex);
    }
}
