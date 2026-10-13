package vn.flexift.core.store;

import com.google.gson.Gson;
import vn.flexift.common.codec.Hashes;
import vn.flexift.common.protocol.Payloads;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Kho lưu các khối đang nhận.
 *
 * <p>Mỗi phiên có một file dữ liệu .part và một file ghi nhớ .part.json.
 * Khối được ghi tại vị trí index * chunkSize nên có thể nhận sai thứ tự.</p>
 *
 * <p>Lớp này không tự mở socket; phần xử lý giao thức sẽ gọi các hàm của nó.</p>
 */
public final class ChunkStore implements AutoCloseable {
    private static final int CHECKPOINT_EVERY = 16;
    private static final Gson GSON = new Gson();

    private final Path receiveDir;
    private final Path partPath;
    private final Path metadataPath;
    private final Payloads.Offer offer;
    private final FileChannel channel;
    private final boolean[] received;

    private int chunksSinceCheckpoint;
    private boolean closed;
    private boolean completed;

    private ChunkStore(Path receiveDir, Path partPath, Path metadataPath,
                       Payloads.Offer offer, FileChannel channel, boolean[] received) {
        this.receiveDir = receiveDir;
        this.partPath = partPath;
        this.metadataPath = metadataPath;
        this.offer = offer;
        this.channel = channel;
        this.received = received;
    }

    /**
     * Mở phiên nhận mới hoặc khôi phục phiên cũ.
     * Nếu metadata cũ không khớp OFFER hiện tại, không được dùng lại dữ liệu đó.
     */
    public static ChunkStore open(Path receiveDir, Payloads.Offer offer) throws IOException {
        validateOffer(offer);
        Files.createDirectories(receiveDir);

        String safeName = safeFileName(offer.name());
        String id = safeTransferId(offer.transferId());
        Path part = receiveDir.resolve(safeName + "." + id + ".part").normalize();
        Path metadata = receiveDir.resolve(id + ".part.json").normalize();

        // Bảo đảm đường dẫn sau chuẩn hóa vẫn nằm trong thư mục nhận.
        Path normalizedDir = receiveDir.toAbsolutePath().normalize();
        if (!part.toAbsolutePath().normalize().startsWith(normalizedDir)
                || !metadata.toAbsolutePath().normalize().startsWith(normalizedDir)) {
            throw new IOException("Đường dẫn file nhận không hợp lệ");
        }

        boolean[] received = new boolean[offer.chunkCount()];
        if (Files.exists(metadata)) {
            Metadata saved = readMetadata(metadata);
            if (!saved.matches(offer, safeName) || saved.received == null
                    || saved.received.length != offer.chunkCount()) {
                throw new IOException("File ghi nhớ không khớp OFFER; từ chối khôi phục phiên");
            }
            System.arraycopy(saved.received, 0, received, 0, received.length);
            if (!Files.exists(part)) {
                // Metadata còn nhưng dữ liệu tạm mất: không thể tin các cờ received.
                received = new boolean[offer.chunkCount()];
            }
        }

        FileChannel channel = FileChannel.open(part,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);

        // Tạo độ dài logic cho file tạm mà không đọc toàn bộ file vào RAM.
        if (offer.size() > 0 && channel.size() < offer.size()) {
            channel.write(ByteBuffer.wrap(new byte[]{0}), offer.size() - 1);
        }

        ChunkStore store = new ChunkStore(receiveDir, part, metadata, offer, channel, received);

        // File rỗng không có payload chunk hợp lệ (ChunkFrame cấm length = 0).
        // Đánh dấu trạng thái rỗng là hoàn tất về dữ liệu; khi kiểm tra sẽ xác minh SHA-256.
        if (offer.size() == 0) {
            if (offer.chunkCount() != 1) {
                store.close();
                throw new IOException("OFFER file rỗng phải có chunkCount = 1 theo Chunker hiện tại");
            }
            store.received[0] = true;
            store.saveMetadata();
        } else if (!Files.exists(metadata)) {
            store.saveMetadata();
        }
        return store;
    }

