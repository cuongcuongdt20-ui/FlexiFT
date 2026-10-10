package vn.flexift.app.util;

/** Địa chỉ người dùng nhập: "192.168.1.5", "192.168.1.5:7777" hoặc "may-b.local:7777". */
public record HostPort(String host, int port) {
    public static final int DEFAULT_PORT = 7777;

    /** @throws IllegalArgumentException với thông báo tiếng Việt, hiển thị thẳng cho người dùng */
    public static HostPort parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Chưa nhập địa chỉ máy cần nối tới.");
        }
        String s = text.strip();
        int colon = s.lastIndexOf(':');
        // Chỉ coi là "host:port" khi có đúng một dấu hai chấm (IPv6 có nhiều dấu hai chấm: giữ nguyên, dùng cổng mặc định).
        if (colon < 0 || colon != s.indexOf(':')) {
            return new HostPort(s, DEFAULT_PORT);
        }
        String host = s.substring(0, colon).strip();
        String portText = s.substring(colon + 1).strip();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Thiếu địa chỉ trước dấu hai chấm.");
        }
        return new HostPort(host, parsePort(portText));
    }

    public static int parsePort(String text) {
        try {
            int p = Integer.parseInt(text.strip());
            if (p < 1 || p > 65535) {
                throw new NumberFormatException();
            }
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cổng phải là số từ 1 đến 65535.");
        }
    }
}
