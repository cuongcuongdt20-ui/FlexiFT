package vn.flexift.core.send;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return hash;
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 khong kha dung tren JVM nay", e);
        }
    }

    public static byte[] sha256File(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                while ((n = in.read(buffer)) != -1) {
                    md.update(buffer, 0, n);
                }
                return md.digest();
            }
            catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 khong kha dung tren JVM nay", e);
            }
        } 
    }
}
