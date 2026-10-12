package vn.flexift.core.send;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

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
        byte[] hex = Hasher.sha256("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals(32, hex.length);
    }
    @Test 
    void sha256_doiMotByteThiKhac() {
        byte[] hex1 = Hasher.sha256(new byte[]{1,2,4});
        byte[] hex2 = Hasher.sha256(new byte[]{1,2,3});
        assertFalse(Arrays.equals(hex1, hex2));
    }
    @Test 
    void sha256_cungDauVaoCungKetQua() {
        byte[] hex1 = Hasher.sha256(new byte[]{1,2,4});
        byte[] hex2 = Hasher.sha256(new byte[]{1,2,4});
        assertArrayEquals(hex1, hex2);
    }
}
