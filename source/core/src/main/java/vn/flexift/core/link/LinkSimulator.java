package vn.flexift.core.link;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import vn.flexift.core.NodeConfig;

/**
 * Điều tốc và giả lập đường truyền ở bên gửi: giới hạn tốc độ, làm hỏng khối, ngắt kết nối một lần.
 * Khi mọi tham số đều tắt, các phương thức không làm gì.
 */
public final class LinkSimulator {
    private final ByteRateLimiter limiter;
    private final double corruptProbability;
    private final long dropAfterBytes;
    private final Random random;
    private final AtomicLong sent = new AtomicLong();
    private final AtomicBoolean dropped = new AtomicBoolean();

    public LinkSimulator(NodeConfig config) {
        this.limiter = config.rateLimitBytesPerSec > 0 ? new ByteRateLimiter(config.rateLimitBytesPerSec) : null;
        this.corruptProbability = config.corruptProbability;
        this.dropAfterBytes = config.dropAfterBytes;
        this.random = new Random(config.seed);
    }

    /** Chờ cho tới khi được phép gửi chừng này byte. */
    public void throttle(int bytes) throws InterruptedException {
        if (limiter != null) {
            limiter.acquire(bytes);
        }
    }

    /** Đổi giới hạn tốc độ khi đang chạy; chỉ có tác dụng nếu lúc tạo đã bật giới hạn. */
    public void setRate(long bytesPerSecond) {
        if (limiter != null) {
            limiter.setRate(bytesPerSecond);
        }
    }

    /**
     * Theo xác suất đã cấu hình, trả về một bản sao bị lật một byte; nếu không thì trả về chính data.
     * Mã băm được tính trước khi gọi hàm này, nên bên nhận sẽ phát hiện khối hỏng.
     */
    public byte[] maybeCorrupt(byte[] data, int length) {
        if (corruptProbability <= 0 || length == 0) {
            return data;
        }
        int position;
        synchronized (random) {
            if (random.nextDouble() >= corruptProbability) {
                return data;
            }
            position = random.nextInt(length);
        }
        byte[] copy = new byte[length];
        System.arraycopy(data, 0, copy, 0, length);
        copy[position] ^= 0x5A;
        return copy;
    }

    /** Trả về true đúng một lần, khi tổng số byte đã gửi vượt ngưỡng ngắt kết nối. */
    public boolean shouldDrop(int justSent) {
        long total = sent.addAndGet(justSent);
        return dropAfterBytes >= 0 && total >= dropAfterBytes && dropped.compareAndSet(false, true);
    }
}
