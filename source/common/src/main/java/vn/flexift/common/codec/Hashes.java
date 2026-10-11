package vn.flexift.common.codec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Các hàm băm SHA-256 dùng chung. */
public final class Hashes {
    private Hashes() {
    }

    public static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK nào cũng có SHA-256", e);
        }
    }

    public static byte[] sha256(byte[] data, int offset, int length) {
        MessageDigest digest = newSha256();
        digest.update(data, offset, length);
        return digest.digest();
    }

    /** Băm cả file theo luồng, không đọc cả file vào bộ nhớ. */
    public static String sha256File(Path file) throws IOException {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[1 << 16];
        try (InputStream in = Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    /** Mã phiên truyền: 16 ký tự đầu của SHA-256 trên (mã băm file, dung lượng, tên). */
    public static String transferId(String fileHash, long size, String name) {
        byte[] seed = (fileHash + "|" + size + "|" + name).getBytes(StandardCharsets.UTF_8);
        return hex(sha256(seed, 0, seed.length)).substring(0, 16);
    }

    public static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
