package vn.flexift.core.send;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vn.flexift.common.codec.ChunkFrame;
import vn.flexift.common.codec.Hashes;

public class ChunkSenderTest {

    private static byte[] guiVaLayByte(FileChannel ch, int chunkSize, int[] indexes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        ChunkSender.sendChunks(ch, chunkSize, indexes, out);
        return bos.toByteArray();
    }

    private static List<ChunkFrame> docHet(byte[] daGhi) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(daGhi));
        List<ChunkFrame> ds = new ArrayList<>();
        ChunkFrame f;
        while ((f = ChunkFrame.read(in, 100)) != null) {
            ds.add(f);
        }
        return ds;
    }

    @Test
    void guiHet(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            assertEquals(11, khung.size());
            for (int i = 0; i < 11; i++) {
                assertEquals(i, khung.get(i).index());
            }
        }
    }

    @Test
    void noiDungDung(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            for (int i = 0; i < 11; i++) {
                assertArrayEquals(Arrays.copyOfRange(data, khung.get(i).index()*100, Math.min(khung.get(i).index() *100 + 100, 1050)), khung.get(i).payload());
            }
        }
    }

    @Test
    void hashDung(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            for (int i = 0; i < 11; i++) {
                assertArrayEquals(Hashes.sha256(khung.get(i).payload(), 0, khung.get(i).payload().length), khung.get(i).sha256());
            }
        }
    }

    @Test
    void khoiCuoiNgan(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            assertEquals(50, khung.get(10).payload().length);
        }
    }

    @Test
    void danhSachCon(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = {2, 5 ,7};
    
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            assertEquals(2, khung.get(0).index());
            assertEquals(5, khung.get(1).index());
            assertEquals(7, khung.get(2).index());
            assertArrayEquals(Arrays.copyOfRange(data, 500, 600), khung.get(1).payload());
        }
    }

    @Test
    void ghepLai(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            byte[] result = new byte[1050];
            for (ChunkFrame f : khung) {
                System.arraycopy(f.payload(), 0, result, f.index() * 100, f.payload().length);
            }
            assertArrayEquals(data, result);
        }
    }

    @Test
    void fileRong(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("a.bin");
        Files.write(file, new byte[0]);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = {0};

            byte[] daGhi = guiVaLayByte(ch, 100, indexes);
            List<ChunkFrame> khung = docHet(daGhi);

            assertTrue(khung.isEmpty());
        }
    }

    @Test
    void indexSai(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            int[] indexes = {99};

            assertThrows(IllegalArgumentException.class, () -> guiVaLayByte(ch, 100, indexes));
        }
    }
}