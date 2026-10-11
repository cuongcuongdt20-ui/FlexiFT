package vn.flexift.common.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vn.flexift.common.protocol.ErrorCode;
import vn.flexift.common.protocol.ProtocolException;

/** Cắt luồng byte TCP thành từng khung điều khiển: 4 byte độ dài (big-endian) + thân JSON. */
public final class FrameCodec {
    public static final int MAX_FRAME = 64 * 1024;

    private FrameCodec() {
    }

    public static void write(DataOutputStream out, byte[] body) throws IOException {
        if (body.length == 0 || body.length > MAX_FRAME) {
            throw new ProtocolException(ErrorCode.E_BAD_FRAME, "frame length " + body.length);
        }
        out.writeInt(body.length);
        out.write(body);
        out.flush();
    }

    /** Chặn cho tới khi đọc đủ một khung. Ném EOFException khi bên kia đóng kết nối. */
    public static byte[] read(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len <= 0 || len > MAX_FRAME) {
            throw new ProtocolException(ErrorCode.E_BAD_FRAME, "frame length " + len);
        }
        byte[] body = new byte[len];
        in.readFully(body);
        return body;
    }
}
