package vn.flexift.core.send;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkerTest {
    @Test 
    void chunkCount_chiaHet() {
        assertEquals(4,  Chunker.chunkCount(4 * Chunker.MB, (int)Chunker.MB));
    }

    @Test 
    void chunkSizeFor_satduoi64MB() {
        assertEquals(256 * 1024, Chunker.chunkSizeFor(64 * Chunker.MB - 1));
    }

    @Test 
    void chunkCount_chunkSizeBangKhong() {
        assertThrows(IllegalArgumentException.class, 
            () -> Chunker.chunkCount(1000L, 0));
    }

    @Test 
    void chunkCount_duIt() {
        assertEquals(3, Chunker.chunkCount(2500000, (int)Chunker.MB));
    }

    @Test 
    void chunkCount_duMotByte() {
        assertEquals(4, Chunker.chunkCount(3 * Chunker.MB + 1, (int)Chunker.MB));
    }

    @Test 
    void chunkCount_fileKhongByte() {
        assertEquals(1, Chunker.chunkCount(0, (int)Chunker.MB));
    }

    @Test 
    void chunkCount_viDuTaiLieu() {
        assertEquals(70, Chunker.chunkCount(73400320, (int)Chunker.MB));
    }


    @Test 
    void chunkSizeFor_nhomNho() {
        assertEquals(256 * 1024, Chunker.chunkSizeFor(10*Chunker.MB));
    }

    @Test 
    void chunkSizeFor_dung64MB() {
        assertEquals(Chunker.MB, Chunker.chunkSizeFor(64*Chunker.MB));
    }

    @Test 
    void chunkSizeFor_nhomGiua() {
        assertEquals(Chunker.MB, Chunker.chunkSizeFor(500*Chunker.MB));
    }

    @Test 
    void chunkSizeFor_dung1GB() {
        assertEquals(Chunker.MB, Chunker.chunkSizeFor(1024*Chunker.MB));
    }

    @Test 
    void chunkSizeFor_tren1GB() {
        assertEquals(4 * Chunker.MB, Chunker.chunkSizeFor(1024*Chunker.MB + 1));
    }

    @Test 
    void chunkSizeFor_nhomLon() {
        assertEquals(4 * Chunker.MB, Chunker.chunkSizeFor(2L * 1024 * Chunker.MB));
    }

    @Test 
    void chunkCount_chunkSizeAm() {
        assertThrows(IllegalArgumentException.class, 
            () -> Chunker.chunkCount(1000L, -1));
    }

    @Test 
    void chunkCount_fileSizeAm() {
        assertThrows(IllegalArgumentException.class, 
            () -> Chunker.chunkCount(-1L, 1024));
    }

    @Test 
    void chunkSizeFor_fileSizeAm() {
        assertThrows(IllegalArgumentException.class, 
            () -> Chunker.chunkSizeFor(-1L));
    }
}
