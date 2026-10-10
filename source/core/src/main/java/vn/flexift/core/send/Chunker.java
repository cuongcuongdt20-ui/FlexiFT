package vn.flexift.core.send;

public class Chunker {
    public static final long MB = 1024L * 1024;
    public static int chunkSizeFor(long fileSize) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("Kich thuoc file >= 0 :" + fileSize);
        }
        if (fileSize < 64 * MB) {
            return (int)(256L * 1024);
        }
        else if (fileSize <= 1024 * MB) {
            return (int)(MB);
        }
        return (int)(4L * MB);
    }
    public static int chunkCount(long fileSize, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Kich thuoc khoi phai > 0 :" + chunkSize);
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("Kich thuoc file >= 0 :" + fileSize);
        }
        if (fileSize == 0) return 1;
        return (int)((fileSize + chunkSize - 1) / chunkSize);
    }
}
