package vn.flexift.app.model;

public enum Direction {
    SEND("Gửi"),
    RECEIVE("Nhận");

    private final String label;

    Direction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
