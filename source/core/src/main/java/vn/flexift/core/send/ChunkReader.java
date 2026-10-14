package vn.flexift.core.send;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChunkReader {
    public static void main(String[] args) throws IOException, InterruptedException {
        BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
        for (int i = 1; i <= 20; i++) {
            q.put(i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(3);
        Queue<Integer> daLay = new ConcurrentLinkedQueue<>();
        for (int t = 0; t < 3; t++) {
            pool.submit(() -> {
                while (true) {
                    Integer n = q.poll();
                    if (n == null) break;
                    System.out.println(Thread.currentThread().getName() + " " + n);
                    daLay.add(n);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        System.out.println(daLay.size());
    }

    public static byte[] readChunk(FileChannel ch, int index, int chunkSize) throws IOException {
        if (index < 0) {
            throw new IllegalArgumentException("Chi so bang dau tu 0: " + index);
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Kich thuoc khoi phai lon hon 0: " + chunkSize);
        }

        long pos = (long) index * chunkSize;
        int soKhoi = Chunker.chunkCount(ch.size(), chunkSize);

        if (index >= soKhoi) {
            throw new IllegalArgumentException("Chi so vuot qua so khoi: " + index);
        }

        int canDoc = (int)Math.min(chunkSize, ch.size() - pos); 
        ByteBuffer buf = ByteBuffer.allocate(canDoc);
        
        long currentPos = pos;
        while (buf.hasRemaining()) {
            int n = ch.read(buf, currentPos);
            if (n == -1) {
                throw new IOException("File bi ngat quang: " + n);
            };
            if (n == 0) {
                Thread.yield();
            }
            currentPos += n;
        }
        return Arrays.copyOf(buf.array(),buf.position() );
    }
}
