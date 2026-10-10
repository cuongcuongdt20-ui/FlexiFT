package vn.flexift.app.model;

/** Chế độ truyền (FR-06, FR-07, FR-08). Tên hằng số trùng với trường "mode" trong message OFFER. */
public enum TransferMode {
    SEQUENTIAL("Tuần tự (1 kênh)"),
    PARALLEL("Song song (2–8 kênh)"),
    AUTO("Tự động");

    private final String label;

    TransferMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
