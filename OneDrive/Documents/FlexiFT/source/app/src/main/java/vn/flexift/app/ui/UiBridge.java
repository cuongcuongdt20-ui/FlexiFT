package vn.flexift.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import vn.flexift.app.model.IncomingOffer;
import vn.flexift.app.model.PeerInfo;
import vn.flexift.app.model.TransferSnapshot;
import vn.flexift.app.service.TransferListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cầu nối luồng mạng -> luồng JavaFX (mục 2.6): luồng mạng không đụng giao diện.
 * Cập nhật tiến độ được gộp lại: mỗi phiên chỉ giữ ảnh chụp mới nhất và đẩy lên giao diện
 * tối đa 10 lần mỗi giây, nên file lớn hay nhiều kênh cũng không làm giao diện đứng.
 * Các sự kiện khác (nối, ngắt, lời đề nghị, lỗi) đi thẳng bằng Platform.runLater.
 */
public final class UiBridge implements TransferListener {
    private static final Duration FLUSH_PERIOD = Duration.millis(100);

    private final TransferListener ui; // luôn được gọi trên luồng JavaFX
    private final Map<String, TransferSnapshot> latest = new ConcurrentHashMap<>();
    private final Timeline flusher;

    public UiBridge(TransferListener ui) {
        this.ui = ui;
        this.flusher = new Timeline(new KeyFrame(FLUSH_PERIOD, e -> flush()));
        this.flusher.setCycleCount(Timeline.INDEFINITE);
    }

    /** Gọi trên luồng JavaFX. */
    public void start() {
        flusher.play();
    }

    public void stop() {
        flusher.stop();
    }

    private void flush() {
        if (latest.isEmpty()) {
            return;
        }
        List<TransferSnapshot> batch = new ArrayList<>();
        for (String id : new ArrayList<>(latest.keySet())) {
            TransferSnapshot s = latest.remove(id);
            if (s != null) {
                batch.add(s);
            }
        }
        for (TransferSnapshot s : batch) {
            ui.onTransferUpdate(s);
        }
    }

    @Override
    public void onTransferUpdate(TransferSnapshot snapshot) {
        latest.put(snapshot.transferId(), snapshot);
    }

    @Override
    public void onListening(String address) {
        Platform.runLater(() -> ui.onListening(address));
    }

    @Override
    public void onListeningStopped() {
        Platform.runLater(ui::onListeningStopped);
    }

    @Override
    public void onPeerConnected(PeerInfo peer) {
        Platform.runLater(() -> ui.onPeerConnected(peer));
    }

    @Override
    public void onPeerDisconnected(String peerId, String reason) {
        Platform.runLater(() -> ui.onPeerDisconnected(peerId, reason));
    }

    @Override
    public void onOffer(IncomingOffer offer) {
        Platform.runLater(() -> ui.onOffer(offer));
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> ui.onError(message));
    }
}
