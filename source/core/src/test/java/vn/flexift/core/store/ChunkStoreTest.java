package vn.flexift.core.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.flexift.common.codec.Hashes;
import vn.flexift.common.protocol.Payloads;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChunkStoreTest {
    @TempDir
    Path temp;

    private Payloads.Offer offer(String name, byte[] bytes, int chunkSize) {
        int count = bytes.length == 0 ? 1 : (bytes.length + chunkSize - 1) / chunkSize;
        return new Payloads.Offer(
                Hashes.transferId(Hashes.hex(Hashes.sha256(bytes, 0, bytes.length)), bytes.length, name),
                name, bytes.length, chunkSize, count,
                Hashes.hex(Hashes.sha256(bytes, 0, bytes.length)), "SEQUENTIAL");
    }

    @Test
    void writesChunksOutOfOrderAndCompletes() throws Exception {
        byte[] all = "abcdefghij".getBytes(StandardCharsets.UTF_8);
        Payloads.Offer offer = offer("test.txt", all, 4);

        Path completed;
        try (ChunkStore store = ChunkStore.open(temp, offer)) {
            assertTrue(store.writeChunk(2, new byte[]{'i', 'j'}, Hashes.sha256(new byte[]{'i', 'j'}, 0, 2)));
            assertTrue(store.writeChunk(0, new byte[]{'a', 'b', 'c', 'd'}, Hashes.sha256(new byte[]{'a','b','c','d'}, 0, 4)));
            assertArrayEquals(new int[][]{{1, 1}}, store.missingRanges());
            assertTrue(store.writeChunk(1, new byte[]{'e','f','g','h'}, Hashes.sha256(new byte[]{'e','f','g','h'}, 0, 4)));
            completed = store.complete();
        }
        assertNotNull(completed);
        assertArrayEquals(all, Files.readAllBytes(completed));
    }

    @Test
    void rejectsChunkWithWrongHash() throws Exception {
        byte[] data = "abcd".getBytes(StandardCharsets.UTF_8);
        Payloads.Offer offer = offer("x.bin", data, 4);
        try (ChunkStore store = ChunkStore.open(temp, offer)) {
            assertFalse(store.writeChunk(0, data, new byte[32]));
            assertEquals(0, store.receivedCount());
        }
    }

    @Test
    void resumesFromMetadata() throws Exception {
        byte[] all = "abcdefgh".getBytes(StandardCharsets.UTF_8);
        Payloads.Offer offer = offer("resume.bin", all, 4);
        try (ChunkStore store = ChunkStore.open(temp, offer)) {
            byte[] first = {'a','b','c','d'};
            assertTrue(store.writeChunk(0, first, Hashes.sha256(first, 0, first.length)));
            store.checkpoint();
        }

        try (ChunkStore restored = ChunkStore.open(temp, offer)) {
            assertEquals(1, restored.receivedCount());
            assertArrayEquals(new int[][]{{1, 1}}, restored.missingRanges());
            byte[] second = {'e','f','g','h'};
            assertTrue(restored.writeChunk(1, second, Hashes.sha256(second, 0, second.length)));
            Path result = restored.complete();
            assertNotNull(result);
            assertArrayEquals(all, Files.readAllBytes(result));
        }
    }

    @Test
    void stripsPathFromIncomingFileName() throws Exception {
        byte[] data = "ok".getBytes(StandardCharsets.UTF_8);
        Payloads.Offer offer = offer("../../evil.txt", data, 4);
        try (ChunkStore store = ChunkStore.open(temp, offer)) {
            assertTrue(store.writeChunk(0, data, Hashes.sha256(data, 0, data.length)));
            Path result = store.complete();
            assertNotNull(result);
            assertEquals(temp.toAbsolutePath().normalize(), result.getParent().toAbsolutePath().normalize());
            assertEquals("evil.txt", result.getFileName().toString());
        }
    }

    @Test
    void refusesResumeIfOfferMetadataChanges() throws Exception {
        byte[] first = "abcd".getBytes(StandardCharsets.UTF_8);
        Payloads.Offer original = offer("file.bin", first, 4);
        try (ChunkStore store = ChunkStore.open(temp, original)) {
            store.checkpoint();
        }
        Payloads.Offer changed = new Payloads.Offer(original.transferId(), original.name(),
                original.size() + 1, original.chunkSize(), original.chunkCount(),
                original.fileHash(), original.mode());
        assertThrows(IOException.class, () -> ChunkStore.open(temp, changed));
    }
}
