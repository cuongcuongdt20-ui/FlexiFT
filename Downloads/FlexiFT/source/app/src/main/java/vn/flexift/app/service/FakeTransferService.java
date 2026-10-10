package vn.flexift.app.service;

import vn.flexift.app.model.Direction;
import vn.flexift.app.model.IncomingOffer;
import vn.flexift.app.model.PeerInfo;
import vn.flexift.app.model.TransferMode;
import vn.flexift.app.model.TransferSnapshot;
import vn.flexift.app.model.TransferState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Lõi giả để dựng và thử giao diện khi lõi thật chưa có. Không mở socket, không ghi file.
 * Mô phỏng: chờ kết nối, có máy nối tới, lời đề nghị gửi file, tiến độ, tốc độ, số kênh,
 * tạm dừng, tiếp tục, hủy, từ chối.
 */
public class FakeTransferService implements TransferService {

    private static final long TICK_MS = 100;
    private static final double BYTES_PER_CHANNEL = 8.0 * 1024 * 1024; // 8 MB/s mỗi kênh

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "fake-core");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, Sim> sims = new ConcurrentHashMap<>();
    private final Map<String, IncomingOffer> pendingOffers = new ConcurrentHashMap<>();
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    private volatile TransferListener listener = new TransferListener() {
        public void onListening(String a) { }
        public void onListeningStopped() { }
        public void onPeerConnected(PeerInfo p) { }
        public void onPeerDisconnected(String id, String r) { }
        public void onOffer(IncomingOffer o) { }
        public void onTransferUpdate(TransferSnapshot s) { }
        public void onError(String m) { }
    };
    private volatile boolean listening;
    private volatile long speedLimit;
    private volatile String deviceName = "May-Toi";
    private int fakePeerCounter;

    public FakeTransferService() {
        scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setListener(TransferListener listener) {
        this.listener = listener;
    }

    @Override
    public void setDeviceName(String name) {
        if (name != null && !name.isBlank()) {
            this.deviceName = name.strip();
        }
    }

    @Override
    public void startListening(int port) {
        if (port < 1 || port > 65535) {
            listener.onError("Cổng không hợp lệ: " + port);
            return;
        }
        scheduler.schedule(() -> {
            listening = true;
            listener.onListening("192.168.1.23:" + port);
        }, 200, TimeUnit.MILLISECONDS);
        // Sau vài giây giả vờ có một máy nối tới và gửi một file đến.
        scheduler.schedule(() -> {
            if (listening) {
                PeerInfo p = newFakePeer("May-Lan");
                listener.onPeerConnected(p);
                scheduler.schedule(() -> {
                    if (listening && peers.containsKey(p.id())) {
                        simulateIncomingOffer();
                    }
                }, 2, TimeUnit.SECONDS);
            }
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    public void stopListening() {
        listening = false;
        listener.onListeningStopped();
    }

    @Override
    public void connectTo(String host, int port) {
        if (host == null || host.isBlank()) {
            listener.onError("Chưa nhập địa chỉ máy cần nối tới.");
            return;
        }
        if (port < 1 || port > 65535) {
            listener.onError("Cổng không hợp lệ: " + port);
            return;
        }
        scheduler.schedule(() -> {
            if (host.strip().equalsIgnoreCase("loi")) { // địa chỉ "loi" dùng để thử trạng thái lỗi
                listener.onError("Không nối được tới " + host + ":" + port + " (hết thời gian chờ).");
                return;
            }
            PeerInfo p = new PeerInfo(UUID.randomUUID().toString(), "May-" + host.strip(), host.strip() + ":" + port);
            peers.put(p.id(), p);
            listener.onPeerConnected(p);
        }, 700, TimeUnit.MILLISECONDS);
    }

    @Override
    public void disconnect(String peerId) {
        PeerInfo p = peers.remove(peerId);
        if (p != null) {
            sims.values().forEach(s -> {
                if (s.snap.peerName().equals(p.name()) && !s.snap.state().isFinished()) {
                    s.update(TransferState.FAILED, s.snap.bytesDone(), 0, 0, "Mất kết nối với " + p.name());
                }
            });
            listener.onPeerDisconnected(peerId, "Đã ngắt kết nối");
        }
    }

    @Override
    public void sendFile(String peerId, Path file, TransferMode mode, int channels) {
        PeerInfo peer = peers.get(peerId);
        if (peer == null) {
            listener.onError("Chưa chọn máy nhận, hoặc máy đó đã ngắt kết nối.");
            return;
        }
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            listener.onError("Không đọc được file: " + file.getFileName());
            return;
        }
        String id = UUID.randomUUID().toString().substring(0, 16);
        TransferSnapshot s = new TransferSnapshot(id, file.getFileName().toString(), size, Direction.SEND,
                peer.name(), mode, TransferState.WAITING_ACCEPT, 0, 0, 0, null);
        Sim sim = new Sim(s, mode == TransferMode.PARALLEL ? Math.max(2, Math.min(8, channels)) : 0);
        sims.put(id, sim);
        listener.onTransferUpdate(s);
        // Bên kia "bấm Nhận" sau 1,2 giây.
        scheduler.schedule(() -> {
            if (sim.snap.state() == TransferState.WAITING_ACCEPT) {
                sim.update(TransferState.RUNNING, 0, 0, 1, null);
            }
        }, 1200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void accept(String transferId, Path saveDir) {
        IncomingOffer o = pendingOffers.remove(transferId);
        if (o == null) {
            listener.onError("Lời đề nghị không còn hiệu lực.");
            return;
        }
        TransferSnapshot s = new TransferSnapshot(o.transferId(), o.fileName(), o.size(), Direction.RECEIVE,
                o.peerName(), o.mode(), TransferState.RUNNING, 0, 0, 1, null);
        Sim sim = new Sim(s, 4);
        sims.put(o.transferId(), sim);
        listener.onTransferUpdate(s);
    }

    @Override
    public void decline(String transferId) {
        pendingOffers.remove(transferId);
    }

    @Override
    public void pause(String transferId) {
        Sim s = sims.get(transferId);
        if (s != null && s.snap.state() == TransferState.RUNNING) {
            s.update(TransferState.PAUSED, s.snap.bytesDone(), 0, s.snap.channels(), null);
        }
    }

    @Override
    public void resume(String transferId) {
        Sim s = sims.get(transferId);
        if (s != null && s.snap.state() == TransferState.PAUSED) {
            s.update(TransferState.RUNNING, s.snap.bytesDone(), 0, s.snap.channels(), null);
        }
    }

    @Override
    public void cancel(String transferId) {
        Sim s = sims.get(transferId);
        if (s != null && !s.snap.state().isFinished()) {
            s.update(TransferState.CANCELLED, s.snap.bytesDone(), 0, 0, null);
        }
        pendingOffers.remove(transferId);
    }

    @Override
    public void setSpeedLimit(long bytesPerSecond) {
        this.speedLimit = Math.max(0, bytesPerSecond);
    }

    /** Chỉ có ở lõi giả: giả lập một máy gửi file đến, để thử hộp thoại Nhận/Từ chối. */
    public void simulateIncomingOffer() {
        PeerInfo p = peers.values().stream().findFirst().orElse(null);
        String peerName = p != null ? p.name() : "May-Lan";
        String id = UUID.randomUUID().toString().substring(0, 16);
        long size = (30 + ThreadLocalRandom.current().nextInt(200)) * 1024L * 1024L;
        IncomingOffer o = new IncomingOffer(id, peerName, "baigiang.pdf", size,
                (int) Math.ceil(size / (1024.0 * 1024)), TransferMode.PARALLEL, false);
        pendingOffers.put(id, o);
        listener.onOffer(o);
    }

    private PeerInfo newFakePeer(String name) {
        PeerInfo p = new PeerInfo(UUID.randomUUID().toString(), name + (fakePeerCounter++ == 0 ? "" : "-" + fakePeerCounter),
                "192.168.1." + (40 + fakePeerCounter) + ":50321");
        peers.put(p.id(), p);
        return p;
    }

    private void tick() {
        try {
            for (Sim sim : sims.values()) {
                sim.step();
            }
        } catch (RuntimeException e) {
            listener.onError("Lõi giả gặp lỗi: " + e);
        }
    }

    @Override
    public void close() {
        listening = false;
        scheduler.shutdownNow();
    }

    /** Mô phỏng một phiên truyền. */
    private final class Sim {
        volatile TransferSnapshot snap;
        private final int fixedChannels; // 0: theo chế độ
        private long runningMs;

        Sim(TransferSnapshot snap, int fixedChannels) {
            this.snap = snap;
            this.fixedChannels = fixedChannels;
        }

        synchronized void update(TransferState st, long bytes, double speed, int ch, String msg) {
            snap = snap.with(st, bytes, speed, ch, msg);
            listener.onTransferUpdate(snap);
        }

        synchronized void step() {
            if (snap.state() != TransferState.RUNNING) {
                return;
            }
            runningMs += TICK_MS;
            int ch = channelsNow();
            double speed = BYTES_PER_CHANNEL * Math.pow(ch, 0.8) * (0.85 + ThreadLocalRandom.current().nextDouble() * 0.3);
            long limit = speedLimit;
            if (limit > 0 && snap.direction() == Direction.SEND) {
                speed = Math.min(speed, limit);
            }
            long done = Math.min(snap.size(), snap.bytesDone() + (long) (speed * TICK_MS / 1000.0));
            if (done >= snap.size()) {
                update(TransferState.COMPLETED, snap.size(), 0, 0, null);
            } else {
                update(TransferState.RUNNING, done, speed, ch, null);
            }
        }

        private int channelsNow() {
            return switch (snap.mode()) {
                case SEQUENTIAL -> 1;
                case PARALLEL -> fixedChannels > 0 ? fixedChannels : 4;
                // Tự động: thêm 1 kênh sau mỗi 2 giây, tối đa 4, rồi tụt xuống 3 (giả lập bộ leo đồi).
                case AUTO -> {
                    int steps = (int) (runningMs / 2000);
                    yield steps < 4 ? 1 + steps : 3;
                }
            };
        }
    }
}
