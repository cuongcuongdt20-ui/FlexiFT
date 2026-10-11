package vn.flexift.common.codec;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import vn.flexift.common.protocol.Envelope;
import vn.flexift.common.protocol.ErrorCode;
import vn.flexift.common.protocol.ProtocolException;

/** Chuyển Envelope thành byte JSON UTF-8 và ngược lại. */
public final class JsonCodec {
    private static final Gson GSON = new Gson();

    private JsonCodec() {
    }

    public static byte[] encode(Envelope envelope) {
        return GSON.toJson(envelope).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Thân khung không phải JSON hợp lệ thì ném E_BAD_FRAME.
     * Loại message lạ không ném lỗi: Gson để t = null, nơi gọi tự quyết định.
     */
    public static Envelope decode(byte[] body) throws ProtocolException {
        try {
            Envelope envelope = GSON.fromJson(new String(body, StandardCharsets.UTF_8), Envelope.class);
            if (envelope == null) {
                throw new ProtocolException(ErrorCode.E_BAD_FRAME, "empty body");
            }
            return envelope;
        } catch (JsonParseException e) {
            throw new ProtocolException(ErrorCode.E_BAD_FRAME, "malformed JSON");
        }
    }

    /** Đọc phần d thành một record payload, ví dụ fromData(msg.d(), Payloads.Offer.class). */
    public static <T> T fromData(JsonObject d, Class<T> type) throws ProtocolException {
        try {
            T value = d == null ? null : GSON.fromJson(d, type);
            if (value == null) {
                throw new ProtocolException(ErrorCode.E_INVALID_ARGUMENT, "missing data");
            }
            return value;
        } catch (JsonParseException e) {
            throw new ProtocolException(ErrorCode.E_INVALID_ARGUMENT, "bad data");
        }
    }

    /** Đổi một record payload thành phần d; payload null thì trả về null. */
    public static JsonObject toData(Object payload) {
        return payload == null ? null : GSON.toJsonTree(payload).getAsJsonObject();
    }

    public static Gson gson() {
        return GSON;
    }
}