    /** Kiểm tra mã băm rồi ghi khối tại đúng vị trí. Trả false nếu mã băm sai. */
    public synchronized boolean writeChunk(int index, byte[] payload, byte[] claimedSha256)
            throws IOException {
        ensureOpen();
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(claimedSha256, "claimedSha256");

        if (index < 0 || index >= offer.chunkCount()) {
            throw new IOException("Chỉ số khối nằm ngoài phạm vi: " + index);
        }
        if (offer.size() == 0) {
            throw new IOException("File rỗng không được gửi khối dữ liệu");
        }

        int expectedLength = expectedChunkLength(index);
        if (payload.length != expectedLength) {
            throw new IOException("Độ dài khối " + index + " không hợp lệ: cần "
                    + expectedLength + ", nhận " + payload.length);
        }
        if (claimedSha256.length != 32) {
            return false;
        }

        byte[] actual = Hashes.sha256(payload, 0, payload.length);
        if (!java.security.MessageDigest.isEqual(actual, claimedSha256)) {
            return false;
        }

        // Ghi đúng vị trí, không phụ thuộc thứ tự các khối đến.
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        long position = (long) index * offer.chunkSize();
        while (buffer.hasRemaining()) {
            int written = channel.write(buffer, position);
            if (written <= 0) {
                throw new IOException("Không ghi được khối " + index);
            }
            position += written;
        }

        synchronized (received) {
            if (!received[index]) {
                received[index] = true;
                chunksSinceCheckpoint++;
            }
        }

        if (chunksSinceCheckpoint >= CHECKPOINT_EVERY) {
            saveMetadata();
            chunksSinceCheckpoint = 0;
        }
        return true;
    }

    /** Các khoảng [đầu, cuối] của khối còn thiếu, bao gồm cả hai đầu. */
    public synchronized int[][] missingRanges() {
        List<int[]> ranges = new ArrayList<>();
        int i = 0;
        while (i < received.length) {
            while (i < received.length && received[i]) i++;
            if (i >= received.length) break;
            int start = i;
            while (i + 1 < received.length && !received[i + 1]) i++;
            ranges.add(new int[]{start, i});
            i++;
        }
        return ranges.toArray(new int[0][]);
    }

    public synchronized boolean hasAllChunks() {
        for (boolean value : received) {
            if (!value) return false;
        }
        return true;
    }

