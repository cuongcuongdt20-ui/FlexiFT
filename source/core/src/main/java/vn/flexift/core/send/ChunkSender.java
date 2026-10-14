package vn.flexift.core.send;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import vn.flexift.common.codec.ChunkFrame;
import vn.flexift.common.codec.Hashes;

public class ChunkSender {
    public static void sendOne(FileChannel ch, int chunkSize, int index, DataOutputStream out) throws IOException {
        byte[] payload = ChunkReader.readChunk(ch, index, chunkSize);
        if (payload.length == 0) {
            return;
        }
        byte[] sha = Hashes.sha256(payload, 0, payload.length);
        ChunkFrame.write(out, index, sha, payload, payload.length);
        out.flush();
    }

    public static void sendChunks(FileChannel ch, int chunkSize, int[] indexes, DataOutputStream out) throws IOException {
        if (indexes == null) {
            throw new IllegalArgumentException("Mang chi so rong");
        }
        for (int i : indexes) {
            sendOne(ch, chunkSize, i, out);
        }
        ChunkFrame.writeEnd(out);
        out.flush();
    }

    public static void sendParallel(FileChannel ch, int chunkSize, int[] indexes, DataOutputStream[] outs) throws IOException, InterruptedException {
        if (outs == null || outs.length == 0) {
            throw new IllegalArgumentException("Dau vao rong");
        }
        if (indexes == null) {
            throw new NullPointerException("So khoi rong");
        }
        BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
        for (int i : indexes) {
            q.put(i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(outs.length);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < outs.length; i++) {
                final int t = i;
                Future<?> f = pool.submit(() -> {
                    try {
                        while (true) {
                            Integer n = q.poll();
                            if (n == null) break;
                            ChunkSender.sendOne(ch, chunkSize, n, outs[t]);
                        }
                        ChunkFrame.writeEnd(outs[t]);
                        outs[t].flush();
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                futures.add(f);
            } 
            for (Future<?> i : futures) {
                try {
                    i.get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof UncheckedIOException) {
                        throw ((UncheckedIOException) e.getCause()).getCause();
                    }
                    throw new RuntimeException(e.getCause());
                }
            } 
        } finally {
            pool.shutdownNow();
        }
    }
}
