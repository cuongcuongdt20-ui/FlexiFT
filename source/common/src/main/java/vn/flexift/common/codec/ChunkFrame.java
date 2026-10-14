package vn.flexift.common.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vn.flexift.common.protocol.ErrorCode;
import vn.flexift.common.protocol.ProtocolException;

/**
 * Khung khối trên kênh dữ liệu:
 * index (4 byte) | length (4 byte) | sha256 (32 byte) | payload (length byte).
 * index = -1 báo kênh không còn gì để gửi; khi đó sau length không còn trường nào.
 */
public record ChunkFrame(int index, byte[] sha256, byte[] payload) {
    public static final int HASH_LENGTH = 32;
    public static final int END = -1;

    /** Ghi một khối; không flush, nơi gọi flush sau mỗi khối. */
    public static void write(DataOutputStream out, int index, byte[] sha256, byte[] payload, int length)
            throws IOException {
        out.writeInt(index);
        out.writeInt(length);
        out.write(sha256, 0, HASH_LENGTH);
        out.write(payload, 0, length);
    }

    public static void writeEnd(DataOutputStream out) throws IOException {
        out.writeInt(END);
        out.writeInt(0);
    }

    /** Trả về null khi gặp khung kết thúc. length vượt maxLength thì coi là vi phạm giao thức. */
    public static ChunkFrame read(DataInputStream in, int maxLength) throws IOException {
        int index = in.readInt();
        int length = in.readInt();
        if (index == END) {
            return null;
        }
        if (index < 0 || length <= 0 || length > maxLength) {
            throw new ProtocolException(ErrorCode.E_BAD_FRAME, "chunk " + index + " length " + length);
        }
        byte[] sha256 = new byte[HASH_LENGTH];
        in.readFully(sha256);
        byte[] payload = new byte[length];
        in.readFully(payload);
        return new ChunkFrame(index, sha256, payload);
    }
}
