package vn.flexift.core.send;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
}
