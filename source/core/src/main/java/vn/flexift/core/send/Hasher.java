package vn.flexift.core.send;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Hasher {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return hash;
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Khong phai byte: " + data, e);
        }
    }
}
