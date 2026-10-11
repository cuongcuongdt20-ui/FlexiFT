package vn.flexift.app.model;

/** Một máy đã nối tới (FR-16: danh sách máy đã nối). */
public record PeerInfo(String id, String name, String address) {
    @Override
    public String toString() {
        return name + "  (" + address + ")";
    }
}
