package vn.flexift.core.receive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.flexift.common.codec.Hashes;
import vn.flexift.common.protocol.Payloads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveSessionTest {
    @TempDir
    Path tempDir;

    private static Payloads.Offer offer(String name, byte[] content, int chunkSize) {
        String hash = Hashes.hex(Hashes.sha256(content, 0, content.length));
        int count = content.length == 0 ? 1 : (content.length + chunkSize - 1) / chunkSize;
        String transferId = Hashes.transferId(hash, content.length, name);
        return new Payloads.Offer(transferId, name, content.length, chunkSize, count, hash, "SEQUENTIAL");
    }

    private static byte[] slice(byte[] content, int chunkSize, int index) {
        int start = index * chunkSize;
        int length = Math.min(chunkSize, content.length - start);
        return Arrays.copyOfRange(content, start, start + length);
    }

    @Test
    void acceptsOfferAndInitiallyReportsAllChunksMissing() throws IOException {
        byte[] content = "abcdef".getBytes();
        Payloads.Offer offer = offer("sample.txt", content, 2);

        try (ReceiveSession session = ReceiveSession.accept(tempDir, offer, "token-123")) {
            assertEquals("token-123", session.dataToken());
            assertEquals(offer, session.offer());
            assertEquals(0, session.receivedCount());
            assertArrayEquals(new int[][]{{0, 2}}, session.missingRanges());
        }
    }

    @Test
    void acknowledgesValidChunksAndCompletesWhenWholeFileHashMatches() throws IOException {
        byte[] content = "abcdef".getBytes();
        Payloads.Offer offer = offer("sample.txt", content, 2);

        try (ReceiveSession session = ReceiveSession.accept(tempDir, offer, "token")) {
            ReceiveSession.ChunkResultInfo first = receive(session, content, 2, 0);
            ReceiveSession.ChunkResultInfo third = receive(session, content, 2, 2);
            ReceiveSession.ChunkResultInfo last = receive(session, content, 2, 1);

            assertEquals(ReceiveSession.ChunkResult.ACK, first.result());
            assertEquals(ReceiveSession.ChunkResult.ACK, third.result());
            assertEquals(ReceiveSession.ChunkResult.ACK, last.result());
            assertNotNull(last.completedFile());
            assertTrue(Files.exists(last.completedFile()));
            assertArrayEquals(content, Files.readAllBytes(last.completedFile()));
        }
    }

    @Test
    void rejectsChunkWithIncorrectSha256() throws IOException {
        byte[] content = "abcd".getBytes();
        Payloads.Offer offer = offer("sample.txt", content, 2);

        try (ReceiveSession session = ReceiveSession.accept(tempDir, offer, "token")) {
            byte[] payload = slice(content, 2, 0);
            byte[] wrongHash = Hashes.sha256(payload, 0, payload.length);
            wrongHash[0] ^= 0x7f;

            ReceiveSession.ChunkResultInfo result = session.receiveChunk(0, payload, wrongHash);
            assertEquals(ReceiveSession.ChunkResult.NACK, result.result());
            assertEquals(0, session.receivedCount());
            assertArrayEquals(new int[][]{{0, 1}}, session.missingRanges());
        }
    }

    @Test
    void resumesSessionAndReportsOnlyMissingChunks() throws IOException {
        byte[] content = "abcdef".getBytes();
        Payloads.Offer offer = offer("sample.txt", content, 2);

        try (ReceiveSession first = ReceiveSession.accept(tempDir, offer, "token-1")) {
            ReceiveSession.ChunkResultInfo result = receive(first, content, 2, 0);
            assertEquals(ReceiveSession.ChunkResult.ACK, result.result());
            first.checkpoint();
        }

        try (ReceiveSession resumed = ReceiveSession.accept(tempDir, offer, "token-2")) {
            assertEquals(1, resumed.receivedCount());
            assertArrayEquals(new int[][]{{1, 2}}, resumed.missingRanges());
        }
    }

    @Test
    void refusesBlankDataToken() {
        Payloads.Offer offer = offer("sample.txt", "abc".getBytes(), 2);
        assertThrows(IOException.class, () -> ReceiveSession.accept(tempDir, offer, "  "));
    }

    private static ReceiveSession.ChunkResultInfo receive(ReceiveSession session, byte[] content,
                                                            int chunkSize, int index) throws IOException {
        byte[] payload = slice(content, chunkSize, index);
        byte[] hash = Hashes.sha256(payload, 0, payload.length);
        return session.receiveChunk(index, payload, hash);
    }
}