    /**
     * Kiểm tra đủ khối và SHA-256 toàn file. Nếu hợp lệ, đổi tên .part thành tên file thật.
     * Trả về đường dẫn file hoàn chỉnh; trả null nếu còn thiếu khối hoặc hash không khớp.
     */
    public synchronized Path complete() throws IOException {
        ensureOpen();
        if (!hasAllChunks()) return null;

        channel.force(true);
        String actualHash = Hashes.sha256File(partPath);
        if (!actualHash.equalsIgnoreCase(offer.fileHash())) {
            return null;
        }

        channel.close();
        closed = true;

        Path target = uniqueTarget(receiveDir.resolve(safeFileName(offer.name())));
        try {
            Files.move(partPath, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(partPath, target);
        }
        Files.deleteIfExists(metadataPath);
        completed = true;
        return target;
    }

    /** Lưu metadata ngay khi tạm dừng hoặc đóng phiên. */
    public synchronized void checkpoint() throws IOException {
        ensureOpen();
        saveMetadata();
        chunksSinceCheckpoint = 0;
        channel.force(true);
    }

    public Payloads.Offer offer() {
        return offer;
    }

    public Path partPath() {
        return partPath;
    }

    public synchronized int receivedCount() {
        int count = 0;
        for (boolean value : received) if (value) count++;
        return count;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        IOException failure = null;
        try {
            saveMetadata();
            channel.force(true);
        } catch (IOException e) {
            failure = e;
        }
        try {
            channel.close();
        } catch (IOException e) {
            if (failure == null) failure = e;
            else failure.addSuppressed(e);
        }
        closed = true;
        if (failure != null) throw failure;
    }

    private int expectedChunkLength(int index) {
        long start = (long) index * offer.chunkSize();
        return (int) Math.min(offer.chunkSize(), offer.size() - start);
    }

    private void saveMetadata() throws IOException {
        Metadata data = new Metadata(offer.transferId(), safeFileName(offer.name()), offer.size(),
                offer.chunkSize(), offer.chunkCount(), offer.fileHash(), received.clone());
        Path temp = metadataPath.resolveSibling(metadataPath.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(data), java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temp, metadataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, metadataPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Metadata readMetadata(Path path) throws IOException {
        try {
            Metadata result = GSON.fromJson(
                    Files.readString(path, java.nio.charset.StandardCharsets.UTF_8), Metadata.class);
            if (result == null) throw new IOException("Metadata rỗng");
            return result;
        } catch (RuntimeException e) {
            throw new IOException("Không đọc được file ghi nhớ: " + path.getFileName(), e);
        }
    }

    private static void validateOffer(Payloads.Offer offer) throws IOException {
        if (offer == null || offer.transferId() == null || offer.name() == null
                || offer.fileHash() == null) {
            throw new IOException("OFFER thiếu trường bắt buộc");
        }
        if (offer.size() < 0 || offer.chunkSize() <= 0 || offer.chunkCount() <= 0) {
            throw new IOException("Dung lượng hoặc thông số chia khối không hợp lệ");
        }
        long expected = offer.size() == 0 ? 1 : (offer.size() + offer.chunkSize() - 1) / offer.chunkSize();
        if (expected != offer.chunkCount()) {
            throw new IOException("chunkCount không khớp size/chunkSize");
        }
        try {
            if (HexFormat.of().parseHex(offer.fileHash()).length != 32) {
                throw new IOException("fileHash phải là SHA-256 dạng hex 32 byte");
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("fileHash không phải chuỗi hex hợp lệ", e);
        }
    }

    private static String safeFileName(String name) throws IOException {
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).trim();
        if (base.isEmpty() || base.equals(".") || base.equals("..")) {
            throw new IOException("Tên file không hợp lệ");
        }
        // Loại các ký tự điều khiển và ký tự cấm phổ biến trên Windows.
        base = base.replaceAll("[\\x00-\\x1F<>:\"|?*]", "_");
        if (base.isBlank()) throw new IOException("Tên file không hợp lệ");
        return base;
    }

    private static String safeTransferId(String id) throws IOException {
        if (id == null || !id.matches("[a-fA-F0-9]{16}")) {
            throw new IOException("transferId phải gồm 16 ký tự hex");
        }
        return id;
    }

    private static Path uniqueTarget(Path requested) {
        if (!Files.exists(requested)) return requested;
        String name = requested.getFileName().toString();
        String stem = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            stem = name.substring(0, dot);
            ext = name.substring(dot);
        }
        for (int i = 1; ; i++) {
            Path candidate = requested.resolveSibling(stem + " (" + i + ")" + ext);
            if (!Files.exists(candidate)) return candidate;
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Kho khối đã đóng");
    }

    private static final class Metadata {
        String transferId;
        String name;
        long size;
        int chunkSize;
        int chunkCount;
        String fileHash;
        boolean[] received;

        Metadata(String transferId, String name, long size, int chunkSize, int chunkCount,
                 String fileHash, boolean[] received) {
            this.transferId = transferId;
            this.name = name;
            this.size = size;
            this.chunkSize = chunkSize;
            this.chunkCount = chunkCount;
            this.fileHash = fileHash;
            this.received = received;
        }

        boolean matches(Payloads.Offer offer, String safeName) {
            return Objects.equals(transferId, offer.transferId())
                    && Objects.equals(name, safeName)
                    && size == offer.size()
                    && chunkSize == offer.chunkSize()
                    && chunkCount == offer.chunkCount()
                    && Objects.equals(fileHash, offer.fileHash());
        }
    }
}
