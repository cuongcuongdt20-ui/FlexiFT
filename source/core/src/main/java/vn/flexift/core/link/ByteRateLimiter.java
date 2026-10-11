package vn.flexift.core.link;

import java.util.concurrent.TimeUnit;

/**
 * Token bucket tính theo byte: mỗi giây nạp bytesPerSecond thẻ, mỗi byte gửi đi tiêu một thẻ.
 * Thiếu thẻ thì luồng gọi ngủ cho tới khi đủ. Nhiều luồng dùng chung được.
 */
public final class ByteRateLimiter {
    /** Xô chỉ chứa tối đa 1/4 giây lưu lượng, để không có đợt gửi dồn lớn. */
    private static final double BURST_SECONDS = 0.25;

    private double bytesPerSecond;
    private double tokens;
    private long lastNanos;

    public ByteRateLimiter(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
        this.lastNanos = System.nanoTime();
    }

    public void acquire(int bytes) throws InterruptedException {
        long waitNanos = reserve(bytes, System.nanoTime());
        if (waitNanos > 0) {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        }
    }

    public synchronized void setRate(long newBytesPerSecond) {
        this.bytesPerSecond = newBytesPerSecond;
    }

    /**
     * Trừ thẻ và trả về số nano giây phải chờ. Tách riêng để kiểm thử với đồng hồ giả.
     * Thẻ được phép âm: đó là món nợ mà lần gọi này trả bằng cách chờ.
     */
    synchronized long reserve(int bytes, long nowNanos) {
        double elapsedSeconds = (nowNanos - lastNanos) / 1_000_000_000.0;
        tokens = Math.min(bytesPerSecond * BURST_SECONDS, tokens + elapsedSeconds * bytesPerSecond);
        lastNanos = nowNanos;
        tokens -= bytes;
        return tokens >= 0 ? 0 : (long) (-tokens / bytesPerSecond * 1_000_000_000.0);
    }
}
