package vn.flexift.common.protocol;

import com.google.gson.JsonObject;

/**
 * Phong bì của mọi message trên kênh điều khiển.
 *
 * @param v  phiên bản giao thức
 * @param t  loại message; null nếu bên gửi dùng một loại mà bên nhận không biết
 * @param id số thứ tự do bên gửi đánh, tăng dần trong một kết nối
 * @param d  dữ liệu riêng của từng loại message; có thể null
 */
public record Envelope(int v, MessageType t, long id, JsonObject d) {
    public static final int VERSION = 1;

    /** id để 0; kết nối sẽ đánh số khi ghi ra socket. */
    public static Envelope of(MessageType t, JsonObject d) {
        return new Envelope(VERSION, t, 0, d);
    }

    public Envelope withId(long newId) {
        return new Envelope(v, t, newId, d);
    }
}
