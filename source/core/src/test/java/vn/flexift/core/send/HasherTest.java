package vn.flexift.core.send;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HasherTest {
    @Test 
    void sha256_abc() {
        String hex = HexFormat.of().formatHex(Hasher.sha256("abc".getBytes(StandardCharsets.UTF_8)));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex);
    }
    @Test 
    void sha256_chuoiRong() {
        String hex = HexFormat.of().formatHex(Hasher.sha256("".getBytes(StandardCharsets.UTF_8)));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex);
    }
    @Test 
    void sha256_luonDai32Byte() {
        byte[] hash = Hasher.sha256("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals(32, hash.length);
    }
    @Test 
    void sha256_doiMotByteThiKhac() {
        byte[] hash1 = Hasher.sha256(new byte[]{1,2,4});
        byte[] hash2 = Hasher.sha256(new byte[]{1,2,3});
        assertFalse(Arrays.equals(hash1, hash2));
    }
    @Test 
    void sha256_cungDauVaoCungKetQua() {
        byte[] hash1 = Hasher.sha256(new byte[]{1,2,4});
        byte[] hash2 = Hasher.sha256(new byte[]{1,2,4});
        assertArrayEquals(hash1, hash2);
    }

    @Test
    void sha256File_khopVoiSha256CuaCaMang(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[200_000];    
        new Random(42).nextBytes(data);     
        Path file = tmp.resolve("a.bin");
        Files.write(file, data);

        assertArrayEquals(Hasher.sha256(data), Hasher.sha256File(file));
    }

    @Test
    void khopVoiMang_nhoHon64KB(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[1000]; 
        new Random(42).nextBytes(data);        
        Path file = tmp.resolve("test1.bin");
        Files.write(file, data);

        assertArrayEquals(Hasher.sha256(data), Hasher.sha256File(file));
    }

    @Test
    void khopVoiMang_khongChiaHet64KB(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[64 * 1024 + 1]; 
        new Random(42).nextBytes(data);        
        Path file = tmp.resolve("test2.bin");
        Files.write(file, data);

        assertArrayEquals(Hasher.sha256(data), Hasher.sha256File(file));
    }

    @Test
    void khopVoiMang_dungBoi64KB(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[64 * 1024 * 3];
        new Random(42).nextBytes(data);         
        Path file = tmp.resolve("test3.bin");
        Files.write(file, data);

        assertArrayEquals(Hasher.sha256(data), Hasher.sha256File(file));
    }

    @Test
    void fileRong(@TempDir Path tmp) throws IOException {
        byte[] data = new byte[0];         
        Path file = tmp.resolve("test4.bin");
        Files.write(file, data);

        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HexFormat.of().formatHex((Hasher.sha256File(file))));
    }
}
