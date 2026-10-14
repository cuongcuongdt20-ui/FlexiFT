package vn.flexift.common.protocol;

/** Mã lỗi của giao thức. fatal = gửi ERROR rồi đóng kết nối. */
public enum ErrorCode {
    E_BAD_FRAME(true),
    E_UNSUPPORTED_VERSION(true),
    E_BAD_TOKEN(true),
    E_INVALID_ARGUMENT(false),
    E_UNKNOWN_TRANSFER(false),
    E_BAD_NAME(false),
    E_NO_SPACE(false),
    E_HASH_MISMATCH(false),
    E_BUSY(false),
    E_INTERNAL(false);

    private final boolean fatal;

    ErrorCode(boolean fatal) {
        this.fatal = fatal;
    }

    public boolean isFatal() {
        return fatal;
    }
}
