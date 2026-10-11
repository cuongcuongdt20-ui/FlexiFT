package vn.flexift.common.protocol;

import java.io.IOException;

/** Ném ra khi dữ liệu nhận được vi phạm giao thức. */
public class ProtocolException extends IOException {
    private final ErrorCode code;

    public ProtocolException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
