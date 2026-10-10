package vn.flexift.app.model;

public enum TransferState {
    WAITING_ACCEPT("Chờ bên kia nhận"),
    RUNNING("Đang truyền"),
    PAUSED("Tạm dừng"),
    COMPLETED("Hoàn tất"),
    FAILED("Lỗi"),
    CANCELLED("Đã hủy"),
    DECLINED("Bị từ chối");

    private final String label;

    TransferState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Phiên đã kết thúc, không còn thay đổi nữa. */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == DECLINED;
    }
}
