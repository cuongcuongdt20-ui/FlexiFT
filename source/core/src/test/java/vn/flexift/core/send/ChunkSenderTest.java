package vn.flexift.core.send;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
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
    void guiTuanTuGhepLai(@TempDir Path tmp) throws IOException {
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

    @RepeatedTest(20)
    void songSongDuKhoi(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }

            ChunkSender.sendParallel(ch, 100, indexes, outs);

            List<ChunkFrame> tatCa = new ArrayList<>();
            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }
            assertEquals(11, tatCa.size());

            Set<Integer> chiSo = new HashSet<>();
            for (ChunkFrame f : tatCa) {
                chiSo.add(f.index());
            }
            assertEquals(11, chiSo.size());   
        }
    }

    @RepeatedTest(20)
    void songSongNhieuKhoi(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[10000];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = new int[100];
            for (int i = 0; i < 100; i++) {
                indexes[i] = i;
            }

            ChunkSender.sendParallel(ch, 100, indexes, outs);

            List<ChunkFrame> tatCa = new ArrayList<>();
            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }
            assertEquals(100, tatCa.size());

            Set<Integer> chiSo = new HashSet<>();
            for (ChunkFrame f : tatCa) {
                chiSo.add(f.index());
            }
            assertEquals(100, chiSo.size());   
        }
    }

    @RepeatedTest(20)
    void songSongGhepLai(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            
            ChunkSender.sendParallel(ch, 100, indexes, outs);
            List<ChunkFrame> tatCa = new ArrayList<>();

            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }
            
            byte[] result = new byte[1050];

            for (ChunkFrame f : tatCa) {
                System.arraycopy(f.payload(), 0, result, f.index()*100, f.payload().length);
            }

            assertArrayEquals(data, result);
        }
    }

    @Test
    void motKenh(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[1];
            DataOutputStream[] outs = new DataOutputStream[1];
            for (int i = 0; i < 1; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            
            ChunkSender.sendParallel(ch, 100, indexes, outs);

            List<ChunkFrame> tatCa = new ArrayList<>();
            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }
            assertEquals(11, tatCa.size()); 
        }
    }

    @Test
    void danhSachCon2(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = {2, 5, 7};
            
            ChunkSender.sendParallel(ch, 100, indexes, outs);

            List<ChunkFrame> tatCa = new ArrayList<>();
            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }

            Set<Integer> chiSo = new HashSet<>();
            for (ChunkFrame f : tatCa) {
                chiSo.add(f.index());
            }
            assertEquals(Set.of(2,5,7), chiSo);
            assertEquals(3, tatCa.size());   
        }
    }

    @Test
    void fileRong2(@TempDir Path tmp) throws IOException, InterruptedException {
        Path file = tmp.resolve("a.bin");
        Files.write(file, new byte[0]);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = {0};
            
            ChunkSender.sendParallel(ch, 100, indexes, outs);

            List<ChunkFrame> tatCa = new ArrayList<>();
            for (ByteArrayOutputStream b : bos) {
                tatCa.addAll(docHet(b.toByteArray()));
            }
            assertTrue(tatCa.isEmpty());  
        }
    }

    @Test
    void outsRong(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            DataOutputStream[] outs = new DataOutputStream[0];

            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            
            assertThrows(IllegalArgumentException.class, () -> {
                ChunkSender.sendParallel(ch, 100, indexes, outs);
            });
        }
    }

    @Test
    void indexesNull(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            
            ByteArrayOutputStream[] bos = new ByteArrayOutputStream[3];
            DataOutputStream[] outs = new DataOutputStream[3];
            for (int i = 0; i < 3; i++) {
                bos[i] = new ByteArrayOutputStream();
                outs[i] = new DataOutputStream(bos[i]);
            }

            int[] indexes = null;
            
            assertThrows(NullPointerException.class, 
                () -> {
                    ChunkSender.sendParallel(ch, 100, indexes, outs);
                }
            );  
        }
    }

    @Test
    void kenhHong(@TempDir Path tmp) throws IOException, InterruptedException {

        byte[] data = new byte[1050];
        new Random(42).nextBytes(data);
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {

            int[] indexes = new int[11];
            for (int i = 0; i < 11; i++) {
                indexes[i] = i;
            }
            
            OutputStream hong = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    throw new IOException("kenh dut");
                }
            };
            DataOutputStream[] outs = { new DataOutputStream(hong) };
            assertThrows(IOException.class, () -> ChunkSender.sendParallel(ch, 100, indexes, outs));  
        }
    }
}