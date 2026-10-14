package vn.flexift.core.send;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ChunkReaderTest {
    @Test 
    void khoiDau(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertArrayEquals(Arrays.copyOfRange(data, 0, 100) ,ChunkReader.readChunk(ch, 0, 100) );
        }
    }

    @Test 
    void khoiGiua(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertArrayEquals(Arrays.copyOfRange(data, 500, 600),ChunkReader.readChunk(ch, 5, 100)  );
        }
    }

    @Test 
    void khoiCuoiNgan(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertArrayEquals(Arrays.copyOfRange(data, 1000, 1050) ,ChunkReader.readChunk(ch, 10, 100) );
        }
    }

    @Test 
    void indexAm(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertThrows(IllegalArgumentException.class,
            () -> ChunkReader.readChunk(ch, -1, 100));
        }
    }

    @Test 
    void indexQuaXa1(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertThrows(IllegalArgumentException.class,
            () -> ChunkReader.readChunk(ch, 50, 100));
        }
    }

    @Test 
    void indexQuaXa2(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1000];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertThrows(IllegalArgumentException.class,
            () -> ChunkReader.readChunk(ch, 10, 100));
        }
    }

    @Test 
    void chunkSizeKhongHopLe(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertThrows(IllegalArgumentException.class,
            () -> ChunkReader.readChunk(ch, 0, 0));
        }
    }

    @Test 
    void ghepLai(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int soKhoi = Chunker.chunkCount(ch.size(), 100);
            for (int i = 0; i < soKhoi; i++) {
                os.write(ChunkReader.readChunk(ch, i, 100));
            }
            byte[] result = os.toByteArray();
            assertArrayEquals(data, result);
        }
    }

    @Test 
    void hashKhop(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int soKhoi = Chunker.chunkCount(ch.size(), 100);
            for (int i = 0; i < soKhoi; i++) {
                os.write(ChunkReader.readChunk(ch, i, 100));
            }
            byte[] result = os.toByteArray();
            assertArrayEquals(Hasher.sha256File(file), Hasher.sha256(result));
        }
    }

    @Test 
    void fileRong(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[0];
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            assertEquals(0, ChunkReader.readChunk(ch, 0, 100).length);
        }
    }
    
}
