package vn.flexift.core.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import vn.flexift.core.NodeConfig;

class ByteRateLimiterTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void sendingFasterThanTheRateMustWait() {
        ByteRateLimiter limiter = new ByteRateLimiter(1_000);          // 1000 byte mỗi giây
        long start = System.nanoTime();

        long wait = limiter.reserve(2_000, start);                     // gửi 2000 byte ngay lập tức
        assertTrue(wait > 1.9 * SECOND && wait < 2.1 * SECOND, "phải chờ khoảng 2 giây, thực tế " + wait);
    }

    @Test
    void sendingAtTheRateNeedsNoExtraWait() {
        ByteRateLimiter limiter = new ByteRateLimiter(1_000);
        long now = System.nanoTime();
        limiter.reserve(100, now);
        // 100 byte mỗi 0,1 giây là đúng bằng tốc độ cho phép: món nợ không tăng lên
        long wait = 0;
        for (int i = 1; i <= 20; i++) {
            wait = limiter.reserve(100, now + i * SECOND / 10);
        }
        assertTrue(wait <= SECOND / 10 + 1_000_000, "chờ không quá một nhịp, thực tế " + wait);
    }

    @Test
    void idleTimeDoesNotBuildAHugeBurst() {
        ByteRateLimiter limiter = new ByteRateLimiter(1_000);
        long now = System.nanoTime();
        // Nghỉ 100 giây rồi gửi 1000 byte: chỉ được dùng trước 1/4 giây lưu lượng, phần còn lại phải chờ
        long wait = limiter.reserve(1_000, now + 100 * SECOND);
        assertTrue(wait > 0.7 * SECOND && wait < 0.8 * SECOND, "thực tế " + wait);
    }

    @Test
    void simulatorDropsExactlyOnce() {
        NodeConfig config = new NodeConfig();
        config.dropAfterBytes = 1_000;
        LinkSimulator simulator = new LinkSimulator(config);

        assertEquals(false, simulator.shouldDrop(600));
        assertEquals(true, simulator.shouldDrop(600));                 // tổng 1200, vượt ngưỡng
        assertEquals(false, simulator.shouldDrop(600));                // chỉ ngắt một lần
    }

    @Test
    void corruptionChangesACopyNotTheOriginal() {
        NodeConfig config = new NodeConfig();
        config.corruptProbability = 1.0;
        LinkSimulator simulator = new LinkSimulator(config);
        byte[] original = new byte[64];

        byte[] sent = simulator.maybeCorrupt(original, original.length);
        assertTrue(sent != original, "phải là bản sao");
        int different = 0;
        for (int i = 0; i < original.length; i++) {
            assertEquals(0, original[i]);
            if (sent[i] != original[i]) {
                different++;
            }
        }
        assertEquals(1, different);
    }
}
