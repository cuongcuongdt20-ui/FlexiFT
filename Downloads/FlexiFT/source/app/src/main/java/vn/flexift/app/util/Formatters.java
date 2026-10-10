package vn.flexift.app.util;

import java.util.Locale;

/** Định dạng số liệu hiển thị trên giao diện. Không phụ thuộc JavaFX nên test được. */
public final class Formatters {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    private Formatters() {
    }

    public static String bytes(long n) {
        if (n < 0) {
            return "—";
        }
        double v = n;
        int u = 0;
        while (v >= 1024 && u < UNITS.length - 1) {
            v /= 1024;
            u++;
        }
        return u == 0 ? n + " B" : String.format(VI, "%.1f %s", v, UNITS[u]);
    }

    public static String speed(double bytesPerSecond) {
        if (bytesPerSecond <= 0) {
            return "—";
        }
        return bytes((long) bytesPerSecond) + "/s";
    }

    /** Thời gian còn lại ước tính, dạng m:ss hoặc h:mm:ss; "—" nếu chưa tính được. */
    public static String eta(long remainingBytes, double bytesPerSecond) {
        if (remainingBytes <= 0 || bytesPerSecond <= 0) {
            return "—";
        }
        long s = Math.round(remainingBytes / bytesPerSecond);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
                : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }

    public static String percent(double fraction) {
        return String.format(VI, "%.0f%%", Math.max(0, Math.min(1, fraction)) * 100);
    }

    /**
     * Tên file chỉ để HIỂN THỊ: bỏ mọi đường dẫn (cả / và \). Việc chặn thoát thư mục nhận thật sự
     * nằm ở bên nhận (NFR-05, TV2); giao diện chỉ tránh hiện tên có đường dẫn gây hiểu lầm.
     */
    public static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "(không tên)";
        }
        int i = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String n = i >= 0 ? name.substring(i + 1) : name;
        return n.isBlank() ? "(không tên)" : n;
    }
}
