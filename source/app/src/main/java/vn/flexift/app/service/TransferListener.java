package vn.flexift.app.service;

import vn.flexift.app.model.IncomingOffer;
import vn.flexift.app.model.PeerInfo;
import vn.flexift.app.model.TransferSnapshot;

/**
 * Lõi báo sự kiện cho giao diện. Các hàm có thể được gọi từ BẤT KỲ luồng nào;
 * giao diện tự chuyển sang luồng JavaFX (xem ui.UiBridge), lõi không được đụng tới giao diện (mục 2.6).
 */
public interface TransferListener {
    void onListening(String address);

    void onListeningStopped();

    void onPeerConnected(PeerInfo peer);

    void onPeerDisconnected(String peerId, String reason);

    void onOffer(IncomingOffer offer);

    void onTransferUpdate(TransferSnapshot snapshot);

    void onError(String message);
}
